package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

class ValidationTagLibTests extends AbstractGrailsTagTests {

    protected void onSetUp() {

        gcl.parseClass '''
import grails.persistence.*

@Entity
class Book {
    String title
    URL publisherURL
    Date releaseDate
    BigDecimal usPrice
}
'''

        gcl.parseClass '''
import grails.persistence.*

@Entity
class Article {
    String title
}
'''

        gcl.parseClass '''
import grails.persistence.*

@Entity
class Person {
    Title title
    String name
}

enum Title implements org.springframework.context.MessageSourceResolvable {
    MR, MRS, MS, DR

    String[] getCodes() {
        ["${getClass().name}.${name()}"] as String[]
    }

    Object[] getArguments() {
        [] as Object[]
    }

    String getDefaultMessage() {
        use(org.apache.commons.lang.WordUtils) {
            name().toLowerCase().replaceAll(/_+/, " ").capitalizeFully()
        }
    }
}
'''

        parsePhoneDomainTestClasses()
    }



    void testFieldValueWithClassAndPropertyNameLookupFromBundle() {
        def domain = ga.getDomainClass("Book")

        LocaleContextHolder.setLocale(Locale.US)
        messageSource.addMessage("Book.label", Locale.US, "Reading Material")
        messageSource.addMessage("Book.title.label", Locale.US, "Subject")
        def b = domain.newInstance()
        b.validate()
        assertTrue b.hasErrors()

        def template = '<g:fieldError bean="${book}" field="title" />'

        webRequest.currentRequest.addPreferredLocale(Locale.US)
        assertOutputEquals 'Property [Subject] of class [Reading Material] cannot be null', template, [book:b]
    }

    void testFieldValueWithShortClassAndPropertyNameLookupFromBundle() {
        def domain = ga.getDomainClass("Book")

        LocaleContextHolder.setLocale(Locale.US)
        messageSource.addMessage("book.label", Locale.US, "Reading Material")
        messageSource.addMessage("book.title.label", Locale.US, "Subject")
        def b = domain.newInstance()
        b.validate()
        assertTrue b.hasErrors()

        def template = '<g:fieldError bean="${book}" field="title" />'

        webRequest.currentRequest.addPreferredLocale(Locale.US)
        assertOutputEquals 'Property [Subject] of class [Reading Material] cannot be null', template, [book:b]
    }

    void testRenderErrorTag() {
        def domain = ga.getDomainClass("Book")
        def b = domain.newInstance()
        b.validate()
        assertTrue b.hasErrors()

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
        assertTrue b.hasErrors()

        def template = '''<g:fieldValue bean="${book}" field="publisherURL" />'''

        assertOutputEquals("a_bad_url", template, [book:b])

        b.properties = [publisherURL:"http://google.com"]
        assertFalse b.hasErrors()

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

    void testFieldValueTagWithFrenchLocaleInTextField() {
        def b = ga.getDomainClass("Book").newInstance()
        b.properties = [publisherURL:"http://google.com", usPrice: 1045.99]

        String template = '''<g:textField name="usPrice" value="${fieldValue(bean: book, field: 'usPrice')}" />'''

        // First test with English.
        webRequest.currentRequest.addPreferredLocale(Locale.US)

        assertOutputEquals '<input type="text" name="usPrice" value="1,045.99" id="usPrice" />',
                template, [book:b]

        webRequest.currentRequest.removeAttribute(GrailsApplicationAttributes.PROPERTY_REGISTRY)
        // And then with French.
        webRequest.currentRequest.addPreferredLocale(Locale.FRENCH)

        assertOutputEquals '<input type="text" name="usPrice" value="1&nbsp;045,99" id="usPrice" />',
                template, [book:b]
    }

    void testHasErrorsTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assertTrue b.hasErrors()

        def template = '''<g:hasErrors bean="${book}">success</g:hasErrors>'''

        assertOutputEquals("success", template, [book:b])

        b = ga.getDomainClass("Book").newInstance()
        b.title = "Groovy In Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.validate()
        assertTrue b.hasErrors()
        assertOutputEquals("success", '''<g:hasErrors bean="${book}" field="releaseDate">success</g:hasErrors>''', [book:b])
        assertOutputEquals("success", '''<g:hasErrors model="[book:book]" field="releaseDate">success</g:hasErrors>''', [book:b])
        assertOutputEquals("", '''<g:hasErrors bean="${book}" field="title">success</g:hasErrors>''', [book:b])
        assertOutputEquals("", '''<g:hasErrors model="[book:book]" field="title">success</g:hasErrors>''', [book:b])

        b.clearErrors()
        b.title = "Groovy in Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.releaseDate = new Date()
        b.usPrice = 10.99

        assertTrue b.validate()
        assertFalse b.hasErrors()

        assertOutputEquals("", template, [book:b])

        b = ga.getDomainClass("Book").newInstance()
        b.title = "Groovy In Action"
        b.publisherURL = new URL("http://canoo.com/gia")
        b.releaseDate = new Date()
        b.usPrice = 10.99
        b.validate()
        def a = ga.getDomainClass("Article").newInstance()
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

        assertTrue b.hasErrors()

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

        assertTrue b.hasErrors()

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

        assertTrue b.validate()
        assertFalse b.hasErrors()

        // should render nothing, not an empty ul - GRAILS-2709
        assertOutputEquals("", template, [book:b])
    }

    void testRenderErrorsTagAsListWithNoBeanAttribute() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assertTrue b.hasErrors()
        request.setAttribute("bookInstance", b)

        def template = '''<g:renderErrors as="list" />'''

        def result = applyTemplate(template,[book:b])
        assertEquals 1, result.count("<li>Property [title] of class [class Book] cannot be null</li>")
        assertEquals 1, result.count("<li>Property [publisherURL] of class [class Book] cannot be null</li>")
        assertEquals 1, result.count("<li>Property [releaseDate] of class [class Book] cannot be null</li>")
        assertTrue result.startsWith("<ul>")
        assertTrue result.endsWith("</ul>")
    }

    void testRenderErrorsAsXMLTag() {
        def b = ga.getDomainClass("Book").newInstance()
        b.validate()

        assertTrue b.hasErrors()

        def template = '''<g:renderErrors bean="${book}" as="xml" />'''

        def result = applyTemplate(template,[book:b])

        println result

        def xml = new XmlSlurper().parseText(result)

        assertEquals 4, xml.error.size()
        assertEquals "Book", xml.error[0].@object.text()
        assertEquals "publisherURL", xml.error[0].@field.text()
        assertEquals "Property [publisherURL] of class [class Book] cannot be null", xml.error[0].@message.text()
    }

    void testHasErrorsWithRequestAttributes() {
        StringWriter sw = new StringWriter()

        withTag("hasErrors", sw) { tag ->

            def mockErrors = [hasErrors:{true}]

            request.setAttribute("somethingErrors", mockErrors as Errors)

            // test when no message found it returns code
            def attrs = [:]
            tag.call(attrs, { "error found"})

            assertEquals "error found", sw.toString()
        }
    }

    void testMessageTagWithError() {
        def error = new FieldError("foo", "bar",1, false, ["my.error.code"] as String[], null, "This is default")
        def template = '<g:message error="${error}" />'

        assertOutputEquals("This is default", template, [error:error])
    }

    void testMessageTagWithLocaleAttribute() {
        messageSource.addMessage("welcome.message", Locale.ENGLISH, "Hello!")
        messageSource.addMessage("welcome.message", Locale.ITALIAN, "Ciao!")

        def template = '<g:message code="welcome.message" />'

        assertOutputEquals("Hello!", template, [:])
        assertOutputEquals("Hello!", template, [locale:Locale.ITALIAN])

    }

    void testMessageTagWithBlankButExistingMessageBundleValue() {

        messageSource.addMessage("test.blank.message", Locale.ENGLISH, "")

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

    void testDefaultMessageAttributeWithAnEmptyStringValue() {
        def template  = '<g:message code="my.message.code" default=""/>'
        assertOutputEquals "", template
    }

    void testFieldValueTagWithMessageSourceResolvablePropertyUsesDefaultMessage() {
        def Title = ga.getClassForName("Title")
        def person = ga.getDomainClass("Person").newInstance()
        person.properties = [title: Title.MR, name: "Al Coholic"]

        def template = '<g:fieldValue bean="${person}" field="title" />'

        assertOutputEquals "Mr", template, [person: person]
    }

    void testFieldValueTagWithMessageSourceResolvablePropertyUsesI18nBundle() {
        def Title = ga.getClassForName("Title")
        def person = ga.getDomainClass("Person").newInstance()
        person.properties = [title: Title.MR, name: "Al Coholic"]

        def locale = new Locale("af", "ZA")
        messageSource.addMessage("Title.MR", locale, "Mnr")

        webRequest.currentRequest.addPreferredLocale(locale)
        def template = '<g:fieldValue bean="${person}" field="title" />'

        assertOutputEquals "Mnr", template, [person: person]
    }

    void testFieldValueTagForNonDomainInstance() {
        def template = '''<g:fieldValue bean="${book}" field="myUrl" />'''

        def myBook = gcl.parseClass('''
            import grails.persistence.*
            @Entity
            class MyBook {
                String title
                URL myUrl
                Date releaseDate
                BigDecimal usPrice
            }''').newInstance()

        myBook.myUrl = new URL("http://google.com")
        assertOutputEquals("http://google.com", template, [book:myBook])
    }

    void testFieldValueTagForNonDomainInstanceWithNestedField() {
        def template = '''<g:fieldValue bean="${book}" field="myUrl.publisherUrl" />'''

        // gcl.loadClass("MyClass", true)
        def myUrl = gcl.parseClass('''
            class MyUrl {
                URL publisherUrl
            }''').newInstance()

        def myBook = gcl.parseClass('''
            import grails.persistence.*
            @Entity
            class MyBook {
                String title
                MyUrl myUrl
                Date releaseDate
                BigDecimal usPrice
            }''').newInstance()

        myBook.myUrl = myUrl
        myBook.myUrl.publisherUrl = new URL("http://google.com")
        assertOutputEquals("http://google.com", template, [book:myBook])
    }

    private void parsePhoneDomainTestClasses() {
        gcl.parseClass('''
            import grails.persistence.*
            @Entity
            class PhoneUsDomain {
                String area
                String prefix
                String number
            }''')

        gcl.parseClass('''
            import grails.persistence.*
            @Entity
            class PhoneUsInternationalDomain {
                String country
                String area
                String prefix
                String number
            }''')

        gcl.parseClass('''
            import grails.persistence.*
            @Entity

            class PersonDomain {
                String firstName
                String lastName
                PhoneUsInternationalDomain phoneUsInternational
                PhoneUsDomain phoneUs
                PhoneUsDomain otherPhoneUs
            }''')
    }

    private void parsePhonePropertyEditorDomainClasses() {
        gcl.parseClass('''
            import java.beans.PropertyEditorSupport

            class PhoneUsDomainMainEditor extends PropertyEditorSupport {
                public String getAsText() {
                    def phoneUsNumber = getValue()
                    return "${phoneUsNumber.area}-${phoneUsNumber.prefix}-${phoneUsNumber.number}"
                }
            }''')

        gcl.parseClass('''
            import java.beans.PropertyEditorSupport

            class PhoneUsDomainForPropertyPathEditor extends PropertyEditorSupport {
                public String getAsText() {
                    def phoneUsNumber = getValue()
                    return "(${phoneUsNumber.area})${phoneUsNumber.prefix}-${phoneUsNumber.number}"
                }
            }''')

        gcl.parseClass('''
            import java.beans.PropertyEditorSupport

            class PhoneUsInternationalDomainEditor extends PropertyEditorSupport {
                public String getAsText() {
                    def phoneUsInternationalNumber = getValue()
                    return "${phoneUsInternationalNumber.country}(${phoneUsInternationalNumber.area})" +
                           "${phoneUsInternationalNumber.prefix}-${phoneUsInternationalNumber.number}"
                }
            }''')

        gcl.parseClass('''
            import org.springframework.beans.PropertyEditorRegistrar
            import org.springframework.beans.PropertyEditorRegistry

            class PhonePropertyEditorDomainRegistrar implements PropertyEditorRegistrar {
                public void registerCustomEditors(PropertyEditorRegistry registry) {
                    registry.registerCustomEditor(PhoneUsInternationalDomain, new PhoneUsInternationalDomainEditor())
                    registry.registerCustomEditor(PhoneUsDomain, new PhoneUsDomainMainEditor())
                    registry.registerCustomEditor(PhoneUsDomain, "phoneUs", new PhoneUsDomainForPropertyPathEditor())
                }
            }''')
    }

    void testFieldValueTagWithPropertyEditorForDomainClasses() {
        parsePhonePropertyEditorDomainClasses()

        def phonePropertyEditorDomainRegistrarClazz = gcl.loadClass("PhonePropertyEditorDomainRegistrar")
        appCtx.registerBeanDefinition(
                "phonePropertyEditorDomainRegistrar", new RootBeanDefinition(phonePropertyEditorDomainRegistrarClazz))

        def phoneUsInternationalDomain = ga.getDomainClass("PhoneUsInternationalDomain").newInstance()
        phoneUsInternationalDomain.properties = [country:"+1", area:"123", prefix:"123", number:"1234"]
        assertFalse phoneUsInternationalDomain.hasErrors()

        def personDomain = ga.getDomainClass("PersonDomain").newInstance()
        personDomain.properties = [firstName:"firstName1", lastName:"lastName1", phoneUsInternational:phoneUsInternationalDomain]
        assertFalse personDomain.hasErrors()

        def template = '''<g:fieldValue bean="${person}" field="phoneUsInternational" />'''
        assertOutputEquals("+1(123)123-1234", template, [person:personDomain])
    }

    void testFieldValueTagWithPropertyEditorForDomainClassesWithPropertyPath() {
        parsePhonePropertyEditorDomainClasses()

        def phonePropertyEditorDomainRegistrarClazz = gcl.loadClass("PhonePropertyEditorDomainRegistrar")
        appCtx.registerBeanDefinition(
                "phonePropertyEditorDomainRegistrar", new RootBeanDefinition(phonePropertyEditorDomainRegistrarClazz))

        def phoneUsDomain = ga.getDomainClass("PhoneUsDomain").newInstance()
        phoneUsDomain.properties = [area:"123", prefix:"123", number:"1234"]
        assertFalse phoneUsDomain.hasErrors()

        def personDomain = ga.getDomainClass("PersonDomain").newInstance()
        personDomain.properties = [firstName:"firstName1", lastName:"lastName1", phoneUs:phoneUsDomain, otherPhoneUs:phoneUsDomain]
        assertFalse personDomain.hasErrors()

        def template = '''<g:fieldValue bean="${person}" field="phoneUs" />'''
        assertOutputEquals("(123)123-1234", template, [person:personDomain])

        def otherTemplate = '''<g:fieldValue bean="${person}" field="otherPhoneUs" />'''
        assertOutputEquals("123-123-1234", otherTemplate, [person:personDomain])
    }

    private void parsePhonePlainTestClasses() {
        gcl.parseClass('''
            class PhoneUs {
                String area
                String prefix
                String number
            }''')

        gcl.parseClass('''
            class PhoneUsInternational {
                String country
                String area
                String prefix
                String number
            }''')

        gcl.parseClass('''
            class PersonPlain {
                String firstName
                String lastName
                PhoneUsInternational phoneUsInternational
                PhoneUs phoneUs
                PhoneUs otherPhoneUs
            }''')
    }

    private void parsePhonePropertyEditorPlainClasses() {
        gcl.parseClass('''
            import java.beans.PropertyEditorSupport

            class PhoneUsMainEditor extends PropertyEditorSupport {
                public String getAsText() {
                    def phoneUsNumber = getValue()
                    return "${phoneUsNumber.area}-${phoneUsNumber.prefix}-${phoneUsNumber.number}"
                }
            }''')

        gcl.parseClass('''
            import java.beans.PropertyEditorSupport

            class PhoneUsForPropertyPathEditor extends PropertyEditorSupport {
                public String getAsText() {
                    def phoneUsNumber = getValue()
                    return "(${phoneUsNumber.area})${phoneUsNumber.prefix}-${phoneUsNumber.number}"
                }
            }''')

        gcl.parseClass('''
            import java.beans.PropertyEditorSupport

            class PhoneUsInternationalEditor extends PropertyEditorSupport {
                public String getAsText() {
                    def phoneUsInternationalNumber = getValue()
                    return "${phoneUsInternationalNumber.country}(${phoneUsInternationalNumber.area})" +
                           "${phoneUsInternationalNumber.prefix}-${phoneUsInternationalNumber.number}"
                }
            }''')

        gcl.parseClass('''
            import org.springframework.beans.PropertyEditorRegistrar
            import org.springframework.beans.PropertyEditorRegistry

            class PhonePropertyEditorRegistrar implements PropertyEditorRegistrar {
                public void registerCustomEditors(PropertyEditorRegistry registry) {
                    registry.registerCustomEditor(PhoneUsInternational, new PhoneUsInternationalEditor())
                    registry.registerCustomEditor(PhoneUs, new PhoneUsMainEditor())
                    registry.registerCustomEditor(PhoneUs, "phoneUs", new PhoneUsForPropertyPathEditor())
                }
            }''')
    }

    void testFieldValueTagWithPropertyEditorForNonDomainClasses() {
        parsePhonePlainTestClasses()
        parsePhonePropertyEditorPlainClasses()

        def phonePropertyEditorRegistrarClazz = gcl.loadClass("PhonePropertyEditorRegistrar")
        appCtx.registerBeanDefinition(
                "phonePropertyEditorRegistrar", new RootBeanDefinition(phonePropertyEditorRegistrarClazz))

        def phoneUsInternational = gcl.loadClass("PhoneUsInternational").newInstance()
        phoneUsInternational.country = "+1"
        phoneUsInternational.area = "123"
        phoneUsInternational.prefix = "123"
        phoneUsInternational.number = "1234"

        def person = gcl.loadClass("PersonPlain").newInstance()
        person.firstName = "firstName1"
        person.lastName = "lastName1"
        person.phoneUsInternational = phoneUsInternational

        def template = '''<g:fieldValue bean="${person}" field="phoneUsInternational" />'''
        assertOutputEquals("+1(123)123-1234", template, [person:person])
    }

    void testFieldValueTagWithPropertyEditorForNonDomainClassesWithPropertyPath() {
        parsePhonePlainTestClasses()
        parsePhonePropertyEditorPlainClasses()

        def phonePropertyEditorRegistrarClazz = gcl.loadClass("PhonePropertyEditorRegistrar")
        appCtx.registerBeanDefinition(
                "phonePropertyEditorRegistrar", new RootBeanDefinition(phonePropertyEditorRegistrarClazz))

        def phoneUs = gcl.loadClass("PhoneUs").newInstance()
        phoneUs.area = "123"
        phoneUs.prefix = "123"
        phoneUs.number = "1234"

        def person = gcl.loadClass("PersonPlain").newInstance()
        person.firstName = "firstName1"
        person.lastName = "lastName1"
        person.phoneUs = phoneUs
        person.otherPhoneUs = phoneUs

        def template = '''<g:fieldValue bean="${person}" field="phoneUs" />'''
        assertOutputEquals("(123)123-1234", template, [person:person])

        def otherTemplate = '''<g:fieldValue bean="${person}" field="otherPhoneUs" />'''
        assertOutputEquals("123-123-1234", otherTemplate, [person:person])
    }
}
