package org.grails.gradle.plugin.commands

import groovy.transform.CompileStatic
import org.gradle.api.tasks.JavaExec

@CompileStatic
class ApplicationContextScriptTask extends JavaExec {

    ApplicationContextScriptTask() {
        setMain("grails.ui.script.GrailsApplicationScriptRunner")
        dependsOn("classes", "findMainClass")
        systemProperties(System.properties.findAll { it.key.toString().startsWith('grails.') } as Map<String, Object>)
    }

}