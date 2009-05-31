package org.codehaus.groovy.grails.documentation

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import groovy.xml.StreamingMarkupBuilder

/**
 * @author Graeme Rocher
 * @since 1.2
 */
public class MetadataGeneratingExpandoMetaClassTests extends GroovyTestCase {

    void testGeneratedMetadata() {
        def emc = new MetadataGeneratingExpandoMetaClass(TestController, ControllerArtefactHandler.TYPE )
        emc.initialize()


        DocumentationContext context = DocumentationContext.instance
        context.active=true

        emc.testMethod = { String one, Integer two -> "test"}

        def c = new TestController()
        assertEquals "test", c.testMethod("one",2)

        println context.methods
    }

}
class TestController {}