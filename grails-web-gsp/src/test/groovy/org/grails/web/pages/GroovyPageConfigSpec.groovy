package org.grails.web.pages

import grails.plugins.GrailsPluginInfo
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll;

class GroovyPageConfigSpec extends Specification {

    @Issue("GRAILS-11331")
    @Unroll
    def "per plugin settings should be supported - #pluginNameInConfig"() {
        given:
        def flatConfig = [("${pluginNameInConfig}.grails.views.gsp.codecs.expression".toString()): 'none']
        def groovyPageConfig = new GroovyPageConfig(flatConfig)
        def grailsPluginInfo = Mock(GrailsPluginInfo)
        when:
        def codecSettings=groovyPageConfig.getCodecSettings(grailsPluginInfo, "expression")
        then:
        grailsPluginInfo.getName() >> {
            'platform-ui'
        }
        0 * grailsPluginInfo._
        codecSettings == 'none'
        where:
        pluginNameInConfig << ['platform-ui','platformUi']
    }

}
