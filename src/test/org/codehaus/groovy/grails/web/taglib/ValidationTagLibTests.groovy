package org.codehaus.groovy.grails.web.taglib;

import java.util.Locale;

import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.util.StringUtils
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.MessageSourceResolvable

class ValidationTagLibTests extends AbstractGrailsTagTests {

    void onSetUp() {
        gcl.parseClass('''
class Book {
    Long id
    Long version
    String title
    URL publisherURL
    Date releaseDate
    BigDecimal usPrice
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

    void testFieldValueWithClassAndPropertyNameLookupFromBundle() {
        def domain = ga.getDomainClass("Book")

        messageSource.addMessage("Book.label", Locale.US, "Reading Material")
        messageSource.addMessage("Book.title.label", Locale.US, "Subject")
        def b = domain.newInstance()
        b.validate()
        assert b.hasErrors()

        def template = '<g:fieldError bean="${book}" field="title" />'

        webRequest.currentRequest.addPreferredLocale(Locale.US)
        assertOutputEquals 'Property [Subject] of class [Reading Material] cannot be null', template, [book:b]

    }

   void testFieldValueWithShortClassAndPropertyNameLookupFromBundle() {
        def domain = ga.getDomainClass("Book")

        messageSource.addMessage("book.label", Locale.US, "Reading Material")
        messageSource.addMessage("book.title.label", Locale.US, "Subject")
        def b = domain.newInstance()
        b.validate()
        assert b.hasErrors()

        def template = '<g:fieldError bean="${book}" field="title" />'

        webRequest.currentRequest.addPreferredLocale(Locale.US)
        assertOutputEquals 'Property [Subject] of class [Reading Material] cannot be null', template, [book:b]

    }

    void testRenderErrorTag() {
        def domain = ga.getDomainClass("Book")
        def b = domain.newInstance()
        b.validate()
        assert b.hasErrors()

        def template = '''<g:fieldError bean="${book}" field="title" />'''

        assertOutputEquals 'Property [title] of class [class Book] cannot be null', template, [book:b]
        b = domain.newInstance()
        b.title = "The Stand"
        b.validate()
        assertOutputEquals '', template, [book:b]
        assertOutputEquals '', template, [book:domain.newInstance()]

    }

    void testFieldValueHtmlEscaping() {
        def b = ga.getDomainClass("Book").newInstance()
        b.properties = [title:"<script>alert('escape me')</script>"]

        def template = '''<g:fieldValue bean="${book}" field="title" />'''

        assertOutputEquals("&lt;script&gt;alert('escape me')&lt;/script&gt;", template, [book:b])

        request.setAttribute("org.codehaus.groovy.grails.GSP_CODEC", 'html')

        assertOutputEquals("<script>alert('escape me')</script>", template, [book:b])
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

    void testFieldValueTagWithDecimalNumber() {
        def b = ga.getDomainClass("Book").newInstance()
        b.properties = [publisherURL:"http://google.com", usPrice: 1045.99]

        // First test with English.
        webRequest.currentRequest.addPreferredLocale(Locale.US)

        def template = '<g:fieldValue bean="${book}" field="usPrice" />'
        assertOutputEquals("1,045.99", template, [book:b])

        webRequest.currentRequest.removeAttribute(GrailsApplicationAttributes.PROPERTY_REGISTRY) 
        // And then with German.
        webRequest.currentRequest.addPreferredLocale(Locale.GERMANY)
        assertOutputEquals("1.045,99", template, [book:b])

        // No decimal part.
        b.properties = [publisherURL:"http://google.com", usPrice: 1045G]
        assertOutputEquals("1.045", template, [book:b])

        // Several decimal places.
        b.properties = [publisherURL:"http://google.com", usPrice: 1045.45567]
        assertOutputEquals("1.045,456", template, [book:b])
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
        b.usPrice = 10.99

        assert b.validate()
        assert !b.hasErrors()

        assertOutputEquals("", template, [book:b])

        b = ga.getDomainClass("Book").newInstance();
        b.title = "Groovy In Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.releaseDate = new Date()
        b.usPrice = 10.99
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
        println result
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
        b.usPrice = 10.99

        assert b.validate()
        assert !b.hasErrors()

        assertOutputEquals("<ul></ul>", template, [book:b])
    }

    void testRenderErrorsAsXMLTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assert b.hasErrors()

        def template = '''<g:renderErrors bean="${book}" as="xml" />'''

        def result = applyTemplate(template,[book:b])

        println result
        def xml = new XmlSlurper().parseText(result)


        assertEquals 4, xml.error.size()
        assertEquals "Book", xml.error[0].@object.text()
        assertEquals "title", xml.error[0].@field.text()
        assertEquals "Property [title] of class [class Book] cannot be null", xml.error[0].@message.text()
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

    void testMessageTagWithLocaleAttribute() {
        messageSource.addMessage( "welcome.message", Locale.ENGLISH, "Hello!")
        messageSource.addMessage( "welcome.message", Locale.ITALIAN, "Ciao!")

        def template = '<g:message code="welcome.message" />'

        assertOutputEquals("Hello!", template, [:])
        assertOutputEquals("Hello!", template, [locale:Locale.ITALIAN])

    }

	void testMessageTagWithBlankButExistingMessageBundleValue() {
	    
	    messageSource.addMessage( "test.blank.message", Locale.ENGLISH, "")
	    
        def template = '<g:message code="test.blank.message" />'

        assertOutputEquals("", template, [:])
    }

    void testMessageTagWithMessage() {
        def resolvable = [
                getArguments: {-> [] as Object[] },
                getCodes: {-> ["my.message.code"] as String[] },
                getDefaultMessage: {-> "The Default Message" }
        ] as MessageSourceResolvable

        def template = '<g:message message="${message}" />'

        assertOutputEquals("The Default Message", template, [message: resolvable])
    }

}
