package org.grails.web.mime

import grails.core.DefaultGrailsApplication
import grails.util.Holders
import org.grails.config.PropertySourcesConfig
import org.grails.plugins.web.mime.MimeTypesFactoryBean
import org.grails.web.mime.DefaultAcceptHeaderParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class AcceptHeaderParserTests {

    def config

    @BeforeEach
    protected void setUp() {
        def configObject = new ConfigSlurper().parse("""
grails.mime.types = [ xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      cvs: 'text/csv',
                      all: '*/*',
                      json: 'application/json',
                      html: ['text/html','application/xhtml+xml'],
                      foov1: 'application/vnd.foo+json;v=1.0',
                      foov2: 'application/vnd.foo+json;v=2.0'
                    ]
        """)
        def ps = new MutablePropertySources()
        ps.addLast(new MapPropertySource("grails", configObject))
        config = new PropertySourcesConfig(ps)

    }

    @AfterEach
    protected void tearDown() {
        config = null
        Holders.setConfig null
    }

    @Test
    void testXmlContentTypeWithCharset() {

        DefaultAcceptHeaderParser parser = getAcceptHeaderParser()
        def mimes = parser.parse("text/xml; charset=UTF-8")

        assertEquals 1, mimes.size()
        assertEquals "application/xml", mimes[0].name
        assertEquals "xml", mimes[0].extension
        assertEquals('UTF-8', mimes[0].parameters.charset)
        assertEquals('1.0', mimes[0].parameters.q)
        assertEquals('1.0', mimes[0].quality)
    }

    protected DefaultAcceptHeaderParser getAcceptHeaderParser() {
        final application = new DefaultGrailsApplication(config: config)
        final factoryBean = new MimeTypesFactoryBean(grailsApplication: application)
        final parser = new DefaultAcceptHeaderParser(factoryBean.getObject())
        parser
    }

    @Test
    void testXmlContentTypeWithCharsetAndVersion() {
        def mimes = getAcceptHeaderParser().parse("text/xml; charset=UTF-8; v=1.1")

        assertEquals 1, mimes.size()
        assertEquals "application/xml", mimes[0].name
        assertEquals "xml", mimes[0].extension
        assertEquals('UTF-8', mimes[0].parameters.charset)
        assertEquals('1.0', mimes[0].parameters.q)
        assertEquals('1.0', mimes[0].quality)
        assertEquals('1.1', mimes[0].version)
    }

    @Test
    void testGRAILS10678() {
        def mimes = getAcceptHeaderParser().parse("application/json;")

        assertEquals 1, mimes.size()
        assertEquals "application/json", mimes[0].name
        assertEquals "json", mimes[0].extension
    }

    @Test
    void testFirefox2AcceptHeaderOrdering() {

        def mimes = getAcceptHeaderParser().parse("text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5")

        assertEquals 5, mimes.size()

        assertEquals(['application/xhtml+xml','application/xml', 'text/html', 'text/plain', '*/*'], mimes.name)
        assertEquals(['html', 'xml', 'html', 'text', 'all'], mimes.extension)
    }

    @Test
    void testFirefox3AcceptHeaderOrdering() {

        def mimes = getAcceptHeaderParser().parse("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

        assertEquals 4, mimes.size()

        assertEquals(['html','html','xml', 'all'], mimes.extension)
    }

    @Test
    void testParseAcceptHeaderWithNonNumericQualityValue() {

        def mimes = getAcceptHeaderParser().parse("text/html,application/xhtml+xml,application/xml;q=blah,*/*;q=0.8")

        assertEquals 4, mimes.size()

        assertEquals(['html','html','xml', 'all'], mimes.extension)
    }

    @Test
    void testAcceptHeaderWithQNumberOrdering() {
        def mimes = getAcceptHeaderParser().parse("text/html,application/xhtml+xml,application/xml;q=1.1,*/*;q=0.8")

        assertEquals 4, mimes.size()

        assertEquals(['xml','html','html', 'all'], mimes.extension)
    }

    @Test
    void testPrototypeHeaderOrdering() {
        def mimes = getAcceptHeaderParser().parse("text/javascript, text/html, application/xml, text/xml, */*")

        assertEquals 4, mimes.size()

        assertEquals(["js",'html', 'xml', 'all'], mimes.extension)
        assertEquals(["text/javascript",'text/html', 'application/xml', '*/*'], mimes.name)
    }

    @Test
    void testOldBrowserHeader() {
        def mimes = getAcceptHeaderParser().parse("*/*")

        assertEquals 1, mimes.size()
        assertEquals(['all'], mimes.extension)
    }

    @Test
    // test for GRAILS-3389
    void testAcceptExtensionWithTokenNoValue() {
        def mimes = getAcceptHeaderParser().parse("text/html,application/xhtml+xml,application/xml;token,*/*;q=0.8")

        assertEquals 4, mimes.size()

        assertEquals(['html','html', 'xml','all'], mimes.extension)
    }

    @Test
    // test for GRAILS-3493
    void testAcceptHeaderWithNoQValue() {
        def mimes = getAcceptHeaderParser().parse("application/xml; charset=UTF-8")

        assertEquals 1, mimes.size()
    }

    @Test
    void testAcceptExtensionWithCustomVndTypeAndVersion() {
        def mimesV1 = getAcceptHeaderParser().parse("application/vnd.foo+json;v=1.0; charset=UTF-8")
        def mimesV2 = getAcceptHeaderParser().parse("application/vnd.foo+json;v=2.0; charset=UTF-8")

        assertEquals(['foov1'], mimesV1.extension)
        assertEquals(['foov2'], mimesV2.extension)
    }
}
