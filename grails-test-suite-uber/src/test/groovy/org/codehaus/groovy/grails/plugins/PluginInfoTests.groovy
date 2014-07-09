package org.codehaus.groovy.grails.plugins

import groovy.util.slurpersupport.GPathResult
import grails.util.PluginBuildSettings
import org.grails.io.support.Resource
import org.grails.build.plugins.XmlDescriptorPluginInfo

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class PluginInfoTests extends GroovyTestCase {

    void testGetBasicPluginInfo() {
        def pluginInfo = new MockPluginInfo(null, null)

        def metadata = new XmlSlurper().parseText('''
<plugin name='plug1' version='0.1'>
</plugin>
''')
        pluginInfo.metadata = metadata

        assertEquals "plug1", pluginInfo.name
        assertEquals "0.1", pluginInfo.version
        assert !pluginInfo.type

        pluginInfo.type = "binary"

        assertEquals "binary", pluginInfo.type
    }
}

class MockPluginInfo extends XmlDescriptorPluginInfo {

    MockPluginInfo(Resource pluginDir, PluginBuildSettings pluginBuildSettings) {
        super(pluginDir, pluginBuildSettings)
    }

    GPathResult parseMetadata(Resource pluginDir) { null }
}
