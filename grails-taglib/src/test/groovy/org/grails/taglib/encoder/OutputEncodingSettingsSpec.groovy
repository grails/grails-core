package org.grails.taglib.encoder

import grails.plugins.GrailsPluginInfo
import org.grails.config.PropertySourcesConfig
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll;

class OutputEncodingSettingsSpec extends Specification {

    @Issue("GRAILS-11331")
    @Unroll
    def "per plugin settings should be supported - #pluginNameInConfig"() {
        given:
        def flatConfig = [("${pluginNameInConfig}.grails.views.gsp.codecs.expression".toString()): 'none']
        def outputEncodingSettings = new OutputEncodingSettings(new PropertySourcesConfig(flatConfig))
        def grailsPluginInfo = Mock(GrailsPluginInfo)
        when:
        def codecSettings=outputEncodingSettings.getCodecSettings(grailsPluginInfo, "expression")
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
