package org.grails.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
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
