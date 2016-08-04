package org.grails.gradle.plugin.commands

import org.gradle.api.tasks.JavaExec

/**
 * Created by Jim on 8/2/2016.
 */
class RuntimeApplicationContextCommandTask extends JavaExec {

    RuntimeApplicationContextCommandTask() {
        setMain("grails.ui.command.GrailsRuntimeApplicationContextCommandRunner")
        dependsOn("classes", "findMainClass")
        systemProperties(System.properties.findAll { it.key.toString().startsWith('grails.') } as Map<String, Object>)
    }
}
