package org.codehaus.groovy.grails.plugins
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class PluginInfoTests extends GroovyTestCase{

    void testGetBasicPluginInfo() {
        def pluginInfo = new PluginInfo(null)

        def metadata = new XmlSlurper().parseText('''
<plugin name='plug1' version='0.1'>
</plugin>
''')
        pluginInfo.metadata = metadata

        assertEquals "plug1", pluginInfo.name
        assertEquals "0.1", pluginInfo.version
    }

}
