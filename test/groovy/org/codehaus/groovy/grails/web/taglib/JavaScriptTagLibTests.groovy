package org.codehaus.groovy.grails.web.taglib;


import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.util.WebUtils;


public class JavaScriptTagLibTests extends AbstractGrailsTagTests {

    void onInit() {
        def urlMappingsClass = gcl.parseClass('''\
class TestUrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {}
        "/people/details/$var1"(controller: 'person', action: 'show' )
    }
}''')
        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, urlMappingsClass)
    }

    void testPrototypeWithAsyncProperty() {
        def template = '<g:remoteFunction controller="bar" action="foo" options="[asynchronous:false]" />'
        request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", ['prototype'])

        assertOutputEquals("new Ajax.Request('/bar/foo',{asynchronous:false,evalScripts:true});", template)
    }


    void testRemoteFieldWithAdditionalArgs() {
        def template = '<g:remoteField controller="bar" action="storeField" id="2" name="nv" paramName="pnv" params="\'a=b&\'+" />'
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
            assertEquals("new Ajax.Request('/test/action?test=%3Chello%3E',{asynchronous:true,evalScripts:true});", sw.toString())

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
        // Tag: <g:remoteLink controller="person" action="show" update="async" params="[var1:'0']">Show async</g:remoteLink>
        // Expected result: <a href="/people/details/0" onclick="new Ajax.Updater('async','/people/details/0',{asynchronous:true,evalScripts:true,parameters:'var1=0'});return false;">Show async</a>
        StringWriter sw = new StringWriter()   
        PrintWriter pw = new PrintWriter(sw)

        withTag("remoteLink", pw) {tag ->
            GroovyObject tagLibrary = (GroovyObject) tag.getOwner()
            def request = tagLibrary.getProperty("request")
            def includedLibrary = ['prototype']
            request.setAttribute("org.codehaus.grails.INCLUDED_JS_LIBRARIES", includedLibrary)

            def attrs = [controller: 'person', action: 'show', params: [var1: "0"], update: 'async']
            tag.call(attrs) {"Show async"}
            println sw.toString()
            assertEquals("<a href=\"/people/details/0\" onclick=\"new Ajax.Updater('async','/people/details/0',{asynchronous:true,evalScripts:true});return false;\">Show async</a>", sw.toString())

            sw.getBuffer().delete(0, sw.getBuffer().length())
            attrs = [controller: 'person', action: 'show', params: [var1: "unsafe ID 0", test: '<hello>'], update: 'async']
            tag.call(attrs) {"Show async"}
            println sw.toString()
            assertEquals("<a href=\"/people/details/unsafe+ID+0?test=%3Chello%3E\" onclick=\"new Ajax.Updater('async','/people/details/unsafe+ID+0?test=%3Chello%3E',{asynchronous:true,evalScripts:true});return false;\">Show async</a>", sw.toString())
        }
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
