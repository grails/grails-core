package org.codehaus.groovy.grails.web.taglib;


import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.util.WebUtils
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptTagLib
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptProvider
import org.codehaus.groovy.grails.support.MockStringResourceLoader;


public class JavascriptTagLibTests extends AbstractGrailsTagTests {
    private static final String EOL = System.getProperty("line.separator")

    public void onSetUp() {
        gcl.parseClass('''
class TestController {}
''')
    }

    void onInit() {
        def urlMappingsClass = gcl.parseClass('''\
class TestUrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {}
        "/people/details/$var1"(controller: 'person', action: 'show' )
    }
}
''')
        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, urlMappingsClass)
    }

    void testJavascriptLibraryWithNestedTemplates() {
        JavascriptTagLib.PROVIDER_MAPPINGS["test"] = TestProvider

        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource("/foo/_part.gsp", '<g:remoteLink controller="foo" action="list" />')
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader
        webRequest.controllerName = "foo"
        def template = '''<g:javascript library="test" /><p><g:remoteLink controller="bar" action="list" /></p><g:render template="part" model="['foo1':foo2]" />'''


        assertOutputEquals('''<script type="text/javascript" src="/js/test.js"></script>
<p><a href="/bar/list" onclick="<remote>return false;" action="list" controller="bar"></a></p><a href="/foo/list" onclick="<remote>return false;" action="list" controller="foo"></a>''', template)

    }
    void testJavascriptInclude() {
        def template = '<g:javascript src="foo.js" />'

        assertOutputEquals '<script type="text/javascript" src="/js/foo.js"></script>' + EOL, template
    }

    void testJavascriptIncludeWithPlugin() {
        def controllerClass = ga.getControllerClass("TestController").clazz
        def template = '<g:javascript src="foo.js" />'

        controllerClass.metaClass.getPluginContextPath = {->"/plugin/one"}
        request.setAttribute(JavascriptTagLib.CONTROLLER, controllerClass.newInstance())
        assertOutputEquals '<script type="text/javascript" src="/plugin/one/js/foo.js"></script>' + EOL, template
    }

    void testJavascriptIncludeWithPluginNoLeadingSlash() {
        def controllerClass = ga.getControllerClass("TestController").clazz
        def template = '<g:javascript src="foo.js" />'

        controllerClass.metaClass.getPluginContextPath = {->"plugin/one"}
        request.setAttribute(JavascriptTagLib.CONTROLLER, controllerClass.newInstance())
        assertOutputEquals '<script type="text/javascript" src="/plugin/one/js/foo.js"></script>' + EOL, template
    }

    
    void testRemoteFieldWithExtraParams() {
        def template = '<g:remoteField controller="test" action="hello" id="1" params="[var1: \'one\', var2: \'two\']" update="success" name="myname" value="myvalue"/>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals '<input type="text" name="myname" value="myvalue" onkeyup="new Ajax.Updater(\'success\',\'/test/hello/1\',{asynchronous:true,evalScripts:true,parameters:\'value=\'+this.value+\'&var1=one&var2=two\'});" />', template
    }

    void testPrototypeSubmitToRemoteWithExtraParams() {
        def template = '<g:submitToRemote name="myButton" url="[controller:\'person\', action:\'show\', params:[var1:\'one\', var2:\'two\']]" ></g:submitToRemote>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals('<input onclick="new Ajax.Request(\'/people/details/one\',{asynchronous:true,evalScripts:true,parameters:Form.serialize(this.form)+\'&var2=two\'});return false" type="button" name="myButton"></input>', template)
    }

    void testPrototypeFormRemoteWithExtraParams() {
        def template = '<g:formRemote name="myForm" url="[controller:\'person\', action:\'show\', params:[var1:\'one\', var2:\'two\']]" ><g:textField name="foo" /></g:formRemote>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals('<form onsubmit="new Ajax.Request(\'/people/details/one\',{asynchronous:true,evalScripts:true,parameters:Form.serialize(this)+\'&var2=two\'});return false" method="POST" action="/people/details/one?var2=two" name="myForm" id="myForm"><input type="text" name="foo" id="foo" value="" /></form>', template)
    }

    /**
     * Tests that the 'formRemote' tag complains if a 'params' attribute
     * is given.
     */
    void testPrototypeFormRemoteWithParamsAttribute() {
        def template = '<g:formRemote name="myForm" url="[controller:\'person\', action:\'list\']" params="[var1:\'one\', var2:\'two\']"><g:textField name="foo" /></g:formRemote>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        shouldFail(GrailsTagException) {
            applyTemplate(template)
        }
    }

    /**
     * Tests that the 'formRemote' tag defaults to supplied 'method'
     * and 'action' attributes in fallback mode, i.e. when javascript
     * is unavailable or disabled.
     */
    void testPrototypeFormRemoteWithOverrides() {
        def template = '''\
<g:formRemote name="myForm" method="GET" action="/person/showOld?var1=one&var2=two"
              url="[controller:'person', action:'show', params: [var1:'one', var2:'two']]" >\
<g:textField name="foo" />\
</g:formRemote>'''
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals('''\
<form onsubmit="new Ajax.Request('/people/details/one',{asynchronous:true,evalScripts:true,parameters:Form.serialize(this)+'&var2=two'});return false" method="GET"\
 action="/person/showOld?var1=one&var2=two" name="myForm" id="myForm"><input type="text" name="foo" id="foo" value="" /></form>''', template)
    }

    void testPrototypeFormRemoteWithExactParams() {
        def template = '<g:formRemote name="myForm" url="[controller:\'person\', action:\'show\', params:[var1:\'one\']]" ><g:textField name="foo" /></g:formRemote>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals('<form onsubmit="new Ajax.Request(\'/people/details/one\',{asynchronous:true,evalScripts:true,parameters:Form.serialize(this)});return false" method="POST" action="/people/details/one" name="myForm" id="myForm"><input type="text" name="foo" id="foo" value="" /></form>', template)
    }


    void testPrototypeWithAsyncProperty() {
        def template = '<g:remoteFunction controller="bar" action="foo" options="[asynchronous:false]" />'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals("new Ajax.Request('/bar/foo',{asynchronous:false,evalScripts:true});", template)
    }

    void testPrototypeWithExtraParams() {
        def template = '<g:remoteFunction controller="person" action="show" params="[var1:\'one\', var2:\'two\']" />'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals("new Ajax.Request('/people/details/one',{asynchronous:true,evalScripts:true,parameters:'var2=two'});", template)
    }


    void testPrototypeLinkWithExtraParams() {
        def template = '<g:remoteLink controller="person" action="show" params="[var1:\'one\', var2:\'two\']" >hello</g:remoteLink>'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals('<a href="/people/details/one?var2=two" onclick="new Ajax.Request(\'/people/details/one\',{asynchronous:true,evalScripts:true,parameters:\'var2=two\'});return false;">hello</a>', template)

    }


    void testRemoteFieldWithAdditionalArgs() {
        def template = '<g:remoteField controller="bar" action="storeField" id="2" name="nv" paramName="pnv" params="\'a=b&\'" />'
        assertOutputEquals '<input type="text" name="nv" value="" onkeyup="new Ajax.Request(\'/bar/storeField/2\',{asynchronous:true,evalScripts:true,parameters:\'a=b&\'+\'pnv=\'+this.value});" />', template
    }

    public void testPrototypeRemoteFunction() throws Exception {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        withTag("remoteFunction", pw) {tag ->
            GroovyObject tagLibrary = (GroovyObject) tag.getOwner()
            def request = tagLibrary.getProperty("request")
            def includedLibrary = ['prototype']
            request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary)

            def attrs = [action: 'action', controller: 'test']
            tag.call(attrs)
            assertEquals("new Ajax.Request('/test/action',{asynchronous:true,evalScripts:true});", sw.toString())

            sw.getBuffer().delete(0, sw.getBuffer().length())
            attrs = [action: 'action', controller: 'test', params: [test:'<hello>']]
            tag.call(attrs)
            assertEquals("new Ajax.Request('/test/action',{asynchronous:true,evalScripts:true,parameters:'test=%3Chello%3E'});", sw.toString())

            sw.getBuffer().delete(0, sw.getBuffer().length())
            attrs = [action: 'action', controller: 'test', update: [success: 'updateMe'], options: [insertion: 'Insertion.Bottom']]
            tag.call(attrs)
            assertEquals("new Ajax.Updater({success:'updateMe'},'/test/action',{asynchronous:true,evalScripts:true,insertion:Insertion.Bottom});", sw.toString())
        }
    }




    public void testRemoteField() {
        // <g:remoteField action="changeTitle" update="titleDiv"  name="title" value="${book?.title}"/>
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        withTag("remoteField", pw) {tag ->
            GroovyObject tagLibrary = (GroovyObject) tag.getOwner()
            def request = tagLibrary.getProperty("request")
            def includedLibrary = ['prototype']
            request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary)

            def attrs = [controller: 'test', action: 'changeTitle', update: 'titleDiv', name: 'title', value: 'testValue']
            tag.call(attrs) {"body"}
            assertEquals("<input type=\"text\" name=\"title\" value=\"testValue\" onkeyup=\"new Ajax.Updater('titleDiv','/test/changeTitle',{asynchronous:true,evalScripts:true,parameters:'value='+this.value});\" />", sw.toString())
        }

    }

    public void testRemoteLink() {
        // test for GRAILS-1304
        def template = '<g:remoteLink controller="person" action="show" update="async" params="[var1:\'0\']">Show async</g:remoteLink>'
        assertOutputEquals '<a href="/people/details/0" onclick="new Ajax.Updater(\'async\',\'/people/details/0\',{asynchronous:true,evalScripts:true});return false;">Show async</a>', template

    }

    public void testPluginAwareJSSrc() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setupPluginController(tag)
            def attrs = [src: 'lib.js']
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/myapp/plugins/myplugin/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testPluginAwareJSSrcTrailingSlash() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setupPluginController(tag)
            setRequestContext('/otherapp/')
            def attrs = [src: 'lib.js']
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/otherapp/plugins/myplugin/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testPluginAwareJSLib() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setupPluginController(tag)
            def attrs = [library: 'lib']
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/myapp/plugins/myplugin/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testJSSrc() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'lib.js']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/myapp/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testJSSrcTrailingSlash() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'lib.js']
            setRequestContext('/otherapp/')
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/otherapp/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testJSSrcWithNoController() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'lib.js']
            setRequestContext()
            request.setAttribute(GrailsApplicationAttributes.CONTROLLER, null);
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/myapp/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testJSLib() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [library: 'lib']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/myapp/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testJSLibTrailingSlash() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [library: 'lib']
            setRequestContext('/otherapp/')
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"/otherapp/js/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    public void testJSWithBody() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            setRequestContext()
            tag.call([:]) {"do.this();"}
            assertEquals("<script type=\"text/javascript\">" + System.getProperty("line.separator") + "do.this();" + System.getProperty("line.separator") + "</script>" + System.getProperty("line.separator"), sw.toString())
        }
    }


    public void testJSLibWithBase() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [library: 'lib', base: 'http://testserver/static/']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"http://testserver/static/lib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }


    public void testJSSrcWithBase() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        withTag("javascript", pw) {tag ->
            def attrs = [src: 'mylib.js', base: 'http://testserver/static/']
            setRequestContext()
            tag.call(attrs) {}
            assertEquals("<script type=\"text/javascript\" src=\"http://testserver/static/mylib.js\"></script>" + System.getProperty("line.separator"), sw.toString())
        }
    }

    def setRequestContext() {
        setRequestContext("/myapp")
    }

    def setRequestContext(path) {
        request.setAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE, path)
    }

    def setupPluginController(tag) {
        GroovyObject tagLibrary = (GroovyObject) tag.getOwner()
        def request = tagLibrary.getProperty("request")
        setRequestContext()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, new Expando(pluginContextPath: "plugins/myplugin"));
    }

    public void testEscapeJavascript() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        withTag("escapeJavascript", pw) {tag ->

            tag.call("This is some \"text\" to be 'escaped'", Collections.EMPTY_MAP);
            assertEquals("This is some \\\"text\\\" to be \\'escaped\\'", sw.toString());
        }
    }
}
class TestProvider implements JavascriptProvider {

    public doRemoteFunction(Object taglib, Object attrs, Object out) {
        out << "<remote>"
    }

    public prepareAjaxForm(Object attrs) {        
    }

}