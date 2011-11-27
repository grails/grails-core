package org.grails.gradle.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.DefaultTask
import org.gradle.api.specs.Spec
import org.gradle.api.specs.Specs

/**
Downloads artifact source jars to a specified directory.  A List of specifications must be applied along with a 
destination directory.  Optionally, a filter may be supplied to limit which source jars are downloaded.

To download all source jars into the 'sourceJars' dir:

<pre>
    task downloadSourceJars(type: org.grails.gradle.tasks.DownloadSources) {
       configurations = [project.configurations.compile]
       into(file('sourceJars'))
    }
    
</pre>

To only download source jars for dependencies that have the word 'datastore' in their module name:

<pre>
    task downloadSourceJars(type: org.grails.gradle.tasks.DownloadSources) {
       configurations = [project.configurations.compile]
       // The argument to the filter closure will be an instance of org.gradle.api.artifacts.ResolvedDependency
       into(file('sourceJars')) { dependency ->
           dependency.moduleName.contains('datastore')
       }
    }
    
</pre>
*/
class DownloadSources extends DefaultTask {
    
    @InputFiles
    List<Configuration> configurations
    
    @OutputDirectory
    File into
    
    private Closure dependencyFilter
    
    @TaskAction
    def download() {
        def dependencies = new LinkedHashSet()
        for (configuration in configurations) {
            getAllDeps(configuration.resolvedConfiguration.getFirstLevelModuleDependencies({ it instanceof ExternalDependency } as Spec), dependencies)
        }
        
        def sourceDependencies = dependencies.collect { ResolvedDependency resolvedDependency ->
            def dependency = new DefaultExternalModuleDependency(resolvedDependency.moduleGroup, resolvedDependency.moduleName, resolvedDependency.moduleVersion,
                    resolvedDependency.configuration)
            dependency.transitive = false
            dependency.artifact { artifact ->
                artifact.name = dependency.name
                artifact.type = 'source'
                artifact.extension = 'jar'
                artifact.classifier = 'sources'
            }
            dependency
        }
        
        def detached = project.configurations.detachedConfiguration(sourceDependencies as Dependency[])
        
        project.copy {
            from detached.resolvedConfiguration.getFiles(Specs.satisfyAll())
            into this.into
        }
    }
    
    Set getAllDeps(Collection deps, Set allDeps = new LinkedHashSet()) {
        deps.each { ResolvedDependency resolvedDependency ->
            def notSeenBefore = allDeps.add(resolvedDependency)
            if (notSeenBefore) { // defend against circular dependencies
                getAllDeps(resolvedDependency.children, allDeps)
            }
        }
        allDeps
    }
    
}