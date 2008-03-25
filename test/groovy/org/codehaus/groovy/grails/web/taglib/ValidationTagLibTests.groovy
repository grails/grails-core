package org.codehaus.groovy.grails.web.taglib;

import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.util.StringUtils;

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

        gcl.parseClass('''
class Article {
    Long id
    Long version
    String title
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

        b = ga.getDomainClass("Book").newInstance()
        b.title = "Groovy In Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.validate()
        assert b.hasErrors()
        assertOutputEquals("success", '''<g:hasErrors bean="${book}" field="releaseDate">success</g:hasErrors>''', [book:b])
        assertOutputEquals("success", '''<g:hasErrors model="[book:book]" field="releaseDate">success</g:hasErrors>''', [book:b])
        assertOutputEquals("", '''<g:hasErrors bean="${book}" field="title">success</g:hasErrors>''', [book:b])
        assertOutputEquals("", '''<g:hasErrors model="[book:book]" field="title">success</g:hasErrors>''', [book:b])

        b.clearErrors()
        b.title = "Groovy in Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.releaseDate = new Date()

        assert b.validate()
        assert !b.hasErrors()

        assertOutputEquals("", template, [book:b])

        b = ga.getDomainClass("Book").newInstance();
        b.title = "Groovy In Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.releaseDate = new Date()
        b.validate()
        def a = ga.getDomainClass("Article").newInstance();
        a.validate()
        assertOutputEquals("success", '''<g:hasErrors bean="${article}">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("success", '''<g:hasErrors bean="${article}" field="title">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("", '''<g:hasErrors bean="${book}">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("", '''<g:hasErrors bean="${book}" field="title">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("success", '''<g:hasErrors model="[book:book,article:article]" bean="${article}">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("success", '''<g:hasErrors model="[book:book,article:article]" bean="${article}" field="title">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("", '''<g:hasErrors model="[book:book,article:article]" bean="${book}">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("", '''<g:hasErrors model="[book:book,article:article]" bean="${book}" field="title">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("success", '''<g:hasErrors model="[book:book,article:article]">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("success", '''<g:hasErrors model="[article:article]">success</g:hasErrors>''', [book:b,article:a])
        assertOutputEquals("", '''<g:hasErrors model="[book:book]">success</g:hasErrors>''', [book:b,article:a])
    }

    void testEachErrorTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assert b.hasErrors()

        def template = '''<g:eachError var="err" bean="${book}">${err.field}|</g:eachError>'''

        def result = applyTemplate(template, [book:b])
        assertTrue result.contains("title|")
        assertTrue result.contains("releaseDate|")
        assertTrue result.contains("publisherURL|")

        template = '''<g:eachError bean="${book}">${it.field}|</g:eachError>'''
        result = applyTemplate(template, [book:b])
        assertTrue result.contains("title|")
        assertTrue result.contains("releaseDate|")
        assertTrue result.contains("publisherURL|")
    }

    void testRenderErrorsTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assert b.hasErrors()

        def template = '''<g:renderErrors bean="${book}" />'''

        def result = applyTemplate(template,[book:b])
        assertTrue result.contains("<li>Property [title] of class [class Book] cannot be null</li>")
        assertTrue result.contains("<li>Property [publisherURL] of class [class Book] cannot be null</li>")
        assertTrue result.contains("<li>Property [releaseDate] of class [class Book] cannot be null</li>")
        assertTrue result.startsWith("<ul>")
        assertTrue result.endsWith("</ul>")

        b.clearErrors()
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
