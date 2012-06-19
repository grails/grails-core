package org.codehaus.groovy.grails.documentation

import groovy.xml.StreamingMarkupBuilder

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class MetadataGeneratingExpandoMetaClassTests extends GroovyTestCase {

    void testGeneratedMetadata() {
        def emc = new MetadataGeneratingExpandoMetaClass(MetadataTestController)
        emc.initialize()

        DocumentationContext context = DocumentationContext.instance
        context.active = true
        context.artefactType = "Controller"

        context.document "A test instance method"
        emc.testMethod = { String one, Integer two -> "test"}

        MetadataTestController.metaClass {
            context.document "A test property"
            getSomeProp = {-> "one" }

            anotherMethod { Integer i -> }

            'static' {
                context.document "A test static method"
                listTests { Map args -> }
            }
        }

        def c = new MetadataTestController()
        assertEquals "test", c.testMethod("one",2)

        DocumentedMethod method = context.methods.find { it.name == 'testMethod'}
        assertNotNull "should have added method to documentation context",method

        assertEquals "testMethod", method.name
        assertEquals "A test instance method", method.text
        assertEquals([String, Integer] as Class[], method.arguments)
        assertEquals "Controller", method.artefact
        assertEquals MetadataTestController, method.type

        method = context.methods.find { it.name == 'getSomeProp'}
        assertNotNull "should have added method to documentation context",method

        assertEquals "A test property", method.text
        assertEquals "getSomeProp", method.name
        assertEquals([] as Class[], method.arguments)
        assertEquals "Controller", method.artefact
        assertEquals MetadataTestController, method.type

        method = context.staticMethods.find { it.name == 'listTests'}
        assertNotNull "should have added method to documentation context",method

        assertEquals "A test static method", method.text
        assertEquals "listTests", method.name
        assertEquals([Map] as Class[], method.arguments)
        assertEquals "Controller", method.artefact
        assertEquals MetadataTestController, method.type

        def prop = context.properties.find { it.name == 'someProp' }

        assertNotNull "should have added property to documentation context",prop
        assertEquals "someProp", prop.name

        assertEquals "Controller", prop.artefact
        assertEquals MetadataTestController, prop.type
    }
}

class MetadataTestController {}
