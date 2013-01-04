package org.codehaus.groovy.grails.resolve.maven.aether.config

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.GrailsCoreDependencies
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.sonatype.aether.graph.Exclusion
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector

/**
 * @author Graeme Rocher
 */
@CompileStatic
class GrailsAetherCoreDependencies extends GrailsCoreDependencies{
    GrailsAetherCoreDependencies(String grailsVersion) {
        super(grailsVersion)
    }

    GrailsAetherCoreDependencies(String grailsVersion, String servletVersion) {
        super(grailsVersion, servletVersion)
    }

    GrailsAetherCoreDependencies(String grailsVersion, String servletVersion, boolean java5compatible) {
        super(grailsVersion, servletVersion, java5compatible)
    }

    ExclusionDependencySelector exclusionDependencySelector

    /**
     * Returns a closure suitable for passing to a DependencyDefinitionParser that will configure
     * the necessary core dependencies for Grails.
     *
     * This method is used internally and should not be called in user code.
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    public Closure createDeclaration() {
        return  {

            AetherDsl dsl = (AetherDsl)getDelegate()

            dsl.dependencies{
                DependenciesConfiguration dependenciesDelegate = (DependenciesConfiguration)getDelegate();
                def dependencyManager = dependenciesDelegate.getDependencyManager()

                boolean defaultDependenciesProvided = dependencyManager.getDefaultDependenciesProvided()
                String compileTimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "compile"
                String runtimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "runtime";

                // dependencies needed by the Grails build system
                registerDependencies(dependenciesDelegate, "build", buildDependencies)

                // dependencies needed when creating docs
                registerDependencies(dependenciesDelegate, "docs", docDependencies)

                // dependencies needed during development, but not for deployment
                registerDependencies(dependenciesDelegate, "provided", providedDependencies)

                // dependencies needed at compile time
                registerDependencies(dependenciesDelegate, compileTimeDependenciesMethod, compileDependencies)

                // dependencies needed for running tests
                registerDependencies(dependenciesDelegate, "test", testDependencies)

                // dependencies needed at runtime only

                registerDependencies(dependenciesDelegate, runtimeDependenciesMethod, runtimeDependencies)
            }

        }
    }

    void registerDependencies(DependenciesConfiguration configuration, String scope, Collection<Dependency> dependencies) {
        for(org.codehaus.groovy.grails.resolve.Dependency d in dependencies) {
            if (scope == 'build') {
                configuration.addBuildDependency(d)
            }
            else {

                configuration.addDependency(d, scope)
            }
        }
    }
}
