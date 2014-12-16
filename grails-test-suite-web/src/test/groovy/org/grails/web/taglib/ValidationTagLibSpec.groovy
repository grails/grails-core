package org.grails.web.taglib

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.grails.plugins.web.taglib.ValidationTagLib
import org.springframework.context.i18n.LocaleContextHolder
import spock.lang.Specification

/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author graemerocher
 */
@TestMixin(GroovyPageUnitTestMixin)
@TestFor(ValidationTagLib)
@Mock(ValidationTagLibBook)
class ValidationTagLibSpec extends Specification {
    void testDefaultErrorMessage() {

        when:
        def b = new ValidationTagLibBook()

        then:
        !b.validate()
        b.hasErrors()

        when:
        def template = '<g:fieldError bean="${book}" field="title" />'
        webRequest.currentRequest.addPreferredLocale(Locale.US)
        def result = applyTemplate( template, [book:b] )

        then:
        result == 'Property [title] of class [class org.grails.web.taglib.ValidationTagLibBook] cannot be null'

        when:"the object is made valid"
        b.title = "The Stand"
        b.validate()
        then:"No message is output"

    }

    void testFieldValueWithClassAndPropertyNameLookupFromBundle() {
        given:
            LocaleContextHolder.setLocale(Locale.US)
            messageSource.addMessage("org.grails.web.taglib.ValidationTagLibBook.label", Locale.US, "Reading Material")
            messageSource.addMessage("org.grails.web.taglib.ValidationTagLibBook.title.label", Locale.US, "Subject")

        when:
            def b = new ValidationTagLibBook()

        then:
            !b.validate()
            b.hasErrors()

        when:
            def template = '<g:fieldError bean="${book}" field="title" />'
            webRequest.currentRequest.addPreferredLocale(Locale.US)
            def result = applyTemplate( template, [book:b] )

        then:
            result == 'Property [Subject] of class [Reading Material] cannot be null'
    }

    void testFieldValueWithShortClassAndPropertyNameLookupFromBundle() {
        given:
            LocaleContextHolder.setLocale(Locale.US)
            messageSource.addMessage("validationTagLibBook.label", Locale.US, "Reading Material")
            messageSource.addMessage("validationTagLibBook.title.label", Locale.US, "Subject")

        when:
            def b = new ValidationTagLibBook()

        then:
            !b.validate()
            b.hasErrors()

        when:
            def template = '<g:fieldError bean="${book}" field="title" />'
            webRequest.currentRequest.addPreferredLocale(Locale.US)
            def result = applyTemplate( template, [book:b] )

        then:
            result == 'Property [Subject] of class [Reading Material] cannot be null'
    }



    void testFieldValueHTMLEscape() {
        given:
            def b = new ValidationTagLibBook(title:"<script>alert('escape me')</script>")
            def template = '''<g:fieldValue bean="${book}" field="title" />'''
            def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
            def expected = "&lt;script&gt;alert(&#39;escape me&#39;)&lt;/script&gt;"
        expect:"content with tags is output"
            applyTemplate( template, [book:b] ) == expected
            applyTemplate( htmlCodecDirective + template, [book:b] ) == expected

    }

    // TODO: port remaing tests to new format
//    void testFieldValueHtmlEscapingWithFunctionSyntaxCall() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [title:"<script>alert('escape me')</script>"]
//
//        def template = '''${fieldValue(bean:book, field:"title")}'''
//        def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
//        def expected = "&lt;script&gt;alert(&#39;escape me&#39;)&lt;/script&gt;"
//        assertOutputEquals(expected, template, [book:b])
//        assertOutputEquals(expected, htmlCodecDirective + template, [book:b])
//    }
//
//    void testFieldValueHtmlEscapingDifferentEncodings() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [title:"<script>alert('escape me')</script>"]
//
//        def template = '''${fieldValue(bean:book, field:"title")}'''
//        def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
//        def expected = "&lt;script&gt;alert(&#39;escape me&#39;)&lt;/script&gt;"
//
//        def resourceLoader = new MockStringResourceLoader()
//        resourceLoader.registerMockResource('/_sometemplate.gsp', htmlCodecDirective + template)
//        resourceLoader.registerMockResource('/_sometemplate_nocodec.gsp', template)
//        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)
//
//        assertOutputEquals(expected, '<g:render template="/sometemplate" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, template + '<g:render template="/sometemplate" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + template + '<g:render template="/sometemplate" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, '<g:render template="/sometemplate" model="[book:book]" />' + template, [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + '<g:render template="/sometemplate" model="[book:book]" />' + template, [book:b])
//
//        assertOutputEquals(expected, '<g:render template="/sometemplate_nocodec" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, template + '<g:render template="/sometemplate_nocodec" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + template + '<g:render template="/sometemplate_nocodec" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, '<g:render template="/sometemplate_nocodec" model="[book:book]" />' + template, [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + '<g:render template="/sometemplate_nocodec" model="[book:book]" />' + template, [book:b])
//    }
//
//    void testFieldValueTag() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [publisherURL:"a_bad_url"]
//        assertTrue b.hasErrors()
//
//        def template = '''<g:fieldValue bean="${book}" field="publisherURL" />'''
//
//        assertOutputEquals("a_bad_url", template, [book:b])
//
//        b.properties = [publisherURL:"http://google.com"]
//        assertFalse b.hasErrors()
//
//        assertOutputEquals("http://google.com", template, [book:b])
//    }
//
//    void testFieldValueTagWithDecimalNumber() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [publisherURL:"http://google.com", usPrice: 1045.99]
//
//        // First test with English.
//        webRequest.currentRequest.addPreferredLocale(Locale.US)
//
//        def template = '<g:fieldValue bean="${book}" field="usPrice" />'
//        assertOutputEquals("1,045.99", template, [book:b])
//
//        webRequest.currentRequest.removeAttribute(GrailsApplicationAttributes.PROPERTY_REGISTRY)
//        // And then with German.
//        webRequest.currentRequest.addPreferredLocale(Locale.GERMANY)
//        assertOutputEquals("1.045,99", template, [book:b])
//
//        // No decimal part.
//        b.properties = [publisherURL:"http://google.com", usPrice: 1045G]
//        assertOutputEquals("1.045", template, [book:b])
//
//        // Several decimal places.
//        b.properties = [publisherURL:"http://google.com", usPrice: 1045.45567]
//        assertOutputEquals("1.045,456", template, [book:b])
//    }
//
//    void testFieldValueTagWithFrenchLocaleInTextField() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [publisherURL:"http://google.com", usPrice: 1045.99]
//
//        String template = '''<g:textField name="usPrice" value="${fieldValue(bean: book, field: 'usPrice')}" />'''
//
//        // First test with English.
//        webRequest.currentRequest.addPreferredLocale(Locale.US)
//
//        assertOutputEquals '<input type="text" name="usPrice" value="1,045.99" id="usPrice" />',
//                template, [book:b]
//
//        webRequest.currentRequest.removeAttribute(GrailsApplicationAttributes.PROPERTY_REGISTRY)
//        // And then with French.
//        webRequest.currentRequest.addPreferredLocale(Locale.FRENCH)
//
//        assertOutputEquals '<input type="text" name="usPrice" value="1&nbsp;045,99" id="usPrice" />',
//                template, [book:b]
//    }
//
//    void testHasErrorsTag() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.validate()
//
//        assertTrue b.hasErrors()
//
//        def template = '''<g:hasErrors bean="${book}">success</g:hasErrors>'''
//
//        assertOutputEquals("success", template, [book:b])
//
//        b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.title = "Groovy In Action"
//        b.publisherURL = new URL("http://canoo.com/gia")
//        b.validate()
//        assertTrue b.hasErrors()
//        assertOutputEquals("success", '''<g:hasErrors bean="${book}" field="releaseDate">success</g:hasErrors>''', [book:b])
//        assertOutputEquals("success", '''<g:hasErrors model="[book:book]" field="releaseDate">success</g:hasErrors>''', [book:b])
//        assertOutputEquals("success", '''${hasErrors(bean: book, field:"releaseDate") { "success" }}''', [book:b])
//        assertOutputEquals("success", '''${hasErrors(model: [book: book], field:"releaseDate") { "success" }}''', [book:b])
//        assertOutputEquals("success", '''${g.hasErrors(bean: book, field:"releaseDate") { "success" }}''', [book:b])
//        assertOutputEquals("success", '''${g.hasErrors(model: [book: book], field:"releaseDate") { "success" }}''', [book:b])
//        assertOutputEquals("", '''<g:hasErrors bean="${book}" field="title">success</g:hasErrors>''', [book:b])
//        assertOutputEquals("", '''<g:hasErrors model="[book:book]" field="title">success</g:hasErrors>''', [book:b])
//
//        b.clearErrors()
//        b.title = "Groovy in Action"
//        b.publisherURL = new URL("http://canoo.com/gia")
//        b.releaseDate = new Date()
//        b.usPrice = 10.99
//
//        assertTrue b.validate()
//        assertFalse b.hasErrors()
//
//        assertOutputEquals("", template, [book:b])
//
//        b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.title = "Groovy In Action"
//        b.publisherURL = new URL("http://canoo.com/gia")
//        b.releaseDate = new Date()
//        b.usPrice = 10.99
//        b.validate()
//        def a = ga.getDomainClass("ValidationTagLibArticle").newInstance()
//        a.validate()
//        assertOutputEquals("success", '''<g:hasErrors bean="${article}">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("success", '''<g:hasErrors bean="${article}" field="title">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("", '''<g:hasErrors bean="${book}">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("", '''<g:hasErrors bean="${book}" field="title">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("success", '''<g:hasErrors model="[book:book,article:article]" bean="${article}">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("success", '''<g:hasErrors model="[book:book,article:article]" bean="${article}" field="title">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("", '''<g:hasErrors model="[book:book,article:article]" bean="${book}">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("", '''<g:hasErrors model="[book:book,article:article]" bean="${book}" field="title">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("success", '''<g:hasErrors model="[book:book,article:article]">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("success", '''<g:hasErrors model="[article:article]">success</g:hasErrors>''', [book:b,article:a])
//        assertOutputEquals("", '''<g:hasErrors model="[book:book]">success</g:hasErrors>''', [book:b,article:a])
//    }
//
//    void testEachErrorTag() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.validate()
//
//        assertTrue b.hasErrors()
//
//        def template = '''<g:eachError var="err" bean="${book}">${err.field}|</g:eachError>'''
//
//        def result = applyTemplate(template, [book:b])
//        assertTrue result.contains("title|")
//        assertTrue result.contains("releaseDate|")
//        assertTrue result.contains("publisherURL|")
//
//        template = '''<g:eachError bean="${book}">${it.field}|</g:eachError>'''
//        result = applyTemplate(template, [book:b])
//        assertTrue result.contains("title|")
//        assertTrue result.contains("releaseDate|")
//        assertTrue result.contains("publisherURL|")
//    }
//
//    void testEachErrorTagInController() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.validate()
//
//        assertTrue b.hasErrors()
//
//        def g = appCtx.gspTagLibraryLookup.lookupNamespaceDispatcher("g")
//        def errorFields = []
//        g.eachError(bean: b) {
//            errorFields << it.field
//        }
//        assertTrue errorFields.contains("title")
//        assertTrue errorFields.contains("releaseDate")
//        assertTrue errorFields.contains("publisherURL")
//    }
//
//    void testRenderErrorsTag() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.validate()
//
//        assertTrue b.hasErrors()
//
//        def template = '''<g:renderErrors bean="${book}" />'''
//
//        def result = applyTemplate(template,[book:b])
//        assertTrue result.contains("<li>Property [title] of class [class ValidationTagLibBook] cannot be null</li>")
//        assertTrue result.contains("<li>Property [publisherURL] of class [class ValidationTagLibBook] cannot be null</li>")
//        assertTrue result.contains("<li>Property [releaseDate] of class [class ValidationTagLibBook] cannot be null</li>")
//        assertTrue result.startsWith("<ul>")
//        assertTrue result.endsWith("</ul>")
//
//        b.clearErrors()
//        b.title = "Groovy in Action"
//        b.publisherURL = new URL("http://canoo.com/gia")
//        b.releaseDate = new Date()
//        b.usPrice = 10.99
//
//        assertTrue b.validate()
//        assertFalse b.hasErrors()
//
//        // should render nothing, not an empty ul - GRAILS-2709
//        assertOutputEquals("", template, [book:b])
//    }
//
//    void testRenderErrorsTagAsListWithNoBeanAttribute() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.validate()
//
//        assertTrue b.hasErrors()
//        request.setAttribute("bookInstance", b)
//
//        def template = '''<g:renderErrors as="list" />'''
//
//        def result = applyTemplate(template,[book:b])
//        assertEquals 1, result.count("<li>Property [title] of class [class ValidationTagLibBook] cannot be null</li>")
//        assertEquals 1, result.count("<li>Property [publisherURL] of class [class ValidationTagLibBook] cannot be null</li>")
//        assertEquals 1, result.count("<li>Property [releaseDate] of class [class ValidationTagLibBook] cannot be null</li>")
//        assertTrue result.startsWith("<ul>")
//        assertTrue result.endsWith("</ul>")
//    }
//
//    void testRenderErrorsAsXMLTag() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.validate()
//
//        assertTrue b.hasErrors()
//
//        def template = '''<g:renderErrors bean="${book}" as="xml" />'''
//
//        def result = applyTemplate(template,[book:b])
//
//        println result
//
//        def xml = new XmlSlurper().parseText(result)
//
//        assertEquals 4, xml.error.size()
//        assertEquals "ValidationTagLibBook", xml.error[0].@object.text()
//        assertEquals "publisherURL", xml.error[0].@field.text()
//        assertEquals "Property [publisherURL] of class [class ValidationTagLibBook] cannot be null", xml.error[0].@message.text()
//    }
//
//    void testHasErrorsWithRequestAttributes() {
//        StringWriter sw = new StringWriter()
//
//        withTag("hasErrors", sw) { tag ->
//
//            def mockErrors = [hasErrors:{true}]
//
//            request.setAttribute("somethingErrors", mockErrors as Errors)
//
//            // test when no message found it returns code
//            def attrs = [:]
//            tag.call(attrs, { "error found"})
//
//        }
//        assertEquals "error found", sw.toString()
//    }
//
//    void testMessageHtmlEscaping() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [title:"<script>alert('escape me')</script>"]
//
//        messageSource.addMessage("default.show.label", Locale.ENGLISH, ">{0}<")
//
//        def template = '''<title><g:message code="default.show.label" args="[book.title]" /></title>'''
//        def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
//        def expected = "<title>>&lt;script&gt;alert(&#39;escape me&#39;)&lt;/script&gt;<</title>"
//        assertOutputEquals(expected, template, [book:b])
//        assertOutputEquals(expected, htmlCodecDirective + template, [book:b])
//    }
//
//    void testMessageRawEncodeAs() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [title:"<b>bold</b> is ok"]
//
//        messageSource.addMessage("default.show.label", Locale.ENGLISH, ">{0}<")
//
//        def template = '''<title><g:message code="default.show.label" args="[book.title]" encodeAs="raw"/></title>'''
//        def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
//        def expected = "<title>><b>bold</b> is ok<</title>"
//        assertOutputEquals(expected, template, [book:b])
//        assertOutputEquals(expected, htmlCodecDirective + template, [book:b])
//    }
//
//    void testMessageNoneEncodeAs() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [title:"<b>bold</b> is ok"]
//
//        messageSource.addMessage("default.show.label", Locale.ENGLISH, ">{0}<")
//
//        def template = '''<title><g:message code="default.show.label" args="[book.title]" encodeAs="none"/></title>'''
//        def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
//        def expected = "<title>><b>bold</b> is ok<</title>"
//        assertOutputEquals(expected, template, [book:b])
//        assertOutputEquals(expected, htmlCodecDirective + template, [book:b])
//    }
//
//    void testMessageHtmlEscapingWithFunctionSyntaxCall() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [title:"<script>alert('escape me')</script>"]
//
//        messageSource.addMessage("default.show.label", Locale.ENGLISH, "{0}")
//
//        def template = '''<title>${g.message([code:"default.show.label", args:[book.title]])}</title>'''
//        def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
//        def expected = "<title>&lt;script&gt;alert(&#39;escape me&#39;)&lt;/script&gt;</title>"
//        assertOutputEquals(expected, template, [book:b])
//        assertOutputEquals(expected, htmlCodecDirective + template, [book:b])
//    }
//
//    void testMessageHtmlEscapingDifferentEncodings() {
//        def b = ga.getDomainClass("ValidationTagLibBook").newInstance()
//        b.properties = [title:"<script>alert('escape me')</script>"]
//
//        messageSource.addMessage("default.show.label", Locale.ENGLISH, "{0}")
//
//        def template = '''<title>${g.message([code:"default.show.label", args:[book.title]])}</title>'''
//        def htmlCodecDirective = '<%@page defaultCodec="HTML" %>'
//        def expected = "<title>&lt;script&gt;alert(&#39;escape me&#39;)&lt;/script&gt;</title>"
//
//        def resourceLoader = new MockStringResourceLoader()
//        resourceLoader.registerMockResource('/_sometemplate.gsp', htmlCodecDirective + template)
//        resourceLoader.registerMockResource('/_sometemplate_nocodec.gsp', template)
//        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)
//
//        assertOutputEquals(expected, '<g:render template="/sometemplate" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, template + '<g:render template="/sometemplate" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + template + '<g:render template="/sometemplate" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, '<g:render template="/sometemplate" model="[book:book]" />' + template, [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + '<g:render template="/sometemplate" model="[book:book]" />' + template, [book:b])
//
//        assertOutputEquals(expected, '<g:render template="/sometemplate_nocodec" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, template + '<g:render template="/sometemplate_nocodec" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + template + '<g:render template="/sometemplate_nocodec" model="[book:book]" />', [book:b])
//        assertOutputEquals(expected + expected, '<g:render template="/sometemplate_nocodec" model="[book:book]" />' + template, [book:b])
//        assertOutputEquals(expected + expected, htmlCodecDirective + '<g:render template="/sometemplate_nocodec" model="[book:book]" />' + template, [book:b])
//    }
//
//    void testMessageTagWithError() {
//        def error = new FieldError("foo", "bar",1, false, ["my.error.code"] as String[], null, "This is default")
//        def template = '<g:message error="${error}" />'
//
//        assertOutputEquals("This is default", template, [error:error])
//    }
//
//    void testMessageTagWithLocaleAttribute() {
//        messageSource.addMessage("welcome.message", Locale.ENGLISH, "Hello!")
//        messageSource.addMessage("welcome.message", Locale.ITALIAN, "Ciao!")
//
//        def template = '<g:message code="welcome.message" />'
//
//        assertOutputEquals("Hello!", template, [:])
//        assertOutputEquals("Hello!", template, [locale:Locale.ITALIAN])
//    }
//
//    void testMessageTagWithBlankButExistingMessageBundleValue() {
//
//        messageSource.addMessage("test.blank.message", Locale.ENGLISH, "")
//
//        def template = '<g:message code="test.blank.message" />'
//
//        assertOutputEquals("", template, [:])
//    }
//
//    void testMessageTagWithMessage() {
//        def resolvable = [
//                getArguments: {-> [] as Object[] },
//                getCodes: {-> ["my.message.code"] as String[] },
//                getDefaultMessage: {-> "The Default Message" }
//        ] as MessageSourceResolvable
//
//        def template = '<g:message message="${message}" />'
//
//        assertOutputEquals("The Default Message", template, [message: resolvable])
//    }
//
//    void testDefaultMessageAttributeWithAnEmptyStringValue() {
//        def template  = '<g:message code="my.message.code" default=""/>'
//        assertOutputEquals "", template
//    }
//
//    void testFieldValueTagWithMessageSourceResolvablePropertyUsesDefaultMessage() {
//        def Title = ga.getClassForName("Title")
//        def person = ga.getDomainClass("ValidationTagLibPerson").newInstance()
//        person.properties = [title: Title.MR, name: "Al Coholic"]
//
//        def template = '<g:fieldValue bean="${person}" field="title" />'
//
//        assertOutputEquals "Mr", template, [person: person]
//    }
//
//    void testFieldValueTagWithMessageSourceResolvablePropertyUsesI18nBundle() {
//        def Title = ga.getClassForName("Title")
//        def person = ga.getDomainClass("ValidationTagLibPerson").newInstance()
//        person.properties = [title: Title.MR, name: "Al Coholic"]
//
//        def locale = new Locale("af", "ZA")
//        messageSource.addMessage("Title.MR", locale, "Mnr")
//
//        webRequest.currentRequest.addPreferredLocale(locale)
//        def template = '<g:fieldValue bean="${person}" field="title" />'
//
//        assertOutputEquals "Mnr", template, [person: person]
//    }
//
//    void testFieldValueTagForNonDomainInstance() {
//        def template = '''<g:fieldValue bean="${book}" field="myUrl" />'''
//
//        def myBook = gcl.parseClass('''
//            import grails.persistence.*
//            @Entity
//            class MyBook {
//                String title
//                URL myUrl
//                Date releaseDate
//                BigDecimal usPrice
//            }''').newInstance()
//
//        myBook.myUrl = new URL("http://google.com")
//        assertOutputEquals("http://google.com", template, [book:myBook])
//    }
//
//    void testFieldValueTagForNonDomainInstanceWithNestedField() {
//        def template = '''<g:fieldValue bean="${book}" field="myUrl.publisherUrl" />'''
//
//        // gcl.loadClass("MyClass", true)
//        def myUrl = gcl.parseClass('''
//            class MyUrl {
//                URL publisherUrl
//            }''').newInstance()
//
//        def myBook = gcl.parseClass('''
//            import grails.persistence.*
//            @Entity
//            class MyBook {
//                String title
//                MyUrl myUrl
//                Date releaseDate
//                BigDecimal usPrice
//            }''').newInstance()
//
//        myBook.myUrl = myUrl
//        myBook.myUrl.publisherUrl = new URL("http://google.com")
//        assertOutputEquals("http://google.com", template, [book:myBook])
//    }
//
//    private void parsePhoneDomainTestClasses() {
//        gcl.parseClass('''
//            import grails.persistence.*
//            @Entity
//            class PhoneUsDomain {
//                String area
//                String prefix
//                String number
//            }''')
//
//        gcl.parseClass('''
//            import grails.persistence.*
//            @Entity
//            class PhoneUsInternationalDomain {
//                String country
//                String area
//                String prefix
//                String number
//            }''')
//
//        gcl.parseClass('''
//            import grails.persistence.*
//            @Entity
//
//            class PersonDomain {
//                String firstName
//                String lastName
//                PhoneUsInternationalDomain phoneUsInternational
//                PhoneUsDomain phoneUs
//                PhoneUsDomain otherPhoneUs
//            }''')
//    }
//
//    private void parsePhonePropertyEditorDomainClasses() {
//        gcl.parseClass('''
//            import java.beans.PropertyEditorSupport
//
//            class PhoneUsDomainMainEditor extends PropertyEditorSupport {
//                String getAsText() {
//                    def phoneUsNumber = getValue()
//                    return "${phoneUsNumber.area}-${phoneUsNumber.prefix}-${phoneUsNumber.number}"
//                }
//            }''')
//
//        gcl.parseClass('''
//            import java.beans.PropertyEditorSupport
//
//            class PhoneUsDomainForPropertyPathEditor extends PropertyEditorSupport {
//                String getAsText() {
//                    def phoneUsNumber = getValue()
//                    return "(${phoneUsNumber.area})${phoneUsNumber.prefix}-${phoneUsNumber.number}"
//                }
//            }''')
//
//        gcl.parseClass('''
//            import java.beans.PropertyEditorSupport
//
//            class PhoneUsInternationalDomainEditor extends PropertyEditorSupport {
//                String getAsText() {
//                    def phoneUsInternationalNumber = getValue()
//                    return "${phoneUsInternationalNumber.country}(${phoneUsInternationalNumber.area})" +
//                           "${phoneUsInternationalNumber.prefix}-${phoneUsInternationalNumber.number}"
//                }
//            }''')
//
//        gcl.parseClass('''
//            import org.springframework.beans.PropertyEditorRegistrar
//            import org.springframework.beans.PropertyEditorRegistry
//
//            class PhonePropertyEditorDomainRegistrar implements PropertyEditorRegistrar {
//                void registerCustomEditors(PropertyEditorRegistry registry) {
//                    registry.registerCustomEditor(PhoneUsInternationalDomain, new PhoneUsInternationalDomainEditor())
//                    registry.registerCustomEditor(PhoneUsDomain, new PhoneUsDomainMainEditor())
//                    registry.registerCustomEditor(PhoneUsDomain, "phoneUs", new PhoneUsDomainForPropertyPathEditor())
//                }
//            }''')
//    }
//
//    void testFieldValueTagWithPropertyEditorForDomainClasses() {
//        parsePhonePropertyEditorDomainClasses()
//
//        def phonePropertyEditorDomainRegistrarClazz = gcl.loadClass("PhonePropertyEditorDomainRegistrar")
//        appCtx.registerBeanDefinition(
//                "phonePropertyEditorDomainRegistrar", new RootBeanDefinition(phonePropertyEditorDomainRegistrarClazz))
//
//        def phoneUsInternationalDomain = ga.getDomainClass("PhoneUsInternationalDomain").newInstance()
//        phoneUsInternationalDomain.properties = [country:"+1", area:"123", prefix:"123", number:"1234"]
//        assertFalse phoneUsInternationalDomain.hasErrors()
//
//        def personDomain = ga.getDomainClass("PersonDomain").newInstance()
//        personDomain.properties = [firstName:"firstName1", lastName:"lastName1", phoneUsInternational:phoneUsInternationalDomain]
//        assertFalse personDomain.hasErrors()
//
//        def template = '''<g:fieldValue bean="${person}" field="phoneUsInternational" />'''
//        assertOutputEquals("+1(123)123-1234", template, [person:personDomain])
//    }
//
//    void testFieldValueTagWithPropertyEditorForDomainClassesWithPropertyPath() {
//        parsePhonePropertyEditorDomainClasses()
//
//        def phonePropertyEditorDomainRegistrarClazz = gcl.loadClass("PhonePropertyEditorDomainRegistrar")
//        appCtx.registerBeanDefinition(
//                "phonePropertyEditorDomainRegistrar", new RootBeanDefinition(phonePropertyEditorDomainRegistrarClazz))
//
//        def phoneUsDomain = ga.getDomainClass("PhoneUsDomain").newInstance()
//        phoneUsDomain.properties = [area:"123", prefix:"123", number:"1234"]
//        assertFalse phoneUsDomain.hasErrors()
//
//        def personDomain = ga.getDomainClass("PersonDomain").newInstance()
//        personDomain.properties = [firstName:"firstName1", lastName:"lastName1", phoneUs:phoneUsDomain, otherPhoneUs:phoneUsDomain]
//        assertFalse personDomain.hasErrors()
//
//        def template = '''<g:fieldValue bean="${person}" field="phoneUs" />'''
//        assertOutputEquals("(123)123-1234", template, [person:personDomain])
//
//        def otherTemplate = '''<g:fieldValue bean="${person}" field="otherPhoneUs" />'''
//        assertOutputEquals("123-123-1234", otherTemplate, [person:personDomain])
//    }
//
//    private void parsePhonePlainTestClasses() {
//        gcl.parseClass('''
//            class PhoneUs {
//                String area
//                String prefix
//                String number
//            }''')
//
//        gcl.parseClass('''
//            class PhoneUsInternational {
//                String country
//                String area
//                String prefix
//                String number
//            }''')
//
//        gcl.parseClass('''
//            class PersonPlain {
//                String firstName
//                String lastName
//                PhoneUsInternational phoneUsInternational
//                PhoneUs phoneUs
//                PhoneUs otherPhoneUs
//            }''')
//    }
//
//    private void parsePhonePropertyEditorPlainClasses() {
//        gcl.parseClass('''
//            import java.beans.PropertyEditorSupport
//
//            class PhoneUsMainEditor extends PropertyEditorSupport {
//                String getAsText() {
//                    def phoneUsNumber = getValue()
//                    return "${phoneUsNumber.area}-${phoneUsNumber.prefix}-${phoneUsNumber.number}"
//                }
//            }''')
//
//        gcl.parseClass('''
//            import java.beans.PropertyEditorSupport
//
//            class PhoneUsForPropertyPathEditor extends PropertyEditorSupport {
//                String getAsText() {
//                    def phoneUsNumber = getValue()
//                    return "(${phoneUsNumber.area})${phoneUsNumber.prefix}-${phoneUsNumber.number}"
//                }
//            }''')
//
//        gcl.parseClass('''
//            import java.beans.PropertyEditorSupport
//
//            class PhoneUsInternationalEditor extends PropertyEditorSupport {
//                String getAsText() {
//                    def phoneUsInternationalNumber = getValue()
//                    return "${phoneUsInternationalNumber.country}(${phoneUsInternationalNumber.area})" +
//                           "${phoneUsInternationalNumber.prefix}-${phoneUsInternationalNumber.number}"
//                }
//            }''')
//
//        gcl.parseClass('''
//            import org.springframework.beans.PropertyEditorRegistrar
//            import org.springframework.beans.PropertyEditorRegistry
//
//            class PhonePropertyEditorRegistrar implements PropertyEditorRegistrar {
//                void registerCustomEditors(PropertyEditorRegistry registry) {
//                    registry.registerCustomEditor(PhoneUsInternational, new PhoneUsInternationalEditor())
//                    registry.registerCustomEditor(PhoneUs, new PhoneUsMainEditor())
//                    registry.registerCustomEditor(PhoneUs, "phoneUs", new PhoneUsForPropertyPathEditor())
//                }
//            }''')
//    }
//
//    void testFieldValueTagWithPropertyEditorForNonDomainClasses() {
//        parsePhonePlainTestClasses()
//        parsePhonePropertyEditorPlainClasses()
//
//        def phonePropertyEditorRegistrarClazz = gcl.loadClass("PhonePropertyEditorRegistrar")
//        appCtx.registerBeanDefinition(
//                "phonePropertyEditorRegistrar", new RootBeanDefinition(phonePropertyEditorRegistrarClazz))
//
//        def phoneUsInternational = gcl.loadClass("PhoneUsInternational").newInstance()
//        phoneUsInternational.country = "+1"
//        phoneUsInternational.area = "123"
//        phoneUsInternational.prefix = "123"
//        phoneUsInternational.number = "1234"
//
//        def person = gcl.loadClass("PersonPlain").newInstance()
//        person.firstName = "firstName1"
//        person.lastName = "lastName1"
//        person.phoneUsInternational = phoneUsInternational
//
//        def template = '''<g:fieldValue bean="${person}" field="phoneUsInternational" />'''
//        assertOutputEquals("+1(123)123-1234", template, [person:person])
//    }
//
//    void testFieldValueTagWithPropertyEditorForNonDomainClassesWithPropertyPath() {
//        parsePhonePlainTestClasses()
//        parsePhonePropertyEditorPlainClasses()
//
//        def phonePropertyEditorRegistrarClazz = gcl.loadClass("PhonePropertyEditorRegistrar")
//        appCtx.registerBeanDefinition(
//                "phonePropertyEditorRegistrar", new RootBeanDefinition(phonePropertyEditorRegistrarClazz))
//
//        def phoneUs = gcl.loadClass("PhoneUs").newInstance()
//        phoneUs.area = "123"
//        phoneUs.prefix = "123"
//        phoneUs.number = "1234"
//
//        def person = gcl.loadClass("PersonPlain").newInstance()
//        person.firstName = "firstName1"
//        person.lastName = "lastName1"
//        person.phoneUs = phoneUs
//        person.otherPhoneUs = phoneUs
//
//        def template = '''<g:fieldValue bean="${person}" field="phoneUs" />'''
//        assertOutputEquals("(123)123-1234", template, [person:person])
//
//        def otherTemplate = '''<g:fieldValue bean="${person}" field="otherPhoneUs" />'''
//        assertOutputEquals("123-123-1234", otherTemplate, [person:person])
//    }
}

@Entity
class ValidationTagLibBook {
    String title
    URL publisherURL
    Date releaseDate
    BigDecimal usPrice
}