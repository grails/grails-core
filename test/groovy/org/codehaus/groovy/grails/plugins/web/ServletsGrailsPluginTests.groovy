package org.codehaus.groovy.grails.plugins.web

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.commons.spring.*
import org.springframework.mock.web.*
import javax.servlet.http.HttpServletRequest

class ServletsGrailsPluginTests extends AbstractGrailsPluginTests {


	void onSetUp() {

		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
	}

	void testHttpMethodMethods() {
        def request = new MockHttpServletRequest()

        request.method = "POST"

        assert request.isPost()
        assert !request.isGet()
        assert request.post
        assert !request.get

        request.method = 'GET'

        assert request.get
        assert !request.post
    }

    void testAccessRequestAttributes() {
        def request = new MockHttpServletRequest()

        request["foo"] = "bar"        
        assert "bar" == request["foo"]
    }

    void testEachMethod() {
        def request = new MockHttpServletRequest()

        request["foo"] = "bar"
        request["bar"] = "foo"

        def list = []
        request.each { k,v ->
            list << v
        }
        assert list.contains("bar")
        assert list.contains("foo")

    }

    void testFindAllMethod() {
        def request = new MockHttpServletRequest()

        request["foo"] = "bar"
        request["bar"] = "foo"
        request["foobar"] = "yes!"

        def results = request.findAll { it.key.startsWith("foo") }

        assertEquals( [foo:"bar",foobar:"yes!"],results )
    }

    void testFindMethod() {
        def request = new MockHttpServletRequest()

        request["foo"] = "bar"
        request["bar"] = "foo"
        request["foobar"] = "yes!"

        def results = request.find { it.key.startsWith("bar") }

        assertEquals( [bar:"foo"],results )
    }
}