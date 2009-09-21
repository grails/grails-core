package grails.util
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class PluginBuildSettingsTests extends GroovyTestCase{

    void testGetPluginSourceFiles() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()
        def sourceFiles = pluginSettings.getPluginSourceFiles()

    }

    void testGetMetadataForPlugin() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()

        def xml = pluginSettings.getMetadataForPlugin("hibernate")

        assertNotNull "should have returned xml",xml

        assertEquals "hibernate", xml.@name.text()
        // exercise cache
        assertNotNull "cache xml was null, shoudln't have been", pluginSettings.getMetadataForPlugin("hibernate")
    }
    void testGetPluginDirForName() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()

        assertNotNull "should have found plugin dir",pluginSettings.getPluginDirForName("hibernate")
        assertNotNull "should have found plugin dir",pluginSettings.getPluginDirForName("hibernate")

    }

    void testGetPluginLibDirs() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()

        assertEquals 2, pluginSettings.getPluginLibDirectories().size()
        assertEquals 2, pluginSettings.getPluginLibDirectories().size()

    }

    void testGetPluginDescriptors() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()
        assertEquals 2, pluginSettings.getPluginDescriptors().size()
        assertEquals 2, pluginSettings.getPluginDescriptors().size()
    }

    void testGetAllArtefactResources() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()

        // called twice to exercise caching
        assertEquals 6, pluginSettings.getArtefactResources().size()
        assertEquals 6, pluginSettings.getArtefactResources().size()
    }

    void testGetAvailableScripts() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()
        def scripts = pluginSettings.getAvailableScripts()

        assertEquals 40, scripts.size()
    }

    void testGetPluginScripts() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()
        def scripts = pluginSettings.getPluginScripts()
        assertEquals 6, scripts.size()
    }

    void testGetPluginXmlMetadata() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()

        // called twice to exercise caching
        assertEquals 2, pluginSettings.getPluginXmlMetadata().size()
        assertEquals 2, pluginSettings.getPluginXmlMetadata().size()
    }

    void testGetPluginInfos() {
        PluginBuildSettings pluginSettings = createPluginBuildSettings()


        def pluginInfos = pluginSettings.getPluginInfos()

        assertEquals 2, pluginInfos.size()

        assertNotNull "should contain hibernate", pluginInfos.find { it.name == 'hibernate' }
        assertNotNull "should contain webflow", pluginInfos.find { it.name == 'webflow' }
    }

    PluginBuildSettings createPluginBuildSettings() {
        def settings = new BuildSettings(new File("."), new File("./test/test-projects/plugin-build-settings"))
        settings.loadConfig()
        def pluginSettings = new PluginBuildSettings(settings)
        return pluginSettings
    }

    void testGetPluginDirectories() {

        PluginBuildSettings pluginSettings = createPluginBuildSettings()

        def pluginDirs = pluginSettings.getPluginDirectories()

        println pluginDirs

        assertEquals 2, pluginDirs.size()

        assertNotNull "hibernate plugin should be there",pluginDirs.find { it.filename.contains('hibernate') }
        assertNotNull "webflow plugin should be there",pluginDirs.find { it.filename.contains('webflow') }
    }

}