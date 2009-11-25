package org.codehaus.groovy.grails.web.filters

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptTagLib
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

/**
 * Sets up the Javascript library to use based on configuration
 *
 * @author Graeme Rocher
 * @since 1.2
 *
 */
class JavascriptLibraryFilters implements GrailsApplicationAware {
    GrailsApplication grailsApplication
    static final Log LOG = LogFactory.getLog(JavascriptLibraryFilters)
    def filters = {
        def library = grailsApplication?.config?.grails?.views?.javascript?.library ?: null

        LOG.debug "Using [$library] as the default Ajax provider." 
        all(controller:'*', action:'*') {
            before = {
                if(!request[JavascriptTagLib.INCLUDED_LIBRARIES]) request[JavascriptTagLib.INCLUDED_LIBRARIES] = []
                if(library) {                    
                    request[JavascriptTagLib.INCLUDED_LIBRARIES] << library
                }
            }
        }
    }

}