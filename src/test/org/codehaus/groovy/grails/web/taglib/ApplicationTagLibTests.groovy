package org.codehaus.groovy.grails.web.taglib;

import grails.util.GrailsUtil;


import org.codehaus.groovy.grails.commons.ApplicationHolder
import javax.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletResponse
import javax.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

class ApplicationTagLibTests extends AbstractGrailsTagTests {

	void testResourceTagWithPluginAttribute() {
		request.contextPath = '/test'
        def template = '${resource(file:"images/foo.jpg", plugin:"controllers")}' 
        assertOutputEquals "/test/plugins/controllers-${GrailsUtil.getGrailsVersion()}/images/foo.jpg", template        
	}

    void testResourceTagWithImplicitPlugin() {
        def template = '${resource(file:"images/foo.jpg")}'

        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, new GroovyPageBinding("/plugin/one"))
        assertOutputEquals "/plugin/one/images/foo.jpg", template

    }

    void testResourceTagWithContextPathAttribute() {
        request.contextPath = '/test'
        def template = '${resource(file:"images/foo.jpg", contextPath:"/foo")}'
        assertOutputEquals "/test/foo/images/foo.jpg", template
        
        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, new GroovyPageBinding("/plugin/one"))
        assertOutputEquals "/test/foo/images/foo.jpg", template
    }
	
	void testResourceTagWithPluginAttributeAndNone() {
		request.contextPath = '/test'
		def template = '${resource(file:"images/foo.jpg", plugin:"none")}' 
		assertOutputEquals "/test/images/foo.jpg", template        
	}
		
	
    void testResourceTag() {
        request.contextPath = '/test'
        def template = '${resource(file:"images/foo.jpg")}' 

        assertOutputEquals '/test/images/foo.jpg', template

        template = '${resource(dir:"images",file:"foo.jpg")}'

        assertOutputEquals '/test/images/foo.jpg', template   
    }

    void testUseJessionIdWithCreateLink() {
        def response = new JsessionIdMockHttpServletResponse()
        ApplicationTagLib.metaClass.getResponse = {-> response}      
        def tagLibBean = appCtx.getBean("org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib")
        ga.config.grails.views.enable.jsessionid=true
        tagLibBean.afterPropertiesSet()
        assertTrue( tagLibBean.@useJsessionId )

        def template = '<g:createLink controller="foo" action="test" />'

        assertOutputEquals "/foo/test;jsessionid=test", template

        ga.config.grails.views.enable.jsessionid=false
        tagLibBean.afterPropertiesSet()
        assertFalse( tagLibBean.@useJsessionId )



        assertOutputEquals "/foo/test", template

    }


  void testObtainCookieValue() {
        def cookie = new Cookie("foo", "bar")
        request.cookies = [cookie] as Cookie[]

        def template = '<g:cookie name="foo" />'

        assertOutputEquals "bar", template

        template = '${cookie(name:"foo")}'

        assertOutputEquals "bar", template

    }

    void testObtainHeaderValue() {
        request.addHeader "FOO", "BAR"
        def template = '<g:header name="FOO" />'

        assertOutputEquals "BAR", template

        template = '${header(name:"FOO")}'

        assertOutputEquals "BAR", template

    }


    void testClonedUrlFromVariable() {
        def template = '''<g:set var="urlMap" value="${[controller: 'test', action: 'justdoit']}"/>${urlMap.controller},${urlMap.action}<g:link url="${urlMap}">test</g:link>${urlMap.controller},${urlMap.action}'''

        assertOutputEquals('test,justdoit<a href="/test/justdoit">test</a>test,justdoit', template)
    }

    void testLinkWithMultipleParameters() {
        def template = '<g:link controller="foo" action="action" params="[test: \'test\', test2: \'test2\']">test</g:link>'

        assertOutputEquals('<a href="/foo/action?test=test&amp;test2=test2">test</a>', template)
    }

    void testLikeWithElementId() {
        def template = '<g:link elementId="myId" controller="foo" action="list">bar</g:link>'
        assertOutputEquals('<a href="/foo/list" id="myId">bar</a>', template)
    }

    void testLinkWithMultipleParametersAndElementId() {
        def template = '<g:link elementId="myid" controller="foo" action="action" params="[test: \'test\', test2: \'test2\']">test</g:link>'

        assertOutputEquals('<a href="/foo/action?test=test&amp;test2=test2" id="myid">test</a>', template)
    }

    void testLinkWithFragment() {
        def template = '<g:link controller="foo" action="bar" fragment="test">link</g:link>'
        profile("link rendering") {
            assertOutputEquals('<a href="/foo/bar#test">link</a>', template)
        }
    }

    void testCreateLinkWithFlowExecutionKeyAndEvent() {
        request.flowExecutionKey = '12345'

        def template = '<g:createLink controller="foo" action="bar" event="boo" />'

        assertOutputEquals('/foo/bar?execution=12345&_eventId=boo', template)

    }

    void testLinkWithFlowExecutionKeyAndEvent() {
        request.flowExecutionKey = '12345'

        def template = '<g:link controller="foo" action="bar" event="boo" >link</g:link>'

        assertOutputEquals('<a href="/foo/bar?execution=12345&amp;_eventId=boo">link</a>', template)

    }


    void testSetTag() {
        def template = '<g:set var="one" value="two" />one: ${one}'

        assertOutputEquals('one: two', template)	
	}
	
	void testSetTagWithBody() {
        def template = '<g:set var="one">two</g:set>one: ${one}'

        assertOutputEquals('one: two', template)		
	}

	void testSetTagWithMap() {
        def template = '<g:set var="e" value="${c.a}"/>${e?.b}'

        assertOutputEquals('', template, [c:[:]])
        assertOutputEquals('foo', template, [c:[a:[b:'foo']]])

    }

    
	void testIteration() {
        def template = '''<g:set var="counter" value="${1}" />
<g:each in="${[10,11,12]}" var="myVal"><g:set var="counter" value="${myVal+counter}" />${counter}</g:each>'''

        printCompiledSource template 
        assertOutputEquals('112234', template, [:], { it.toString().trim() })
    }
	
	void testMetaTag() {
        def template = '<g:meta name="app.version"/>'

        assertOutputEquals('0.9.9.1', template)
	}

    void testCreateLinkToWithDirAndLeadingSlash() {
        def template = '<g:createLinkTo dir="/images" file="foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkToWithDirAndLeadingNoLeadingSlash() {
        def template = '<g:createLinkTo dir="images" file="foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkToWithFileAndLeadingSlash() {
        def template = '<g:createLinkTo dir="/images" file="/foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkTo() {

        def template = '<g:resource dir="test" />'
        assertOutputEquals '/test', template
        
        template = '<g:resource dir="test" file="file" />'
        assertOutputEquals '/test/file', template

        template = '<g:resource dir="" />'
        assertOutputEquals '', template

	}

    void testCreateLinkToFilesInRoot() {
        def template = '<g:resource dir="/" file="test.gsp" />'
        assertOutputEquals '/test.gsp', template
    }

    void testResourceFilesInRootWithContext() {
        def template = '<g:resource dir="/" file="test.gsp" />'
        request.contextPath = "/foo"
        assertOutputEquals '/foo/test.gsp', template
    }

    void testCreateLinkWithZeroId() {
        def template = '<g:createLink action="testAction" controller="testController" id="${id}"  />'
        assertOutputEquals '/testController/testAction/0', template, [id:0]
    }

	void testCreateLinkURLEncoding() {
        def template = '<g:createLink action="testAction" controller="testController" params="[name:\'Marc Palmer\']"  />'
        assertOutputEquals '/testController/testAction?name=Marc+Palmer', template
	}

	void testCreateLinkURLEncodingWithHTMLChars() {
        def template = '<g:createLink action="testAction" controller="testController" params="[email:email]" />'
        assertOutputEquals '/testController/testAction?email=%3Cmarc%40anyware.co.uk%3E', template, [email:'<marc@anyware.co.uk>']
	}

	void testCreateLinkWithBase() {
        def template = '<g:createLink base="http://www128.myhost.com:3495" action="testAction" controller="testController" />'
        assertOutputEquals 'http://www128.myhost.com:3495/testController/testAction', template
	}

    void testCreateLinkWithUseJSessionIdAndContextPapth() {

        request.contextPath = "/foo"
        def taglib = appCtx.getBean(ApplicationTagLib.name)
        
        taglib.useJsessionId = true
        def template = '<g:createLink action="testAction" controller="testController" />'

        assertOutputEquals '/foo/testController/testAction', template

    }

    void testCreateLinkWithContextPath() {
        request.contextPath = "/foo"
        def template = '<g:createLink action="testAction" controller="testController" />'

        assertOutputEquals '/foo/testController/testAction', template
    }

    void testAbsoluteWithContextPath() {
        request.contextPath = "/foo"
        def template = '<g:createLink action="testAction" controller="testController" absolute="true" />'

        assertOutputEquals 'http://localhost:8080/testController/testAction', template

        ConfigurationHolder.config.grails.serverURL="http://www.mysite.com"
        assertOutputEquals 'http://www.mysite.com/testController/testAction', template
        ConfigurationHolder.config.grails.serverURL=null
    }
    
    /**
     * Tests regression of <a href="http://jira.codehaus.org/browse/GRAILS-3368">GRAILS-3368</a>.
     * The context path should not be included in the generated link
     * if "base" is set to an empty string.
     */
    void testCreateLinkWithNoContextPath() {
        def template = '<g:createLink base="" action="testAction" controller="testController" />'
        assertOutputEquals '/testController/testAction', template
    }

	void testCreateLinkWithAbsolute() {
        def template = '<g:createLink absolute="true" action="testAction" controller="testController" />'
        assertOutputEquals 'http://localhost:8080/testController/testAction', template
	}

}
class JsessionIdMockHttpServletResponse extends MockHttpServletResponse {

  public String encodeURL(String url) {
    return super.encodeURL("$url;jsessionid=test");
  }

}