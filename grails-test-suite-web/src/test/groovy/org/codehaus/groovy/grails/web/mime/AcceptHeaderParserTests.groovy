package org.codehaus.groovy.grails.web.mime

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class AcceptHeaderParserTests extends GroovyTestCase {

    def config
    protected void setUp() {
        config = new ConfigSlurper().parse("""
grails.mime.types = [ xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      cvs: 'text/csv',
                      all: '*/*',
                      json: 'application/json',
                      html: ['text/html','application/xhtml+xml']
                    ]
        """)

    }

    protected void tearDown() {
        config = null
        ConfigurationHolder.setConfig null
        MimeType.reset()
    }

    void testXmlContentTypeWithCharset() {
        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("text/xml; charset=UTF-8")

        assertEquals 1, mimes.size()
        assertEquals "application/xml", mimes[0].name
        assertEquals "xml", mimes[0].extension
        assertEquals('UTF-8', mimes[0].parameters.charset)
        assertEquals('1.0', mimes[0].parameters.q)
    }

    void testFirefox2AcceptHeaderOrdering() {

        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5")

        assertEquals 5, mimes.size()

        assertEquals(['application/xhtml+xml','application/xml', 'text/html', 'text/plain', '*/*'], mimes.name)
        assertEquals(['html', 'xml', 'html', 'text', 'all'], mimes.extension)
    }

    void testFirefox3AcceptHeaderOrdering() {

        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

        assertEquals 4, mimes.size()

        assertEquals(['html','html','xml', 'all'], mimes.extension)
    }

    void testAcceptHeaderWithQNumberOrdering() {
        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("text/html,application/xhtml+xml,application/xml;q=1.1,*/*;q=0.8")

        assertEquals 4, mimes.size()

        assertEquals(['xml','html','html', 'all'], mimes.extension)
    }

    void testPrototypeHeaderOrdering() {
        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("text/javascript, text/html, application/xml, text/xml, */*")

        assertEquals 4, mimes.size()

        assertEquals(["js",'html', 'xml', 'all'], mimes.extension)
        assertEquals(["text/javascript",'text/html', 'application/xml', '*/*'], mimes.name)
    }

    void testOldBrowserHeader() {
        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("*/*")

        assertEquals 1, mimes.size()
        assertEquals(['all'], mimes.extension)
    }

    // test for GRAILS-3389
    void testAcceptExtensionWithTokenNoValue() {
        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("text/html,application/xhtml+xml,application/xml;token,*/*;q=0.8")

        assertEquals 4, mimes.size()

        assertEquals(['html','html', 'xml','all'], mimes.extension)
    }

    // test for GRAILS-3493
    void testAcceptHeaderWithNoQValue() {
        def mimes = new DefaultAcceptHeaderParser(new DefaultGrailsApplication(config:config)).parse("application/xml; charset=UTF-8")

        assertEquals 1, mimes.size()
    }
}
