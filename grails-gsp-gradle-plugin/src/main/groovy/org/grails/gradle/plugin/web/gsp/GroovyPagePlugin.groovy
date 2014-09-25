package org.grails.gradle.plugin.web.gsp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.IsolatedAntBuilder

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
            gspCompile "org.grails:grails-web-gsp:3.0.0.BUILD-SNAPSHOT"
            gspCompile 'javax.servlet:javax.servlet-api:3.1.0'
        }

        def compileGroovyPages = project.tasks.create("compileGroovyPages") << {
            def antBuilder = project.services.get(IsolatedAntBuilder)

            antBuilder.withClasspath(project.configurations.gspCompile).execute {
                taskdef (name: 'gspc', classname : 'org.grails.web.pages.GroovyPageCompilerTask')
                gspc(destdir:new File(project.buildDir, "classes/main"),
                    srcdir:"${project.projectDir}/grails-app/views",
                    packagename: project.name,
                    serverpath:"/WEB-INF/grails-app/views/",
                    tmpdir: new File(project.buildDir, "gsptemp")) {
                    classpath {
                        pathelement( path: (project.configurations.gspCompile + project.configurations.compile ).asPath)
                    }
                }
            }
        }


        project.tasks.getByName('assemble').dependsOn(compileGroovyPages)
    }
}
