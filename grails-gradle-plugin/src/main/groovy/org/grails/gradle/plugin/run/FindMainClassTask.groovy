package org.grails.gradle.plugin.run

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.grails.gradle.plugin.util.SourceSets
import org.springframework.boot.gradle.SpringBootPluginExtension

/**
 * A task that finds the main task, differs slightly from Boot's version as expects a subclass of GrailsConfiguration
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class FindMainClassTask extends DefaultTask {

    @TaskAction
    void setMainClassProperty() {
        Project project = this.project
        if ( !project.property("mainClassName") ) {
            project.setProperty "mainClassName", findMainClass()
        }
    }

    protected String findMainClass() {
        Project project = this.project

        // Try the SpringBoot extension setting
        def bootExtension = project.extensions.findByType( SpringBootPluginExtension )
        if(bootExtension?.mainClass) {
            return bootExtension.mainClass
        }

        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)

        if(!mainSourceSet) return null

        MainClassFinder mainClassFinder = createMainClassFinder()
        return mainClassFinder.findMainClass(mainSourceSet.output.classesDir)
    }

    protected MainClassFinder createMainClassFinder() {
        new MainClassFinder()
    }
}
