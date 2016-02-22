package org.grails.web.json;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */



import groovy.json.JsonException;
import groovy.json.JsonOutput;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyObjectSupport;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Temporary fork of Groovy's JsonBuilder until Groovy 2.4.6 is released.
 *
 * DO NOT USE, this class will be deleted when Groovy 2.4.6 is released!
 *
 * @author Tim Yates
 * @author Andrey Bloschetsov
 * @author Graeme Rocher
 */
@Deprecated
public class StreamingJsonBuilder extends GroovyObjectSupport {
    static final char OPEN_BRACKET = '[';
    static final char CLOSE_BRACKET = ']';
    static final char OPEN_BRACE = '{';
    static final char CLOSE_BRACE = '}';
    static final char COLON = ':';
    static final char COMMA = ',';
    static final char SPACE = ' ';
    static final char NEW_LINE = '\n';
    static final char QUOTE = '"';
    private static final String DOUBLE_CLOSE_BRACKET = "}}";
    private static final String COLON_WITH_OPEN_BRACE = ":{";

    private Writer writer;

    /**
     * Instantiates a JSON builder.
     *
     * @param writer A writer to which Json will be written
     */
    public StreamingJsonBuilder(Writer writer) {
        this.writer = writer;
    }

    /**
     * Instantiates a JSON builder, possibly with some existing data structure.
     *
     * @param writer  A writer to which Json will be written
     * @param content a pre-existing data structure, default to null
     */
    public StreamingJsonBuilder(Writer writer, Object content) throws IOException {
        this(writer);
        if (content != null) {
            writer.write(JsonOutput.toJson(content));
        }
    }

    /**
     * Named arguments can be passed to the JSON builder instance to create a root JSON object
     * <p>
     * Example:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *   def json = new groovy.json.StreamingJsonBuilder(w)
     *   json name: "Tim", age: 31
     *
     *   assert w.toString() == '{"name":"Tim","age":31}'
     * }
     * </pre>
     *
     * @param m a map of key / value pairs
     * @return a map of key / value pairs
     */
    public Object call(Map m) throws IOException {
        writer.write(JsonOutput.toJson(m));

        return m;
    }

    /**
     * The empty args call will create a key whose value will be an empty JSON object:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json.person()
     *
     *     assert w.toString() == '{"person":{}}'
     * }
     * </pre>
     *
     * @param name The name of the empty object to create
     * @throws IOException
     */
    public void call(String name) throws IOException {
        writer.write(JsonOutput.toJson(Collections.singletonMap(name, Collections.emptyMap())));
    }

    /**
     * A list of elements as arguments to the JSON builder creates a root JSON array
     * <p>
     * Example:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *   def json = new groovy.json.StreamingJsonBuilder(w)
     *   def result = json([1, 2, 3])
     *
     *   assert result == [ 1, 2, 3 ]
     *   assert w.toString() == "[1,2,3]"
     * }
     * </pre>
     *
     * @param l a list of values
     * @return a list of values
     */
    public Object call(List l) throws IOException {
        writer.write(JsonOutput.toJson(l));

        return l;
    }

    /**
     * Varargs elements as arguments to the JSON builder create a root JSON array
     * <p>
     * Example:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *   def json = new groovy.json.StreamingJsonBuilder(w)
     *   def result = json 1, 2, 3
     *
     *   assert result instanceof List
     *   assert w.toString() == "[1,2,3]"
     * }
     * </pre>

     * @param args an array of values
     * @return a list of values
     */
    public Object call(Object... args) throws IOException {
        return call(Arrays.asList(args));
    }

    /**
     * A collection and closure passed to a JSON builder will create a root JSON array applying
     * the closure to each object in the collection
     * <p>
     * Example:
     * <pre class="groovyTestCase">
     * class Author {
     *      String name
     * }
     * def authors = [new Author (name: "Guillaume"), new Author (name: "Jochen"), new Author (name: "Paul")]
     *
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json authors, { Author author ->
     *         name author.name
     *     }
     *
     *     assert w.toString() == '[{"name":"Guillaume"},{"name":"Jochen"},{"name":"Paul"}]'
     * }
     * </pre>
     * @param coll a collection
     * @param c a closure used to convert the objects of coll
     */
    public Object call(Iterable coll, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
        return StreamingJsonDelegate.writeCollectionWithClosure(writer, coll, c);
    }

    /**
     * Delegates to {@link #call(Iterable, Closure)}
     */
    public Object call(Collection coll, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
        return call((Iterable)coll, c);
    }

    /**
     * A closure passed to a JSON builder will create a root JSON object
     * <p>
     * Example:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *   def json = new groovy.json.StreamingJsonBuilder(w)
     *   json {
     *      name "Tim"
     *      age 39
     *   }
     *
     *   assert w.toString() == '{"name":"Tim","age":39}'
     * }
     * </pre>
     *
     * @param c a closure whose method call statements represent key / values of a JSON object
     */
    public Object call(@DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
        writer.write(OPEN_BRACE);
        StreamingJsonDelegate.cloneDelegateAndGetContent(writer, c);
        writer.write(CLOSE_BRACE);

        return null;
    }

    /**
     * A name and a closure passed to a JSON builder will create a key with a JSON object
     * <p>
     * Example:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *   def json = new groovy.json.StreamingJsonBuilder(w)
     *   json.person {
     *      name "Tim"
     *      age 39
     *   }
     *
     *   assert w.toString() == '{"person":{"name":"Tim","age":39}}'
     * }
     * </pre>
     *
     * @param name The key for the JSON object
     * @param c a closure whose method call statements represent key / values of a JSON object
     */
    public void call(String name, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
        writer.write(OPEN_BRACE);
        writer.write(JsonOutput.toJson(name));
        writer.write(COLON);
        call(c);
        writer.write(CLOSE_BRACE);
    }

    /**
     * A name, a collection and closure passed to a JSON builder will create a root JSON array applying
     * the closure to each object in the collection
     * <p>
     * Example:
     * <pre class="groovyTestCase">
     * class Author {
     *      String name
     * }
     * def authors = [new Author (name: "Guillaume"), new Author (name: "Jochen"), new Author (name: "Paul")]
     *
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json.people authors, { Author author ->
     *         name author.name
     *     }
     *
     *     assert w.toString() == '{"people":[{"name":"Guillaume"},{"name":"Jochen"},{"name":"Paul"}]}'
     * }
     * </pre>
     * @param coll a collection
     * @param c a closure used to convert the objects of coll
     */
    public void call(String name, Iterable coll, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
        writer.write(OPEN_BRACE);
        writer.write(JsonOutput.toJson(name));
        writer.write(COLON);
        call(coll, c);
        writer.write(CLOSE_BRACE);
    }

    /**
     * Delegates to {@link #call(String, Iterable, Closure)}
     */
    public void call(String name, Collection coll, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
        call(name, (Iterable)coll, c);
    }

    /**
     * If you use named arguments and a closure as last argument,
     * the key/value pairs of the map (as named arguments)
     * and the key/value pairs represented in the closure
     * will be merged together &mdash;
     * the closure properties overriding the map key/values
     * in case the same key is used.
     *
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json.person(name: "Tim", age: 35) { town "Manchester" }
     *
     *     assert w.toString() == '{"person":{"name":"Tim","age":35,"town":"Manchester"}}'
     * }
     * </pre>
     *
     * @param name The name of the JSON object
     * @param map The attributes of the JSON object
     * @param callable Additional attributes of the JSON object represented by the closure
     * @throws IOException
     */
    public void call(String name, Map map, @DelegatesTo(StreamingJsonDelegate.class) Closure callable) throws IOException {
        writer.write(OPEN_BRACE);
        writer.write(JsonOutput.toJson(name));
        writer.write(COLON_WITH_OPEN_BRACE);
        boolean first = true;
        for (Object it : map.entrySet()) {
            if (!first) {
                writer.write(COMMA);
            } else {
                first = false;
            }

            Map.Entry entry = (Map.Entry) it;
            writer.write(JsonOutput.toJson(entry.getKey()));
            writer.write(COLON);
            writer.write(JsonOutput.toJson(entry.getValue()));
        }
        StreamingJsonDelegate.cloneDelegateAndGetContent(writer, callable, map.size() == 0);
        writer.write(DOUBLE_CLOSE_BRACKET);
    }

    /**
     * A method call on the JSON builder instance will create a root object with only one key
     * whose name is the name of the method being called.
     * This method takes as arguments:
     * <ul>
     *     <li>a closure</li>
     *     <li>a map (ie. named arguments)</li>
     *     <li>a map and a closure</li>
     *     <li>or no argument at all</li>
     * </ul>
     * <p>
     * Example with a classical builder-style:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json.person {
     *         name "Tim"
     *          age 28
     *     }
     *
     *     assert w.toString() == '{"person":{"name":"Tim","age":28}}'
     * }
     * </pre>
     *
     * Or alternatively with a method call taking named arguments:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json.person name: "Tim", age: 32
     *
     *     assert w.toString() == '{"person":{"name":"Tim","age":32}}'
     * }
     * </pre>
     *
     * If you use named arguments and a closure as last argument,
     * the key/value pairs of the map (as named arguments)
     * and the key/value pairs represented in the closure
     * will be merged together &mdash;
     * the closure properties overriding the map key/values
     * in case the same key is used.
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json.person(name: "Tim", age: 35) { town "Manchester" }
     *
     *     assert w.toString() == '{"person":{"name":"Tim","age":35,"town":"Manchester"}}'
     * }
     * </pre>
     *
     * The empty args call will create a key whose value will be an empty JSON object:
     * <pre class="groovyTestCase">
     * new StringWriter().with { w ->
     *     def json = new groovy.json.StreamingJsonBuilder(w)
     *     json.person()
     *
     *     assert w.toString() == '{"person":{}}'
     * }
     * </pre>
     *
     * @param name the single key
     * @param args the value associated with the key
     */
    public Object invokeMethod(String name, Object args) {
        boolean notExpectedArgs = false;
        if (args != null && Object[].class.isAssignableFrom(args.getClass())) {
            Object[] arr = (Object[]) args;
            try {
                switch(arr.length) {
                    case 0:
                        call(name);
                        break;
                    case 1:
                        if (arr[0] instanceof Closure) {
                            final Closure callable = (Closure) arr[0];
                            call(name, callable);
                        } else if (arr[0] instanceof Map) {
                            final Map<String, Map> map = Collections.singletonMap(name, (Map) arr[0]);
                            call(map);
                        } else {
                            notExpectedArgs = true;
                        }
                        break;
                    case 2:
                        final Object first = arr[0];
                        final Object second = arr[1];
                        final boolean isClosure = second instanceof Closure;

                        if(isClosure && first instanceof Map ) {
                            final Closure callable = (Closure) second;
                            call(name, (Map)first, callable);
                        }
                        else if(isClosure && first instanceof Iterable) {
                            final Iterable coll = (Iterable) first;
                            final Closure callable = (Closure) second;
                            call(name, coll, callable);
                        }
                        else if(isClosure && first.getClass().isArray()) {
                            final Iterable coll = Arrays.asList((Object[])first);
                            final Closure callable = (Closure) second;
                            call(name, coll, callable);
                        }
                        else {
                            notExpectedArgs = true;
                        }
                        break;
                    default:
                        notExpectedArgs = true;
                }
            } catch (IOException ioe) {
                throw new JsonException(ioe);
            }
        } else {
            notExpectedArgs = true;
        }

        if (!notExpectedArgs) {
            return this;
        } else {
            throw new JsonException("Expected no arguments, a single map, a single closure, or a map and closure as arguments.");
        }
    }

    /**
     * The delegate used when invoking closures
     */
    public static class StreamingJsonDelegate extends GroovyObjectSupport {

        protected final Writer writer;
        protected boolean first;
        protected State state;


        public StreamingJsonDelegate(Writer w, boolean first) {
            this.writer = w;
            this.first = first;
        }

        /**
         * @return Obtains the current writer
         */
        public Writer getWriter() {
            return writer;
        }

        public Object invokeMethod(String name, Object args) {
            if (args != null && Object[].class.isAssignableFrom(args.getClass())) {
                try {
                    Object[] arr = (Object[]) args;

                    final int len = arr.length;
                    switch (len) {
                        case 1:
                            final Object value = arr[0];
                            if(value instanceof Closure) {
                                call(name, (Closure)value);
                            }
                            else {
                                call(name, value);
                            }
                            return null;
                        case 2:
                            if(arr[len -1] instanceof Closure) {
                                final Object obj = arr[0];
                                final Closure callable = (Closure) arr[1];
                                if(obj instanceof Iterable) {
                                    call(name, (Iterable)obj, callable);
                                    return null;
                                }
                                else if(obj.getClass().isArray()) {
                                    call(name, Arrays.asList( (Object[])obj), callable);
                                    return null;
                                }
                                else {
                                    call(name, obj, callable);
                                    return null;
                                }
                            }
                        default:
                            final List<Object> list = Arrays.asList(arr);
                            call(name, list);

                    }
                } catch (IOException ioe) {
                    throw new JsonException(ioe);
                }
            }

            return this;
        }

        /**
         * Writes the name and a JSON array
         * @param name The name of the JSON attribute
         * @param list The list representing the array
         * @throws IOException
         */
        public void call(String name, List<Object> list) throws IOException {
            writeName(name);
            writeArray(list);
        }

        /**
         * Writes the name and a JSON array
         * @param name The name of the JSON attribute
         * @param array The list representing the array
         * @throws IOException
         */
        public void call(String name, Object...array) throws IOException {
            writeName(name);
            writeArray(Arrays.asList(array));
        }

        /**
         * A collection and closure passed to a JSON builder will create a root JSON array applying
         * the closure to each object in the collection
         * <p>
         * Example:
         * <pre class="groovyTestCase">
         * class Author {
         *      String name
         * }
         * def authorList = [new Author (name: "Guillaume"), new Author (name: "Jochen"), new Author (name: "Paul")]
         *
         * new StringWriter().with { w ->
         *     def json = new groovy.json.StreamingJsonBuilder(w)
         *     json.book {
         *        authors authorList, { Author author ->
         *         name author.name
         *       }
         *     }
         *
         *     assert w.toString() == '{"book":{"authors":[{"name":"Guillaume"},{"name":"Jochen"},{"name":"Paul"}]}}'
         * }
         * </pre>
         * @param coll a collection
         * @param c a closure used to convert the objects of coll
         */
        public void call(String name, Iterable coll, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
            writeName(name);
            writeObjects(coll, c);
        }

        /**
         * Delegates to {@link #call(String, Iterable, Closure)}
         */
        public void call(String name, Collection coll, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
            call(name, (Iterable)coll, c);
        }

        /**
         * Writes the name and value of a JSON attribute
         *
         * @param name The attribute name
         * @param value The value
         * @throws IOException
         */
        public void call(String name, Object value) throws IOException {
            writeName(name);
            writeValue(value);
        }


        /**
         * Writes the name and value of a JSON attribute
         *
         * @param name The attribute name
         * @param value The value
         * @throws IOException
         */
        public void call(String name, Object value, @DelegatesTo(StreamingJsonDelegate.class) Closure callable) throws IOException {
            writeName(name);
            verifyValue();
            writeObject(writer, value, callable);
        }
        /**
         * Writes the name and another JSON object
         *
         * @param name The attribute name
         * @param value The value
         * @throws IOException
         */
        public void call(String name,@DelegatesTo(StreamingJsonDelegate.class) Closure value) throws IOException {
            writeName(name);
            verifyValue();
            writer.write(OPEN_BRACE);
            StreamingJsonDelegate.cloneDelegateAndGetContent(writer, value);
            writer.write(CLOSE_BRACE);

        }
        /**
         * Writes an unescaped value. Note: can cause invalid JSON if passed JSON is invalid
         *
         * @param name The attribute name
         * @param json The value
         * @throws IOException
         */
        public void call(String name, JsonOutput.JsonUnescaped json) throws IOException {
            writeName(name);
            writer.write(json.toString());
        }


        private void writeObjects(Iterable coll, @DelegatesTo(StreamingJsonDelegate.class) Closure c) throws IOException {
            verifyValue();
            writeCollectionWithClosure(writer, coll, c);
        }

        protected void verifyValue() {
            if(state == State.VALUE) {
                throw new IllegalStateException("Cannot write value when value has just been written. Write a name first!");
            }
            else {
                state = State.VALUE;
            }
        }


        protected void writeName(String name) throws IOException {
            if(state == State.NAME) {
                throw new IllegalStateException("Cannot write a name when a name has just been written. Write a value first!");
            }
            else {
                this.state = State.NAME;
            }
            if (!first) {
                writer.write(COMMA);
            } else {
                first = false;
            }
            writer.write(JsonOutput.toJson(name));
            writer.write(COLON);
        }

        protected void writeValue(Object value) throws IOException {
            verifyValue();
            writer.write(JsonOutput.toJson(value));
        }

        protected void writeArray(List<Object> list) throws IOException {
            verifyValue();
            writer.write(JsonOutput.toJson(list));
        }

        public static boolean isCollectionWithClosure(Object[] args) {
            return args.length == 2 && args[0] instanceof Iterable && args[1] instanceof Closure;
        }

        public static Object writeCollectionWithClosure(Writer writer, Collection coll, @DelegatesTo(StreamingJsonDelegate.class)  Closure closure) throws IOException {
            return writeCollectionWithClosure(writer, (Iterable)coll, closure);
        }

        public static Object writeCollectionWithClosure(Writer writer, Iterable coll, @DelegatesTo(StreamingJsonDelegate.class)  Closure closure) throws IOException {
            writer.write(OPEN_BRACKET);
            boolean first = true;
            for (Object it : coll) {
                if (!first) {
                    writer.write(COMMA);
                } else {
                    first = false;
                }

                writeObject(writer, it, closure);
            }
            writer.write(CLOSE_BRACKET);

            return writer;
        }

        private static void writeObject(Writer writer, Object object,@DelegatesTo(StreamingJsonDelegate.class)   Closure closure) throws IOException {
            writer.write(OPEN_BRACE);
            curryDelegateAndGetContent(writer, closure, object);
            writer.write(CLOSE_BRACE);
        }

        public static void cloneDelegateAndGetContent(Writer w, @DelegatesTo(StreamingJsonDelegate.class)  Closure c)
        {
            cloneDelegateAndGetContent(w, c, true);
        }

        public static void cloneDelegateAndGetContent(Writer w,@DelegatesTo(StreamingJsonDelegate.class)   Closure c, boolean first) {
            StreamingJsonDelegate delegate = new StreamingJsonDelegate(w, first);
            Closure cloned = (Closure) c.clone();
            cloned.setDelegate(delegate);
            cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
            cloned.call();
        }

        public static void curryDelegateAndGetContent(Writer w, @DelegatesTo(StreamingJsonDelegate.class)  Closure c, Object o) {
            curryDelegateAndGetContent(w, c, o, true);
        }

        public static void curryDelegateAndGetContent(Writer w, @DelegatesTo(StreamingJsonDelegate.class)   Closure c, Object o, boolean first) {
            StreamingJsonDelegate delegate = new StreamingJsonDelegate(w, first);
            Closure curried = c.curry(o);
            curried.setDelegate(delegate);
            curried.setResolveStrategy(Closure.DELEGATE_FIRST);
            curried.call();
        }

        private enum State {
            NAME, VALUE
        }
    }
}


