package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.grails.commons.*

class LinkRenderingTagLib2Tests extends AbstractGrailsTagTests {

    void onInit() {
        def mappingClass = gcl.parseClass('''
class TestUrlMappings {
	static mappings = {
	    "/$id?"{
			controller = "content"
			action = "view"
		}

		"/$dir/$id"{
			controller = "content"
			action = "view"
		}
	}
}
        ''')

        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, mappingClass)
    }

    void testLinkWithOnlyId() {
        def template = '<g:link id="competition">Enter</g:link>'

        assertOutputEquals('<a href="/competition">Enter</a>', template)
    }

    void testLinkWithOnlyIdAndAction() {
        def template = '<g:link id="competition" controller="content" action="view">Enter</g:link>'

        assertOutputEquals('<a href="/competition">Enter</a>', template)
    }

    void assertOutputEquals(expected, template, params = [:]) {
        def engine = appCtx.groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make(params)

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        assertEquals expected, sw.toString()
    }

}
