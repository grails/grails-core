/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 23, 2007
 */
package org.codehaus.groovy.grails.web.mime

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class AcceptHeaderParserTests extends GroovyTestCase {

    protected void setUp() {
        def config = new ConfigSlurper().parse( """
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

        ConfigurationHolder.setConfig config
    }

    protected void tearDown() {
        ConfigurationHolder.setConfig null
    }


    void testXmlContentTypeWithCharset() {
        def mimes = new DefaultAcceptHeaderParser().parse("text/xml; charset=UTF-8")

        assertEquals 1, mimes.size()
        assertEquals "text/xml", mimes[0].name
        assertEquals "xml", mimes[0].extension
        assertEquals( [charset:'UTF-8'], mimes[0].parameters )
    }


    void testAcceptHeaderOrdering() {
        def mimes = new DefaultAcceptHeaderParser().parse("text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5")

        assertEquals 6, mimes.size()

        assertEquals( ['text/html', 'text/plain', '*/*','text/xml', 'application/xml', 'application/xhtml+xml'], mimes.name )
        assertEquals( ['html', 'text', 'all', 'xml','xml', 'html'], mimes.extension )
        
    }

    void testPrototypeHeaderOrdering() {
        def mimes = new DefaultAcceptHeaderParser().parse("text/javascript, text/html, application/xml, text/xml, */*")

        assertEquals 5, mimes.size()

        assertEquals( ["text/javascript",'text/html', 'application/xml', 'text/xml', '*/*'], mimes.name )
        assertEquals( ["js",'html', 'xml', 'xml', 'all'], mimes.extension )
        
    }

    void testOldBrowserHeader() {
        def mimes = new DefaultAcceptHeaderParser().parse("*/*")

        assertEquals 1, mimes.size()
        assertEquals( ['all'], mimes.extension )
    }


}
