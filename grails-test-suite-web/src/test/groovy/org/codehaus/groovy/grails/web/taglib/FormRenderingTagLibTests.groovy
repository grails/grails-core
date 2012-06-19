package org.codehaus.groovy.grails.web.taglib

class FormRenderingTagLibTests extends AbstractGrailsTagTests {

    void testTimeZoneSelect() {
        def template = '<g:timeZoneSelect name="foo"/>'

        def engine = appCtx.groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make()

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        def output = sw.toString()

        assertTrue output.startsWith('<select name="foo" id="foo" >')
        assertTrue output.endsWith('</select>')
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
