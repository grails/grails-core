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
package org.grails.plugins.web.rest.render.xml

import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.core.StreamWriteConstraints
import tools.jackson.core.StreamWriteFeature
import tools.jackson.core.util.JsonGeneratorDelegate
import tools.jackson.databind.JacksonSerializable
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.util.RawValue
import tools.jackson.databind.util.TokenBuffer

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

/**
 * Writes a problem in the XML format of RFC 9457, as Spring's Jackson XML converter writes it, for an application without
 * {@code jackson-dataformat-xml}: a {@code problem} element in the {@code urn:ietf:rfc:7807} namespace, with the members
 * of the problem, those of a subclass included, and its extension members, as a mapper with Spring's mix-in for problems
 * writes them, every member it writes included, also one whose name it writes twice, the properties of a bean in no
 * namespace and the entries of a map in that of the map, as Spring's converter writes them. It fails, as Spring's
 * converters fail, for a problem nested deeper than the mapper's {@code StreamWriteConstraints} allow. A list is written as an element for each of its values, wrapped in an element of its name where
 * it is the property of a bean, and a {@code null} as an {@code xsi:nil} element.
 *
 * @since 9.0
 */
@PackageScope
@CompileStatic
final class ProblemXml {

    static final String NAMESPACE = 'urn:ietf:rfc:7807'

    private static final String XSI = 'http://www.w3.org/2001/XMLSchema-instance'

    /** The names each mapper writes the members of RFC 9457 under, which its naming strategy may change. */
    private static final Map<ObjectMapper, Set<String>> MEMBER_NAMES =
            Collections.synchronizedMap(new WeakHashMap<ObjectMapper, Set<String>>())

    // The JDK's own writer, whatever StAX implementation another library puts on the classpath
    private static final XMLOutputFactory OUTPUT = XMLOutputFactory.newDefaultFactory()

    private ProblemXml() {
    }

    /**
     * @param problem the problem
     * @param writer the response writer
     * @param mapper a mapper with Spring's {@code ProblemDetailJacksonMixin}, which writes the problem as Spring's
     *        converters write it
     */
    static void write(ProblemDetail problem, Writer writer, ObjectMapper mapper) {
        XMLStreamWriter xml = OUTPUT.createXMLStreamWriter(writer)
        xml.writeStartElement('problem')
        xml.writeDefaultNamespace(NAMESPACE)
        boolean plainDecimals = mapper.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
        // the tokens the mapper writes, read back without its read features, with every member it writes, one whose name
        // it writes twice included, as Spring's converters write them
        TokenBuffer buffer = ProblemBeans.marking(mapper).writer().without(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueIntoBuffer(new NestingLimited(problem, mapper.tokenStreamFactory().streamWriteConstraints()))
        Object converted
        JsonParser parser = buffer.asParser()
        try {
            JsonToken first = parser.nextToken()
            if (first != JsonToken.START_OBJECT) {
                throw MismatchedInputException.from(parser, Map, 'A problem is written as an object')
            }
            converted = read(parser, first)
        }
        finally {
            parser.close()
        }
        boolean extension = false
        for (Map.Entry<String, Object> entry : ((Members) converted).entries) {
            String member = entry.key
            if (member == ProblemBeans.ANY_START || member == ProblemBeans.ANY_END) {
                extension = member == ProblemBeans.ANY_START
            }
            else if (!ProblemBeans.isMarker(member)) {
                // Spring's converter writes the members of RFC 9457 and the extension members, which the any-getter
                // writes, in its namespace, and the properties of a subclass, as those of any bean, in none
                boolean property = !(extension || memberNames(mapper).contains(member))
                element(xml, writer, member, entry.value, property ? '' : NAMESPACE, NAMESPACE, plainDecimals, property)
            }
        }
        xml.writeEndElement()
        xml.flush()
    }

    /**
     * @param namespace the namespace of the element
     * @param enclosing the namespace of the element it is in, which it declares its own where they differ
     */
    private static void element(XMLStreamWriter xml, Writer writer, String name, Object value, String namespace,
            String enclosing, boolean plainDecimals, boolean property) {
        if (value instanceof List) {
            List<?> listed = (List<?>) value
            // a bean written as an array is not a collection; its marker is first, unless a serializer modifier of another
            // module of the mapper, which runs after the one that adds it, orders the properties of the bean
            int marker = listed.indexOf(ProblemBeans.ARRAY_MARKER)
            boolean array = marker >= 0
            List<?> values = listed
            if (array) {
                values = new ArrayList<Object>(listed)
                values.removeAt(marker)
            }
            if (property && !array) {
                // a collection a bean property holds is wrapped in an element of its name, as Jackson's XML mapper wraps it
                xml.writeStartElement(name)
                declare(xml, namespace, enclosing)
                for (Object each : values) {
                    element(xml, writer, name, each, namespace, namespace, plainDecimals, false)
                }
                xml.writeEndElement()
            }
            else {
                for (Object each : values) {
                    element(xml, writer, name, each, namespace, enclosing, plainDecimals, false)
                }
            }
        }
        else if (value == null) {
            xml.writeEmptyElement(name)
            declare(xml, namespace, enclosing)
            xml.writeNamespace('xsi', XSI)
            xml.writeAttribute('xsi', XSI, 'nil', 'true')
        }
        else {
            xml.writeStartElement(name)
            declare(xml, namespace, enclosing)
            if (value instanceof Members) {
                Members members = (Members) value
                // the properties of a bean are in no namespace, and the entries of a map in that of the map
                boolean bean = members.bean
                String inner = bean ? '' : namespace
                for (Map.Entry<String, Object> member : members.entries) {
                    if (!ProblemBeans.isMarker(member.key)) {
                        element(xml, writer, member.key, member.value, inner, namespace, plainDecimals, bean)
                    }
                }
            }
            else if (value instanceof RawValue) {
                // written as it is, as Jackson writes a raw value: the start tag is closed, then the text follows it
                xml.writeCharacters('')
                xml.flush()
                writer.write(String.valueOf(((RawValue) value).rawValue()))
            }
            else {
                characters(xml, text(value, plainDecimals))
            }
            xml.writeEndElement()
        }
    }

    /** The names the mapper writes the members of RFC 9457 under, read off a problem it writes with all of them. */
    private static Set<String> memberNames(ObjectMapper mapper) {
        MEMBER_NAMES.computeIfAbsent(mapper) { ObjectMapper writing ->
            ProblemDetail sample = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, 'detail')
            sample.type = URI.create('urn:sample')
            sample.title = 'title'
            sample.instance = URI.create('/instance')
            Set<String> names = new HashSet<String>()
            JsonParser parser = writing.writer().without(SerializationFeature.WRAP_ROOT_VALUE).writeValueIntoBuffer(sample).asParser()
            try {
                parser.nextToken()
                for (JsonToken token = parser.nextToken(); token == JsonToken.PROPERTY_NAME; token = parser.nextToken()) {
                    names << parser.currentName()
                    parser.nextToken()
                    parser.skipChildren()
                }
            }
            finally {
                parser.close()
            }
            names
        }
    }

    /** Reads the value that starts with the token given, an object as its members, in their order. */
    private static Object read(JsonParser parser, JsonToken token) {
        switch (token) {
            case JsonToken.START_OBJECT:
                List<Map.Entry<String, Object>> entries = []
                for (JsonToken next = parser.nextToken(); next == JsonToken.PROPERTY_NAME; next = parser.nextToken()) {
                    String name = parser.currentName()
                    entries << (Map.Entry<String, Object>) new AbstractMap.SimpleImmutableEntry<String, Object>(name,
                            read(parser, parser.nextToken()))
                }
                return new Members(entries)
            case JsonToken.START_ARRAY:
                List<Object> values = []
                for (JsonToken next = parser.nextToken(); next != JsonToken.END_ARRAY; next = parser.nextToken()) {
                    values << read(parser, next)
                }
                return values
            case JsonToken.VALUE_STRING:
                return parser.getString()
            case JsonToken.VALUE_NUMBER_INT:
            case JsonToken.VALUE_NUMBER_FLOAT:
                // the number the serializer wrote
                return parser.getNumberValue()
            case JsonToken.VALUE_TRUE:
                return Boolean.TRUE
            case JsonToken.VALUE_FALSE:
                return Boolean.FALSE
            case JsonToken.VALUE_EMBEDDED_OBJECT:
                // binary data and raw values
                return parser.getEmbeddedObject()
            default:
                return null
        }
    }

    /**
     * A problem written as it is, but for failing where it is nested deeper than the mapper writes, as the mapper's
     * generator fails, which a buffer of tokens does not check.
     */
    private static final class NestingLimited implements JacksonSerializable {

        private final ProblemDetail problem

        private final StreamWriteConstraints constraints

        NestingLimited(ProblemDetail problem, StreamWriteConstraints constraints) {
            this.problem = problem
            this.constraints = constraints
        }

        @Override
        void serialize(JsonGenerator generator, SerializationContext context) {
            context.writeValue(new NestingLimitedGenerator(generator, constraints), problem)
        }

        @Override
        void serializeWithType(JsonGenerator generator, SerializationContext context, TypeSerializer typeSerializer) {
            serialize(generator, context)
        }
    }

    /** A generator that fails as the mapper's generator does where what it writes is nested deeper than it writes. */
    private static final class NestingLimitedGenerator extends JsonGeneratorDelegate {

        private final StreamWriteConstraints constraints

        NestingLimitedGenerator(JsonGenerator generator, StreamWriteConstraints constraints) {
            // values written as they are, such as by writePOJO, are written through this generator too
            super(generator, false)
            this.constraints = constraints
        }

        @Override
        JsonGenerator writeStartObject() {
            super.writeStartObject()
            validated()
        }

        @Override
        JsonGenerator writeStartObject(Object forValue) {
            super.writeStartObject(forValue)
            validated()
        }

        @Override
        JsonGenerator writeStartObject(Object forValue, int size) {
            super.writeStartObject(forValue, size)
            validated()
        }

        @Override
        JsonGenerator writeStartArray() {
            super.writeStartArray()
            validated()
        }

        @Override
        JsonGenerator writeStartArray(Object forValue) {
            super.writeStartArray(forValue)
            validated()
        }

        @Override
        JsonGenerator writeStartArray(Object forValue, int size) {
            super.writeStartArray(forValue, size)
            validated()
        }

        // an array of int, long or double, which the delegate writes as a whole, is one level deeper than what holds it
        @Override
        JsonGenerator writeArray(int[] array, int offset, int length) {
            constraints.validateNestingDepth(streamWriteContext().nestingDepth + 1)
            super.writeArray(array, offset, length)
        }

        @Override
        JsonGenerator writeArray(long[] array, int offset, int length) {
            constraints.validateNestingDepth(streamWriteContext().nestingDepth + 1)
            super.writeArray(array, offset, length)
        }

        @Override
        JsonGenerator writeArray(double[] array, int offset, int length) {
            constraints.validateNestingDepth(streamWriteContext().nestingDepth + 1)
            super.writeArray(array, offset, length)
        }

        private JsonGenerator validated() {
            constraints.validateNestingDepth(streamWriteContext().nestingDepth)
            this
        }
    }

    /** The members of an object as the mapper writes them, in their order, a name it writes twice included. */
    private static final class Members {

        final List<Map.Entry<String, Object>> entries

        Members(List<Map.Entry<String, Object>> entries) {
            this.entries = entries
        }

        boolean isBean() {
            entries.any { Map.Entry<String, Object> entry -> ProblemBeans.isMarker(entry.key) }
        }
    }

    /**
     * Writes the text as the writer of Spring's XML converter writes it, where the JDK's writer writes it otherwise: a
     * carriage return, which an XML parser would read as a line feed, and U+FFFE and U+FFFF as character references.
     */
    private static void characters(XMLStreamWriter xml, String text) {
        checked(text)
        int start = 0
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i)
            if (c == (char) 0xD || c == (char) 0xFFFE || c == (char) 0xFFFF) {
                xml.writeCharacters(text.substring(start, i))
                xml.writeEntityRef("#x${Integer.toHexString((int) c)}")
                start = i + 1
            }
        }
        xml.writeCharacters(text.substring(start))
    }

    /**
     * Fails, as the writer of Spring's XML converter fails, where XML 1.0 cannot hold a character of the text, which the
     * JDK's writer does not check, and then where it has half of a surrogate pair, which the JDK's writer writes as
     * {@code ?}.
     */
    private static void checked(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i)
            if (c == (char) 0) {
                throw new XMLStreamException('Invalid null character in text to output')
            }
            if (c < (char) 0x20 && c != (char) 0x9 && c != (char) 0xA && c != (char) 0xD) {
                throw new XMLStreamException("Invalid white space character (0x${Integer.toHexString((int) c)}) in text to " +
                        'output (in xml 1.1, could output as a character entity)')
            }
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i)
            if (Character.isHighSurrogate(c)) {
                // it fails on the character written after it, which is the < of the end tag where the text ends with it
                char next = i + 1 < text.length() ? text.charAt(i + 1) : (char) '<'
                if (!Character.isLowSurrogate(next)) {
                    throw new XMLStreamException("Broken surrogate pair: first char 0x${Integer.toHexString((int) c)}, " +
                            "second 0x${Integer.toHexString((int) next)}; illegal combination")
                }
                i++
            }
            else if (Character.isLowSurrogate(c)) {
                throw new XMLStreamException("Unmatched second part of surrogate pair (0x${Integer.toHexString((int) c)})")
            }
        }
    }

    /**
     * The text of a value, as Jackson writes it: binary data in Base64, a decimal as the mapper's features say, and an
     * infinite floating-point number as XML Schema writes it, as Jackson's XML mapper does by default.
     */
    private static String text(Object value, boolean plainDecimals) {
        if (value instanceof byte[]) {
            return Base64.encoder.encodeToString((byte[]) value)
        }
        if ((value instanceof Double || value instanceof Float) && ((Number) value).doubleValue().infinite) {
            return ((Number) value).doubleValue() > 0 ? 'INF' : '-INF'
        }
        if (value instanceof BigDecimal && plainDecimals) {
            return ((BigDecimal) value).toPlainString()
        }
        value.toString()
    }

    private static void declare(XMLStreamWriter xml, String namespace, String enclosing) {
        if (namespace != enclosing) {
            xml.writeDefaultNamespace(namespace)
        }
    }
}
