package org.codehaus.groovy.grails.web.taglib

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptProvider
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptTagLib
import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.web.util.WebUtils

class JavascriptTagLibTests extends AbstractGrailsTagTests {

    private static final String EOL = new String([(char)13,(char)10] as char[])

    protected void onSetUp() {
        gcl.parseClass('''
class TestController {}
''')
        JavascriptTagLib.PROVIDER_MAPPINGS["test"] = TestProvider
    }

    protected void onInit() {
        def urlMappingsClass = gcl.parseClass('''\
class TestUrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {}
        "/people/details/$var1"(controller: 'person', action: 'show')
    }
}
''')
        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, urlMappingsClass)
    }

    void testJavascriptLibraryWithNestedTemplates() {

        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource("/foo/_part.gsp", '<g:remoteLink controller="foo" action="list" />')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader resourceLoader
        webRequest.controllerName = "foo"
        def template = '''<g:javascript library="test" /><p><g:remoteLink controller="bar" action="list" /></p><g:render template="part" model="['foo1':foo2]" />'''

        String newLine = EOL
        assertOutputContains('<script src="/js/test.js" type="text/javascript"></script>\r\n<p><a href="/bar/list" onclick="<remote>return false;"></a></p><a href="/foo/list" onclick="<remote>return false;"></a>', template)
    }

    void testJavascriptIncludeWithPluginAttribute() {
        def template = '<g:javascript src="foo.js" plugin="controllers" />'
        def grailsVersion = GrailsUtil.getGrailsVersion()
        assertOutputContains "<script src=\"/plugins/controllers-$grailsVersion/js/foo.js\" type=\"text/javascript\"></script>", template
    }

    void testJavascriptInclude() {
        def template = '<g:javascript src="foo.js" />'
        assertOutputContains '<script src="/js/foo.js" type="text/javascript"></script>', template
    }

    void testJavascriptIncludeWithPlugin() {
        def template = '<g:javascript src="foo.js" />'
        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, new GroovyPageBinding("/plugin/one"))
        assertOutputContains '<script src="/plugin/one/js/foo.js" type="text/javascript"></script>', template
    }

    void testJavascriptIncludeWithContextPathSpecified() {
        def template = '<g:javascript src="foo.js" />'

        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, new GroovyPageBinding("/plugin/one"))
        assertOutputContains '<script src="/plugin/one/js/foo.js" type="text/javascript"></script>', template

        template = '<g:javascript src="foo.js" contextPath="/foo" />'
        assertOutputContains '<script src="/foo/js/foo.js" type="text/javascript"></script>', template
    }

    void testJavascriptIncludeWithPluginNoLeadingSlash() {
        def template = '<g:javascript src="foo.js" />'
        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, new GroovyPageBinding("plugin/one"))
        assertOutputContains '<script src="/plugin/one/js/foo.js" type="text/javascript"></script>' + EOL, template
    }

    /**
     * Tests that the INCLUDED_JS_LIBRARIES attribute is set correctly without resources plugin
     */
    void testLibraryAttributeSet() {
        def template = '<g:javascript library="testing"/>'

        assertOutputContains('<script src="/js/testing.js" type="text/javascript"></script>', template)
        assertEquals(['testing'], request.getAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES"))
    }

    def replaceMetaClass(Object o) {
        def old = o.metaClass

        // Create a new EMC for the class and attach it.
        def emc = new ExpandoMetaClass(o.class, true, true)
        emc.initialize()
        o.metaClass = emc

        return old
    }

    /**
     * Tests that the 'formRemote' tag complains if a 'params' attribute
     * is given.
     */
    void testFormRemoteWithParamsAttribute() {
        def template = '<g:formRemote name="myForm" url="[controller:\'person\', action:\'list\']" params="[var1:\'one\', var2:\'two\']"><g:textField name="foo" /></g:formRemote>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['test'])

        shouldFail(GrailsTagException) {
            applyTemplate(template)
        }
    }

    void testFormRemoteWithStringUrl() {
        def template = '''\
<g:formRemote name="myForm" method="GET" url="/dirt-grails/ruleDetails/saveDynamicParameters" >\
<g:textField name="foo" />\
</g:formRemote>'''
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['test'])

        assertOutputEquals('''\
<form onsubmit="<remote>return false" method="GET"\
 action="/dirt-grails/ruleDetails/saveDynamicParameters" id="myForm"><input type="text" name="foo" value="" id="foo" /></form>''', template)
    }

    /**
     * <p>Tests that the 'formRemote' tag defaults to supplied 'method'
     * and 'action' attributes in fallback mode, i.e. when javascript
     * is unavailable or disabled.</p>
     *
     * <p>Also makes sure no regressions on http://jira.codehaus.org/browse/GRAILS-3330</p>
     */
    void testFormRemoteWithOverrides() {
        def template = '''\
<g:formRemote name="myForm" method="GET" action="/person/showOld?var1=one&var2=two"
              url="[controller:'person', action:'show', params: [var1:'one', var2:'two']]" >\
<g:textField name="foo" />\
</g:formRemote>'''
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['test'])

        assertOutputEquals('''\
<form onsubmit="<remote>return false" method="GET"\
 action="/person/showOld?var1=one&var2=two" id="myForm"><input type="text" name="foo" value="" id="foo" /></form>''', template)
    }

    void testRemoteLinkWithSpaceBeforeGStringVariable() {
        // see http://jira.codehaus.org/browse/GRAILS-4672
        def template = '<g:remoteLink controller="people" action="theAction" params="someParams" update="success" onComplete="doSomething();" title="The Album Is ${variable}" class="hoverLT">${variable}</g:remoteLink>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['test'])
        assertOutputContains 'title="The Album Is Undertow"', template, [variable: 'Undertow']
    }

    void testRemoteLinkWithSpaceBeforeAndAfterGStringVariable() {
        // see http://jira.codehaus.org/browse/GRAILS-4672
        def template = '<g:remoteLink controller="people" action="theAction" params="someParams" update="success" onComplete="doSomething();" title="The Album Is ${variable} By Tool" class="hoverLT">${variable}</g:remoteLink>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['test'])
        assertOutputContains 'title="The Album Is Undertow By Tool"', template, [variable: 'Undertow']
    }

    void testPluginAwareJSSrc() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setupPluginController(tag)
            def attrs = [src: 'lib.js']
            tag.call(attrs) {}
            assertEquals("<script src=\"/myapp/plugins/myplugin/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testPluginAwareJSSrcTrailingSlash() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setupPluginController(tag)
            setRequestContext('/otherapp/')
            def attrs = [src: 'lib.js']
            tag.call(attrs) {}
            assertEquals("<script src=\"/otherapp/plugins/myplugin/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testPluginAwareJSLib() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setupPluginController(tag)
            def attrs = [library: 'lib']
            tag.call(attrs) {}
            assertEquals("<script src=\"/myapp/plugins/myplugin/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testJSSrc() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'lib.js']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script src=\"/myapp/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testJSSrcTrailingSlash() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'lib.js']
            setRequestContext('/otherapp/')
            tag.call(attrs) {}
            assertEquals("<script src=\"/otherapp/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testJSSrcWithNoController() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'lib.js']
            setRequestContext()
            request.setAttribute(GrailsApplicationAttributes.CONTROLLER, null)
            tag.call(attrs) {}
            assertEquals("<script src=\"/myapp/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testJSLib() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [library: 'lib']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script src=\"/myapp/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testJSLibTrailingSlash() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [library: 'lib']
            setRequestContext('/otherapp/')
            tag.call(attrs) {}
            assertEquals("<script src=\"/otherapp/js/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testJSWithBody() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setRequestContext()
            tag.call([:]) {"do.this();"}
            assertEquals("<script type=\"text/javascript\">" + EOL + "do.this();" + EOL + "</script>" + EOL, sw.toString())
        }
    }

    void testJSLibWithBase() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [library: 'lib', base: 'http://testserver/static/']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script src=\"http://testserver/static/lib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    void testJSSrcWithBase() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'mylib.js', base: 'http://testserver/static/']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script src=\"http://testserver/static/mylib.js\" type=\"text/javascript\"></script>" + EOL, sw.toString())
        }
    }

    def setRequestContext() {
        setRequestContext("/myapp")
    }

    def setRequestContext(path) {
        request.setAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE, path)
    }

    def setupPluginController(tag) {
        setRequestContext()
        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, new GroovyPageBinding("plugins/myplugin"))
    }

    void testEscapeJavascript() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        withTag("escapeJavascript", pw) {tag ->
            tag.call(Collections.EMPTY_MAP, "This is some \"text\" to be 'escaped'")
            assertEquals("This is some \\\"text\\\" to be \\'escaped\\'", sw.toString())
        }
    }
}

class TestProvider implements JavascriptProvider {

    def doRemoteFunction(Object taglib, Object attrs, Object out) {
        out << "<remote>"
    }

    def prepareAjaxForm(Object attrs) {}
}
