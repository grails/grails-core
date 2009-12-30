package org.codehaus.groovy.grails.plugins

import groovy.util.slurpersupport.GPathResult
import org.springframework.core.io.Resource
import grails.util.PluginBuildSettings

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class PluginInfoTests extends GroovyTestCase{

    void testGetBasicPluginInfo() {
        def pluginInfo = new MockPluginInfo(null, null)

        def metadata = new XmlSlurper().parseText('''
<plugin name='plug1' version='0.1'>
</plugin>
''')
        pluginInfo.metadata = metadata

        assertEquals "plug1", pluginInfo.name
        assertEquals "0.1", pluginInfo.version
    }

}
class MockPluginInfo extends PluginInfo {

    public MockPluginInfo(Resource pluginDir, PluginBuildSettings pluginBuildSettings) {
        super(pluginDir, pluginBuildSettings);    
    }


    public GPathResult parseMetadata(Resource pluginDir) {
        null
    }


}
