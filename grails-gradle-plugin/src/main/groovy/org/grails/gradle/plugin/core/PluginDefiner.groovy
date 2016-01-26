package org.grails.gradle.plugin.core

import grails.util.Environment
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * Makes it easier to define Grails plugins and also makes them aware of the development environment so that they can be run inline without creating a JAR
 *
 * @author Graeme Rocher
 * @since 3.2
 */
@PackageScope
class PluginDefiner {
    Project project

    PluginDefiner(Project project) {
        this.project = project
    }

    void methodMissing(String name, args) {
        Object[] argArray = (Object[])args

        if(!argArray) {
            throw new MissingMethodException(name, GrailsExtension, args)
        }
        else {
            if(argArray[0] instanceof Map) {
                Map notation = (Map)argArray[0]
                if(!notation.containsKey('group')) {
                    notation.put('group','org.grails.plugins')
                }
            }
            else if(argArray[0] instanceof CharSequence) {
                String str = argArray[0].toString()

                if(str.startsWith(':')) {
                    argArray[0] = "org.grails.plugins$str".toString()
                }
            }
            project.dependencies.add(name, *argArray )
        }
    }

    @CompileStatic
    Dependency project(String path) {
        if(Environment.isDevelopmentRun()) {
            project.dependencies.project(path:path, configuration:'exploded')
        }
        else {
            project.dependencies.project(path:path)
        }
    }
}