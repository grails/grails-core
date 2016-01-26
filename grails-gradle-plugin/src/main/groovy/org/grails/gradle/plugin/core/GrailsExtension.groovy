package org.grails.gradle.plugin.core

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.util.ConfigureUtil

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
     * Whether to use Ant to do the conversion
     */
    boolean native2asciiAnt = false

    /**
     * Whether assets should be packaged in META-INF/assets for plugins
     */
    boolean packageAssets = true

    /**
     * Configure the reloading agent
     */
    Agent agent = new Agent()

    /**
     * Configure the reloading agent
     */
    Agent agent(Closure configurer) {
        ConfigureUtil.configure(configurer, agent)
    }
    /**
     * Configuration for the reloading agent
     */
    static class Agent {
        boolean enabled = true
        File path
        String inclusions = "grails.plugins..*"
        String exclusions
        Boolean logging
        boolean synchronize = true
        boolean allowSplitPackages = true
        File cacheDir = new File("build/springloaded")

        Map<String, String> systemProperties = ['jdk.reflect.allowGetCallerClass': 'true']
        List<String> jvmArgs = ['-Xverify:none']
    }
}
