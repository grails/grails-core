package org.codehaus.groovy.grails.reload;

 import org.codehaus.groovy.grails.web.servlet.mvc.*
 import org.codehaus.groovy.grails.commons.*
 import org.apache.commons.logging.*
 import org.codehaus.groovy.grails.web.taglib.*

/**
 * Tests for auto-reloading of tag libraries
 *
 * @author Graeme Rocher 
 **/

class TagLibReloadTests extends AbstractGrailsTagTests {
 

    void testReloadTagLibrary() {
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        Class oldClass = ga.getTagLibClass("TestTagLib").getClazz()
        def result
        println "trying myTag"
		withTag("myTag",pw) { tag ->
            tag.call([foo:"bar"],null)
        }
        assertEquals "foo:bar", sw.toString()
        

        def event = [source:new GroovyClassLoader().parseClass('''
class TestTagLib {
    def myTag = { attrs, body ->
        out << "bar:${attrs.bar}"
    }
}
'''),
                        ctx:appCtx, manager:mockManager]

        def plugin = mockManager.getGrailsPlugin("controllers")

        def eventHandler = plugin.instance.onChange
        eventHandler.delegate = plugin
        eventHandler.call(event)
        
		withTag("myTag",pw) { tag ->
            tag.call([bar:"foo"], null)
        }
        assertEquals "foo:barbar:foo", sw.toString()
    }

	void onInit() {
		def tagLibClass = gcl.parseClass(
'''
class TestTagLib {
    def myTag = { attrs, body ->
        println "attributes $attrs"
        out << "foo:${attrs.foo}"
    }
}
'''
        )

        ga.addArtefact(tagLibClass)
    }

}