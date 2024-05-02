/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.web

import grails.core.DefaultGrailsApplication
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class JSONBuilderTests {

    @BeforeEach
    void setUp() {
        def initializer = new ConvertersConfigurationInitializer(grailsApplication: new DefaultGrailsApplication())
        initializer.initialize()
    }

    @Test
    void testSimple() {
        def builder = new JSONBuilder()

        def result = builder.build {
            rootprop ="something"
        }

        assertEquals '{"rootprop":"something"}', result.toString()
    }

    @Test
    void testArrays() {
        def builder = new JSONBuilder()

        def result = builder.build {
            categories = ['a', 'b', 'c']
            rootprop ="something"
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something"}', result.toString()
    }

    @Test
    void testSubObjects() {
        def builder = new JSONBuilder()

        def result = builder.build {
            categories = ['a', 'b', 'c']
            rootprop ="something"
            test {
                subprop = 10
            }
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10}}', result.toString()
    }

    @Test
    void testAssignedObjects() {

        def builder = new JSONBuilder()

        def result = builder.build {
            categories = ['a', 'b', 'c']
            rootprop ="something"
            test = {
                subprop = 10
            }
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10}}', result.toString()
    }

    @Test
    void testNamedArgumentHandling() {
        def builder = new JSONBuilder()

        def result = builder.build {
            categories = ['a', 'b', 'c']
            rootprop ="something"
            test subprop:10, three:[1,2,3]
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10,"three":[1,2,3]}}', result.toString()
    }

    @Test
    void testArrayOfClosures() {
        def builder = new JSONBuilder()

        def result = builder.build {
            foo = [ { bar = "hello" } ]
        }

        assertEquals '{"foo":[{"bar":"hello"}]}', result.toString()
    }

    @Test
    void testRootElementList() {
        def builder = new JSONBuilder()

        def results = ['one', 'two', 'three']

        def result = builder.build {
            for (b in results) {
                element b
            }
        }

        assertEquals '["one","two","three"]', result.toString()

        result = builder.build {
            results
        }

        assertEquals '["one","two","three"]', result.toString()
    }

    @Test
    void testExampleFromReferenceGuide() {
        def builder = new JSONBuilder()

        def results = ['one', 'two', 'three']

        def result = builder.build {
            for (b in results) {
                element title:b
            }
        }

        assertEquals '[{"title":"one"},{"title":"two"},{"title":"three"}]', result.toString()

        result = builder.build {
            books = results.collect {
                [title:it]
            }
        }

        assertEquals '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}', result.toString()

        result = builder.build {
            books = array {
                for (b in results) {
                    book title:b
                }
            }
        }

        assertEquals '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}', result.toString()
    }

    @Test
    void testAppendToArray() {
        def builder = new JSONBuilder()

        def results = ['one', 'two', 'three']

        def result = builder.build {
            books = array { list ->
                for (b in results) {
                    list << [title:b]
                }
            }
        }

        assertEquals '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}', result.toString()
    }
}
