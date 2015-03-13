package org.grails.gradle.plugin.core

import org.apache.tools.ant.taskdefs.condition.Os

/**
 * A extension to the Gradle plugin to configure Grails settings
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class GrailsExtension {

    /**
     * Whether to invoke native2ascii on resource bundles
     */
    boolean native2ascii = !Os.isFamily(Os.FAMILY_WINDOWS)

    /**
     * Whether assets should be packaged in META-INF/assets for plugins
     */
    boolean packageAssets = true
}
