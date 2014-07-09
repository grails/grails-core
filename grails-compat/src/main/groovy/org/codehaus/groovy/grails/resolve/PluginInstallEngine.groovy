package org.codehaus.groovy.grails.resolve

import grails.util.BuildSettings
import grails.util.Metadata
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic

/**
 * @deprecated Use {@link org.grails.dependency.resolution.PluginInstallEngine} instead
 * @author Graeme Rocher
 */
@CompileStatic
@Deprecated
class PluginInstallEngine extends org.grails.dependency.resolution.PluginInstallEngine {
    PluginInstallEngine(BuildSettings settings, PluginBuildSettings pbs, Metadata md, AntBuilder ant) {
        super(settings, pbs, md, ant)
    }

    PluginInstallEngine(BuildSettings settings, PluginBuildSettings pbs, Metadata md) {
        super(settings, pbs, md)
    }

    PluginInstallEngine(BuildSettings settings, PluginBuildSettings pbs) {
        super(settings, pbs)
    }

    PluginInstallEngine(BuildSettings settings) {
        super(settings)
    }
}
