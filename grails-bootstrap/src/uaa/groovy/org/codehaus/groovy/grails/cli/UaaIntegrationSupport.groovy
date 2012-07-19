package org.codehaus.groovy.grails.cli
/**
 *  Groovy XML support class for UaaIntegration.
 */
class UaaIntegrationSupport {

    /**
     * Finds a plugin in the given plugin list.
     *
     * @param pluginList The plugin list
     * @param pluginName The plugin name
     * @return The plugin node
     */
    static findPlugin(pluginList, String pluginName) {
        if (pluginList == null) {
            return null
        }

        def p = pluginList.plugin.find { plugin -> plugin.@name?.text() == pluginName }
        if (p) {
            return p
        }

    }
}
