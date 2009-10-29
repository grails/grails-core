package grails.web

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer

/**
 * @author Graeme Rocher
 * @since 1.2
 */

public class JSONBuilderTests extends GroovyTestCase{

    void testSimple() {
        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
        def builder = new JSONBuilder()

        def result = builder.build {
            rootprop ="something"
        }

        assertEquals '{"rootprop":"something"}', result.toString()
    }


    void testArrays() {
        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
        def builder = new JSONBuilder()

        def result = builder.build {
            categories = ['a', 'b', 'c']
            rootprop ="something"
        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something"}', result.toString()        
    }

    void testSubObjects() {
        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
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

    void testAssignedObjects() {

        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
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

    void testNamedArgumentHandling() {
        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
        def builder = new JSONBuilder()

        def result = builder.build {
            categories = ['a', 'b', 'c']
            rootprop ="something"
            test subprop:10, three:[1,2,3]

        }

        assertEquals '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10,"three":[1,2,3]}}', result.toString()
    }


    void testArrayOfClosures() {
        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
        def builder = new JSONBuilder()

       
        def result = builder.build {
            foo = [ { bar = "hello" } ]
        }

        assertEquals '{"foo":[{"bar":"hello"}]}', result.toString()
    }

    void testRootElementList() {
        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
        def builder = new JSONBuilder()

        def results = ['one', 'two', 'three']

        def result = builder.build {

           for(b in results) {
               element b
           }
        }

        assertEquals '["one","two","three"]', result.toString()
    }

    void testExampleFromReferenceGuide() {
        def initializer = new ConvertersConfigurationInitializer()
        initializer.initialize()
        def builder = new JSONBuilder()

        def results = ['one', 'two', 'three']

        def result = builder.build {
           for(b in results) {
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
                for(b in results) {
                    book title:b
                }
           }
        }

        assertEquals '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}', result.toString()
    }
}