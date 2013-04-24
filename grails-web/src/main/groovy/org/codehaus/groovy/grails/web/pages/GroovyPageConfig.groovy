package org.codehaus.groovy.grails.web.pages

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.web.util.WithCodecHelper

@CompileStatic
class GroovyPageConfig {
    Map flatConfig
    
    GroovyPageConfig(Map flatConfig) {
        this.flatConfig = flatConfig
    }

    public String getCodecSettings(GrailsPluginInfo pluginInfo, String codecWriterName) {
        String gspCodecsPrefix = "${pluginInfo ? pluginInfo.name + '.' : ''}${GroovyPageParser.CONFIG_PROPERTY_GSP_CODECS}"
        Map codecSettings = (Map)flatConfig.get(gspCodecsPrefix)
        String codecInfo = null
        if(!codecSettings) {
            if(codecWriterName==WithCodecHelper.EXPRESSION_CODEC_NAME) {
                codecInfo = codecSettings.get(WithCodecHelper.EXPRESSION_CODEC_NAME_ALIAS)?.toString()
                if(!codecInfo) {
                    // legacy fallback
                    codecInfo = flatConfig.get(GroovyPageParser.CONFIG_PROPERTY_DEFAULT_CODEC)?.toString()
                }
            }
        } else {
            codecInfo = codecSettings.get(codecWriterName)?.toString()
        }
        codecInfo
    }
}
