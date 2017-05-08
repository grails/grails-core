package org.grails.web.mapping

import grails.core.GrailsApplication
import grails.web.mapping.UrlMappingData
import grails.web.mapping.UrlMappingInfo
import spock.lang.Specification

class UrlMappingUtilsSpec extends Specification {

    def "test buildDispatchUrlForMapping"() {
        when:
        UrlMappingData urlData = new ResponseCodeMappingData("500")
        UrlMappingInfo info = new DefaultUrlMappingInfo([controller: 'error', action: 'logError'], urlData, null)
        String dispatchUrl = UrlMappingUtils.buildDispatchUrlForMapping(info, true)

        then:
        dispatchUrl == 'fresa'
    }
}
