package org.grails.gradle.plugin.web.gsp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

/**
 * A plugin that adds support for compiling Groovy Server Pages (GSP)
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class GroovyPagePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

        project.configurations {
            gspCompile
        }

        project.dependencies {
            gspCompile 'javax.servlet:javax.servlet-api:3.1.0'
        }

        def mainSourceSet = findMainSourceSet(project)

        def destDir = mainSourceSet?.output?.classesDir ?: new File(project.buildDir, "main/classes")

        project.tasks.create("compileGroovyPages", GroovyPageCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/grails-app/views")
            serverpath = "/WEB-INF/grails-app/views/"
            classpath = project.configurations.compile + project.configurations.gspCompile + project.fileTree(destDir)
        }

        project.tasks.create("compileWebappGroovyPages", GroovyPageCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/web-app")
            serverpath = "/"
            classpath = project.configurations.compile + project.configurations.gspCompile + project.fileTree(destDir)
        }

        project.tasks.create("compileMainTemplatesGroovyPages", GroovyPageCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/src/main/templates")
            serverpath = "/"
            classpath = project.configurations.compile + project.configurations.gspCompile + project.fileTree(destDir)
        }

        def compileGroovyPages = project.tasks.getByName("compileGroovyPages")
        compileGroovyPages.dependsOn( project.tasks.getByName('compileWebappGroovyPages'), project.tasks.getByName('compileMainTemplatesGroovyPages'))
        compileGroovyPages.dependsOn( project.tasks.getByName("compileGroovy") )

        def warTask = project.tasks.findByName('war')
        def assembleTask = project.tasks.findByName('assemble')
        if(warTask) {
            warTask.dependsOn(compileGroovyPages)
        }
        else {
            assembleTask?.dependsOn(compileGroovyPages)
        }
    }

    /**
     * Finds the main SourceSet for the project
     * @param project The project
     * @return The main source set or null if it can't be found
     */
    static SourceSet findMainSourceSet(Project project) {
        JavaPluginConvention plugin = project.getConvention().getPlugin(JavaPluginConvention)
        def sourceSets = plugin?.sourceSets
        return sourceSets?.find { SourceSet sourceSet -> sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME }
    }
}


