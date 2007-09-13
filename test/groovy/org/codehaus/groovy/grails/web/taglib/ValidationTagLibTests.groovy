package org.codehaus.groovy.grails.web.taglib;

import org.springframework.validation.Errors
import org.springframework.validation.FieldError;

class ValidationTagLibTests extends AbstractGrailsTagTests {

    void onSetUp() {
        gcl.parseClass('''
class Book {
    Long id
    Long version
    String title
    URL publisherURL
    Date releaseDate
}

        ''')
    }

    void testFieldValueTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.properties = [publisherURL:"a_bad_url"]
        assert b.hasErrors()

        def template = '''<g:fieldValue bean="${book}" field="publisherURL" />'''

        assertOutputEquals("a_bad_url", template, [book:b])

        b.properties = [publisherURL:"http://google.com"]
        assert !b.hasErrors()

        assertOutputEquals("http://google.com", template, [book:b])


    }

    void testHasErrorsTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assert b.hasErrors()

        def template = '''<g:hasErrors bean="${book}">success</g:hasErrors>'''

        assertOutputEquals("success", template, [book:b])

        b.title = "Groovy in Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.releaseDate = new Date()

        assert b.validate()
        assert !b.hasErrors()

        assertOutputEquals("", template, [book:b])        
    }

    void testEachErrorTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assert b.hasErrors()

        def template = '''<g:eachError var="err" bean="${book}">${err.field}|</g:eachError>'''

        assertOutputEquals("title|publisherURL|releaseDate|", template, [book:b])

        template = '''<g:eachError bean="${book}">${it.field}|</g:eachError>'''

        assertOutputEquals("title|publisherURL|releaseDate|", template, [book:b])

    }

    void testRenderErrorsTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assert b.hasErrors()

        def template = '''<g:renderErrors bean="${book}" />'''

        assertOutputEquals("<ul><li>Property [title] of class [class Book] cannot be null</li><li>Property [publisherURL] of class [class Book] cannot be null</li><li>Property [releaseDate] of class [class Book] cannot be null</li></ul>", template, [book:b])

        b.title = "Groovy in Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.releaseDate = new Date()

        assert b.validate()
        assert !b.hasErrors()

        assertOutputEquals("<ul></ul>", template, [book:b])
    }

    void testHasErrorsWithRequestAttributes() {
		StringWriter sw = new StringWriter();

		withTag("hasErrors", sw) { tag ->

            def mockErrors = [hasErrors:{true}]
            
	        request.setAttribute("somethingErrors", mockErrors as Errors);

			// test when no message found it returns code
			def attrs = [:]
			tag.call( attrs, { "error found"} )

			assertEquals "error found", sw.toString()
		}
	}

	void testMessageTagWithError() {
        def error = new FieldError("foo", "bar",1, false, ["my.error.code"] as String[], null, "This is default")
        def template = '<g:message error="${error}" />'

        assertOutputEquals("This is default", template, [error:error])
    }

	void testMessageTagWithBlankButExistingMessageBundleValue() {
	    println "Locale is ${Locale.ENGLISH}"
	    messageSource.addMessage( "test.blank.message", Locale.ENGLISH, "")
	    
        def template = '<g:message code="test.blank.message" />'

        assertOutputEquals("", template, [:])
    }



}
