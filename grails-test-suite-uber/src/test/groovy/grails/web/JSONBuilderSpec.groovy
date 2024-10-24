package grails.web

import grails.core.DefaultGrailsApplication
import groovy.json.JsonBuilder
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class JSONBuilderSpec extends Specification {

    void setup() {
        def initializer = new ConvertersConfigurationInitializer(grailsApplication: new DefaultGrailsApplication())
        initializer.initialize()
    }

    void testSimple() {

        when:
        def result = new JsonBuilder({
            rootprop "something"
        })

        then:
        '{"rootprop":"something"}' == result.toString()
    }

    void testArrays() {

        when:
        def result = new JsonBuilder({
            categories 'a', 'b', 'c'
            rootprop "something"
        })

        then:
        '{"categories":["a","b","c"],"rootprop":"something"}' == result.toString()
    }

    void testSubObjects() {

        when:
        def result = new JsonBuilder({
            categories 'a', 'b', 'c'
            rootprop "something"
            test {
                subprop 10
            }
        })

        then:
        '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10}}' == result.toString()
    }

    void testAssignedObjects() {

        when:
        def result = new JsonBuilder({
            categories 'a', 'b', 'c'
            rootprop "something"
            test {
                subprop 10
            }
        })

        then:
        '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10}}' == result.toString()
    }

    void testNamedArgumentHandling() {

        when:
        def result = new JsonBuilder({
            categories 'a', 'b', 'c'
            rootprop "something"
            test {
                subprop 10
                three 1, 2, 3
            }
        })

        then:
        '{"categories":["a","b","c"],"rootprop":"something","test":{"subprop":10,"three":[1,2,3]}}' == result.toString()
    }

    @Ignore("It is not possible to use closures in the new JsonBuilder. This test is not supported.")
    void testArrayOfClosures() {

        when:
        def result = new JsonBuilder({
            foo [ { bar "hello" } ]
        })

        then:
        '{"foo":[{"bar":"hello"}]}' == result.toString()
    }

    void testRootElementList() {

        given:
        def results = ['one', 'two', 'three']

        when:
        def result = new JsonBuilder(results)

        then:
        '["one","two","three"]' == result.toString()
    }

    void testExampleFromReferenceGuide() {

        given:
        final List<String> results = ['one', 'two', 'three']
        def result

        when:
        result = new JsonBuilder({
            List<Map<String, String>> books = []
            for (t in results) {
                books << [title: t]
            }
            return books
        }())

        then:
        '[{"title":"one"},{"title":"two"},{"title":"three"}]' == result.toString()

        when:
        result = new JsonBuilder({
            books results.collect { [title: it] }
        })

        then:
        '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}' == result.toString()

        when:
        new JsonBuilder({
            List<Map<String, String>> array = []
            for (b in results) {
                array.add(title: b)
            }
            books array
        })


        then:
        '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}' == result.toString()
    }


    @Ignore("This isn't supported with the new JsonBuilder. This test is not supported.")
    void testAppendToArray() {
        def builder = new JSONBuilder()

        def results = ['one', 'two', 'three']

        def result = builder.build {
            books = array { list ->
                for (b in results) {
                    list << [title: b]
                }
            }
        }

        assertEquals '{"books":[{"title":"one"},{"title":"two"},{"title":"three"}]}', result.toString()
    }
}
