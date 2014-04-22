package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.plugins.GrailsPluginInfo

import spock.lang.Issue
import spock.lang.Specification

class GroovyPageConfigSpec extends Specification {

    @Issue("GRAILS-11331")
    def "per plugin settings should be supported"() {
        given:
        def flatConfig = ['platformUi.grails.views.gsp.codecs.expression': 'none']
        def groovyPageConfig = new GroovyPageConfig(flatConfig)
        def grailsPluginInfo = Mock(GrailsPluginInfo)
        when:
        def codecSettings=groovyPageConfig.getCodecSettings(grailsPluginInfo, "expression")
        then:
        grailsPluginInfo.getName() >> {
            'platformUi'
        }
        0 * grailsPluginInfo._
        codecSettings == 'none'
    }

}
