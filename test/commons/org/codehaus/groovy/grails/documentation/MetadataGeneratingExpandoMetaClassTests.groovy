package org.codehaus.groovy.grails.documentation

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import groovy.xml.StreamingMarkupBuilder

/**
 * @author Graeme Rocher
 * @since 1.2
 */
public class MetadataGeneratingExpandoMetaClassTests extends GroovyTestCase {

    void testGeneratedMetadata() {
        def emc = new MetadataGeneratingExpandoMetaClass(TestController)
        emc.initialize()


        DocumentationContext context = DocumentationContext.instance
        context.active=true
        context.artefactType="Controller"


        context.document "A test instance method"
        emc.testMethod = { String one, Integer two -> "test"}

        TestController.metaClass {
            context.document "A test property"
            getSomeProp = {-> "one" }

            anotherMethod { Integer i -> }



            'static' {
                context.document "A test static method"
                listTests { Map args -> }
            }
        }

        def c = new TestController()
        assertEquals "test", c.testMethod("one",2)

        println context.methods
        println context.staticMethods
        println context.properties
        DocumentedMethod method = context.methods.find { it.name == 'testMethod'}
        assertNotNull "should have added method to documentation context",method

        assertEquals "testMethod", method.name
        assertEquals "A test instance method", method.text
        assertEquals( [String, Integer] as Class[], method.arguments )
        assertEquals "Controller", method.artefact
        assertEquals TestController, method.type

        method = context.methods.find { it.name == 'getSomeProp'}
        assertNotNull "should have added method to documentation context",method

        assertEquals "A test property", method.text
        assertEquals "getSomeProp", method.name
        assertEquals( [] as Class[], method.arguments )
        assertEquals "Controller", method.artefact
        assertEquals TestController, method.type


        method = context.staticMethods.find { it.name == 'listTests'}
        assertNotNull "should have added method to documentation context",method

        assertEquals "A test static method", method.text
        assertEquals "listTests", method.name
        assertEquals( [Map] as Class[], method.arguments )
        assertEquals "Controller", method.artefact
        assertEquals TestController, method.type


        def prop = context.properties.find { it.name == 'someProp' }


        assertNotNull "should have added property to documentation context",prop
        assertEquals "someProp", prop.name

        assertEquals "Controller", prop.artefact
        assertEquals TestController, prop.type



    }


}
class TestController {}