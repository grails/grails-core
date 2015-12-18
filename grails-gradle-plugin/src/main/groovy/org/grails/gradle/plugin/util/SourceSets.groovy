package org.grails.gradle.plugin.util

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class SourceSets {

    /**
     * Finds the main SourceSet for the project
     * @param project The project
     * @return The main source set or null if it can't be found
     */
    static SourceSet findMainSourceSet(Project project) {
        SourceSetContainer sourceSets = findSourceSets(project)
        return sourceSets?.find { SourceSet sourceSet -> sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME }
    }

    static SourceSetContainer findSourceSets(Project project) {
        JavaPluginConvention plugin = project.getConvention().getPlugin(JavaPluginConvention)
        def sourceSets = plugin?.sourceSets
        sourceSets
    }
}
