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
import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

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
    void onSetUp() {
        gcl.parseClass('''
class TestTagLib {
    static namespace = "t1"

    def condition = { attrs, body ->
        
    }
    def foo = { attrs, body ->
        out << "bar"
        out << body?.call()
    }

    def nested = { attrs, body ->
       out << "<${attrs.name}>"
        out << foo()  
        out << body()
       out << "</${attrs.name}>"
    }
}
        ''')        
    }
}