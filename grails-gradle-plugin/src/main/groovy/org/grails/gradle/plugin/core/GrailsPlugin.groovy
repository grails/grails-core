package org.grails.gradle.plugin.core

import grails.util.BuildSettings
import grails.util.Environment
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.grails.gradle.plugin.agent.AgentTasksEnhancer
import org.grails.gradle.plugin.watch.GrailsWatchPlugin
import org.grails.gradle.plugin.watch.WatchConfig
import org.springframework.boot.gradle.SpringBootPlugin

class GrailsPlugin extends GroovyPlugin {



    void apply(Project project) {
        super.apply(project)

        project.getPlugins().apply(SpringBootPlugin)


        def projectDir = project.projectDir

        def grailsSourceDirs = []
        def excludedDirs = ['views', 'migrations', 'assets', 'i18n']
        new File("$projectDir/grails-app").eachDir { File subdir ->
            def dirName = subdir.name
            if(!subdir.hidden && !dirName.startsWith(".") && !excludedDirs.contains(dirName)) {
                grailsSourceDirs << subdir.absolutePath
            }
        }

        grailsSourceDirs << "$projectDir/src/main/groovy"

        System.setProperty( BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
        def environment = Environment.current


        if(environment.isReloadEnabled()) {
            new GrailsWatchPlugin().apply(project)
            NamedDomainObjectContainer<WatchConfig> watchConfigs = project.extensions.getByName('watch')
            def watchConfig = watchConfigs.create("groovy")
            watchConfig.directory = project.file("grails-app")
            watchConfig.extensions = ['groovy', 'java']
            watchConfig.tasks('compileGroovy')

            project.configurations {
                agent
            }
            project.dependencies {
                agent "org.springframework:springloaded:1.2.0.RELEASE"
            }
            project.afterEvaluate(new AgentTasksEnhancer())
        }


        project.sourceSets {
            main {
                groovy {
                    srcDirs = grailsSourceDirs
                    filter {
                        exclude "$projectDir/grails-app/conf/hibernate"
                        exclude "$projectDir/grails-app/conf/spring"
                    }
                    resources {
                        srcDirs = [
                                "$projectDir/grails-app/conf/hibernate",
                                "$projectDir/grails-app/conf/spring",
                                "$projectDir/grails-app/views",
                                "$projectDir/src/main/webapp"
                        ]
                    }
                }
            }
        }
    }

}
