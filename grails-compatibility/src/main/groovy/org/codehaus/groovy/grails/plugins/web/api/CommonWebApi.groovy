package org.codehaus.groovy.grails.plugins.web.api

import org.codehaus.groovy.grails.plugins.GrailsPluginManager

/**
 * @deprecated Use {@link org.grails.web.api.CommonWebApi} instead
 */
@Deprecated
class CommonWebApi extends org.grails.web.api.CommonWebApi {

    CommonWebApi(GrailsPluginManager pluginManager) {
        super(pluginManager)
    }

    CommonWebApi() {
    }
}
