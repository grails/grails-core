package org.grails.web.pages

import org.grails.web.taglib.AbstractGrailsTagTests
import org.springframework.web.context.request.RequestContextHolder

class TagLibNamespaceTests extends AbstractGrailsTagTests {

    void testInvokeNamespacedTag() {
        assertOutputEquals "bar",'<t1:foo />'
    }

    void testInvokeNestedNamespacedTag() {
        assertOutputEquals "<hello>barbar</hello>", '<t1:nested name="hello"><t1:foo /></t1:nested>'
    }

    void testDynamicDispatch() {
        def template = '<t1:condition><%println t1."${\'nested\'}"(name:\'hello\')%></t1:condition>'

        assertOutputEquals ''.trim(),template
    }

    void onTearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }

    protected void onSetUp() {
        gcl.parseClass('''
import grails.gsp.*

@TagLib
class TestTagLib {
    static namespace = "t1"

    Closure condition = { attrs, body -> }

    Closure foo = { attrs, body ->
        out << "bar"
        out << body?.call()
    }

    Closure nested = { attrs, body ->
       out << "<${attrs.name}>"
        out << foo()
        out << body()
       out << "</${attrs.name}>"
    }
}
        ''')
    }
}
