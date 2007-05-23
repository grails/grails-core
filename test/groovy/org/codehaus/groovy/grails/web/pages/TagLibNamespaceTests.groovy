package org.codehaus.groovy.grails.web.pages

import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.errors.*
import org.codehaus.groovy.grails.support.*
import grails.util.*
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*

class TagLibNamespaceTests extends AbstractGrailsControllerTests {


    void testInvokeNamespacedTag() {

        def gpte = new GroovyPagesTemplateEngine(servletContext)

        def t = gpte.createTemplate('<t1:foo />', "foo_test")
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        webRequest.out = pw
        w.writeTo(pw)

        assertEquals "bar", sw.toString()
    }

    /*
      TODO: FIXME! Invoking tags as methods is broken when using namespaces!

     void testInvokeNestedNamespacedTag() {
        def gpte = new GroovyPagesTemplateEngine(servletContext)

        def t = gpte.createTemplate('<t1:nested name="hello"><t1:foo /></t1:nested>', "foo2_test")
        def w = t.make()

        def sw = new StringWriter()
        def pw = new PrintWriter(sw)

        webRequest.out = pw
        w.writeTo(pw)

        assertEquals "<hello>barbar</hello>", sw.toString()
    }  */

    void onTearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }
    void onSetUp() {
        gcl.parseClass('''
class TestTagLib {
    static namespace = "t1"

    def foo = { attrs, body ->
        out << "bar"
        out << body?.call()
    }

    def nested = { attrs, body ->
       out << "<${attrs.name}>"
        out << foo()  // TODO: FIXME! This call is broken!
        out << body()
       out << "</${attrs.name}>"
    }
}
        ''')        
    }
}