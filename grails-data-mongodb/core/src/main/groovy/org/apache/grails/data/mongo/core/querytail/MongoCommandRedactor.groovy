/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.grails.data.mongo.core.querytail

import groovy.transform.CompileStatic
import org.bson.BsonDocument
import org.bson.BsonValue

import org.apache.grails.data.core.querytail.QueryTailRedactor

/**
 * Takes the values out of a MongoDB command before it is captured. A command is a document, so
 * there is nothing to parse: every value becomes {@link QueryTailRedactor#PLACEHOLDER}, and the
 * keys, which are the options of the command, its operators and the field names of the collection,
 * are kept, except where a key may be data. The keys of a document stored in a map property, or in
 * a property or a collection the {@linkplain MongoMappedFields mapping} does not know, are recorded
 * as the placeholder too, and so is every segment of a dotted path past such a key. A numeric key
 * is kept only where it indexes an array the mapping knows; in a map, in an element of an array,
 * which is a map or a document unless the field is a list of lists, or at the top of a document,
 * it is data. The entries the driver adds for its session, the cluster time and the transaction,
 * which say nothing about the command, are left out.
 *
 * <p>A command is recorded as its name, the database and collection it runs against, and the
 * document of its remaining entries: {@code find shop.book {"filter": {"title": ?}, "limit": ?}}.</p>
 *
 * @since 8.1
 */
@CompileStatic
final class MongoCommandRedactor {

    private static final String PLACEHOLDER = QueryTailRedactor.PLACEHOLDER

    private static final String DATABASE = '$db'

    private static final String COLLECTION = 'collection'

    /** The entries the driver adds, which describe its session and transaction rather than the command. */
    private static final Set<String> LEFT_OUT = ['lsid', '$clusterTime', 'txnNumber', 'autocommit',
            'startTransaction', '$readPreference', 'apiVersion', 'apiStrict', 'apiDeprecationErrors'] as Set<String>

    /**
     * The options whose document, or documents, are keyed by the fields of the collection, wherever they appear,
     * including those of the operations of a client-level {@code bulkWrite}.
     */
    private static final Set<String> FIELD_KEYED = ['filter', 'query', 'q', 'u', 'update', 'document', 'documents',
            'updateMods', 'pipeline', 'sort', 'projection', 'fields', 'key', 'hint', 'min', 'max', 'arrayFilters',
            'partialFilterExpression', 'wildcardProjection'] as Set<String>

    private MongoCommandRedactor() {
    }

    /**
     * Redacts a command.
     *
     * @param database the database the command runs against
     * @param commandName the name of the command, which is its first key
     * @param command the command as the driver sends it
     * @param fields the fields the mapping of the datastore knows, or null before the datastore is built
     * @return the command with its values replaced
     */
    static String redact(String database, String commandName, BsonDocument command, MongoMappedFields fields) {
        String collection = null
        String collectionKey = null
        BsonValue first = command.get(commandName)
        if (first != null && first.isString()) {
            collection = first.asString().value
            collectionKey = commandName
        }
        else if (command.get(COLLECTION)?.isString()) {
            collection = command.getString(COLLECTION).value
            collectionKey = COLLECTION
        }
        StringBuilder out = new StringBuilder(commandName)
        if (database != null || collection != null) {
            out.append(' ')
            if (database != null) {
                out.append(database)
            }
            if (collection != null) {
                out.append(database != null ? '.' : '').append(collection)
            }
        }
        Scope options = Scope.options(collection, fields)
        StringBuilder body = new StringBuilder()
        for (Map.Entry<String, BsonValue> entry in command.entrySet()) {
            String key = entry.key
            if (key == DATABASE || key == collectionKey || LEFT_OUT.contains(key)) {
                continue
            }
            body.append(body.length() == 0 ? '{' : ', ')
            appendEntry(body, key, entry.value, options)
        }
        if (body.length() > 0) {
            out.append(' ').append(body).append('}')
        }
        out.toString()
    }

    private static void appendEntry(StringBuilder out, String key, BsonValue value, Scope scope) {
        Scope child = scope.of(key)
        out.append(child.keyText).append(': ')
        appendValue(out, value, child)
    }

    private static void appendValue(StringBuilder out, BsonValue value, Scope scope) {
        if (value.isDocument()) {
            out.append('{')
            boolean first = true
            for (Map.Entry<String, BsonValue> entry in value.asDocument().entrySet()) {
                if (!first) {
                    out.append(', ')
                }
                first = false
                appendEntry(out, entry.key, entry.value, scope)
            }
            out.append('}')
        }
        else if (value.isArray()) {
            out.append('[')
            boolean first = true
            Scope element = scope.element()
            for (BsonValue item in value.asArray()) {
                if (!first) {
                    out.append(', ')
                }
                first = false
                appendValue(out, item, element)
            }
            out.append(']')
        }
        else {
            out.append(PLACEHOLDER)
        }
    }

    private static String quote(String key) {
        StringBuilder out = new StringBuilder(key.length() + 2).append('"')
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i)
            if (c == '"' || c == '\\') {
                out.append('\\').append(c)
            }
            else if (c < 0x20) {
                out.append(String.format('\\u%04x', (int) c))
            }
            else {
                out.append(c)
            }
        }
        out.append('"').toString()
    }

    /**
     * Where a document sits in the command: among the options of the command, whose keys are kept
     * and name what their documents hold, or in a document keyed by fields, where a key is kept when
     * it is an operator or a field the mapping knows at its path.
     */
    @CompileStatic
    private static final class Scope {

        final String collection
        final MongoMappedFields fields
        /** The field path of the document; null in a document keyed by fields the mapping does not know. */
        final List<String> path
        final boolean optionKeyed
        final String keyText
        /** How many indices into the field at the path have been taken: 0 at the field, 1 in one of its elements. */
        final int depth

        private Scope(String collection, MongoMappedFields fields, List<String> path, boolean optionKeyed,
                String keyText, int depth) {
            this.collection = collection
            this.fields = fields
            this.path = path
            this.optionKeyed = optionKeyed
            this.keyText = keyText
            this.depth = depth
        }

        static Scope options(String collection, MongoMappedFields fields) {
            new Scope(collection, fields, Collections.<String>emptyList(), true, null, 0)
        }

        /**
         * @return the scope of an element of an array in this scope: one index deeper into the field
         * at the path, so that a numeric key inside the element is data unless the elements are arrays
         */
        Scope element() {
            optionKeyed || path == null || path.isEmpty() ? this : new Scope(collection, fields, path, false, keyText, depth + 1)
        }

        private boolean indexed(List<String> at, int atDepth) {
            at != null && !at.isEmpty() && fields != null && fields.isIndexed(collection, at, atDepth)
        }

        /**
         * @return the scope of the value of a key in a document of this scope, with the key as it is recorded
         */
        Scope of(String key) {
            if (optionKeyed) {
                boolean fieldKeyed = FIELD_KEYED.contains(key)
                return new Scope(collection, fields, Collections.<String>emptyList(), !fieldKeyed, quote(key), 0)
            }
            if (key.startsWith('$')) {
                // an operator on an array, such as $elemMatch, describes its elements
                return new Scope(collection, fields, path, false, quote(key), indexed(path, depth) ? depth + 1 : depth)
            }
            List<String> current = path
            int currentDepth = depth
            String[] segments = key.split('\\.', -1)
            StringBuilder recorded = new StringBuilder()
            boolean anyKept = false
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i]
                boolean kept
                if (current == null) {
                    kept = false
                }
                else if (MongoMappedFields.isIndex(segment)) {
                    kept = indexed(current, currentDepth)
                    if (kept) {
                        currentDepth++
                    }
                    else {
                        current = null
                    }
                }
                else if (i == 0 && current.isEmpty()) {
                    kept = true
                    current = [segment]
                    currentDepth = 0
                }
                else {
                    List<String> deeper = new ArrayList<>(current)
                    deeper.add(segment)
                    kept = fields != null && fields.isField(collection, deeper)
                    current = kept ? deeper : null
                    currentDepth = 0
                }
                if (i > 0) {
                    recorded.append('.')
                }
                recorded.append(kept ? segment : PLACEHOLDER)
                anyKept |= kept
            }
            new Scope(collection, fields, current, false, anyKept ? quote(recorded.toString()) : PLACEHOLDER, currentDepth)
        }
    }
}
