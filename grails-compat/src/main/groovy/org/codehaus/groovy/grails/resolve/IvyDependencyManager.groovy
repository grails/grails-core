package org.codehaus.groovy.grails.resolve

import grails.util.BuildSettings
import grails.util.Metadata
import groovy.transform.CompileStatic
import org.apache.ivy.core.settings.IvySettings

/**
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.dependency.resolution.ivy.IvyDependencyManager} instead
 */
@CompileStatic
@Deprecated
class IvyDependencyManager extends org.grails.dependency.resolution.ivy.IvyDependencyManager {
    IvyDependencyManager(String applicationName, String applicationVersion, BuildSettings settings, Metadata metadata, IvySettings ivySettings) {
        super(applicationName, applicationVersion, settings, metadata, ivySettings)
    }

    IvyDependencyManager(String applicationName, String applicationVersion, BuildSettings settings, Metadata metadata) {
        super(applicationName, applicationVersion, settings, metadata)
    }

    IvyDependencyManager(String applicationName, String applicationVersion, BuildSettings settings) {
        super(applicationName, applicationVersion, settings)
    }

    IvyDependencyManager(String applicationName, String applicationVersion) {
        super(applicationName, applicationVersion)
    }
}
