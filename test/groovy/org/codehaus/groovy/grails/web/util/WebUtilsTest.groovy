/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 26, 2007
 */
package org.codehaus.groovy.grails.web.util

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class WebUtilsTest extends GroovyTestCase {

    protected void setUp() {
        def config = new ConfigSlurper().parse( """
grails.mime.file.extensions=false
        """)

        ConfigurationHolder.setConfig config

    }

    protected void tearDown() {
        ConfigurationHolder.setConfig null
    }


    void testAreFileExtensionsEnabled() {
         assert !WebUtils.areFileExtensionsEnabled()

         ConfigurationHolder.config.grails.mime.file.extensions=true
         
         assert WebUtils.areFileExtensionsEnabled()
    }

    void testGetFormatFromURI() {
        assertEquals "xml", WebUtils.getFormatFromURI("/foo/bar.xml")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo.xml")
        assertEquals "xml", WebUtils.getFormatFromURI("/foo/bar.suff/bar.xml")
    }

}