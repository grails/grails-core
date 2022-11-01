package org.grails.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.specs.Spec

class GrailsBuildPlugin implements Plugin<Project> {

    void apply(Project project) {

        // Add utility for getting sources, returns a configuration containing the source jar versions
        // of the dependencies in the given configuration(s)
        project.ext {
            sourcesFor = { configurations -> classifiedDependencies(project, configurations, "sources") }
            pomFor = { configurations -> classifiedDependencies(project, configurations, "pom") }
            javadocFor = { configurations -> classifiedDependencies(project, configurations, "javadoc") }
        }
    }

    private Configuration classifiedDependencies(project, configurations, String targetClassifier) {
        // We can't use varargs for the closure signature due to an issue in the mixin mechanism in this
        // version of Gradle.
        if (configurations instanceof Configuration) {
            configurations = [configurations]
        }

        def addChildren
        addChildren = { Collection deps, Set allDeps = new LinkedHashSet() ->
            deps.each { ResolvedDependency resolvedDependency ->
                def notSeenBefore = allDeps.add(resolvedDependency)
                if (notSeenBefore) { // defend against circular dependencies
                    addChildren(resolvedDependency.children, allDeps)
                }
            }
            allDeps
        }

        def dependencies = new LinkedHashSet()
        for (configuration in configurations) {
            addChildren(configuration.resolvedConfiguration.getFirstLevelModuleDependencies({ it instanceof ExternalDependency } as Spec), dependencies)
        }

        def sourceDependencies = dependencies.collect { ResolvedDependency resolvedDependency ->
            def dependency = new DefaultExternalModuleDependency(resolvedDependency.moduleGroup, resolvedDependency.moduleName, resolvedDependency.moduleVersion)
            dependency.setTransitive(false)
            dependency.artifact { artifact ->
                artifact.name = dependency.name
                artifact.type = targetClassifier
                if('pom' == targetClassifier) {
                    artifact.extension = 'pom'
                }
                else {
                    artifact.extension = 'jar'
                    artifact.classifier = targetClassifier
                }
            }
            dependency
        }

        project.configurations.detachedConfiguration(sourceDependencies as Dependency[])
                .with(true) {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, (Usage) project.objects.named(Usage, Usage.JAVA_RUNTIME))
                }
    }

}
