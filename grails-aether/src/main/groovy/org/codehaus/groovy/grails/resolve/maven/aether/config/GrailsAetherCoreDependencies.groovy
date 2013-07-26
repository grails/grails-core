/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.resolve.maven.aether.config

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.GrailsCoreDependencies
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector

/**
 * Adapts Grails' dependencies to Aether dependencies
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class GrailsAetherCoreDependencies extends GrailsCoreDependencies {

    GrailsAetherCoreDependencies(String grailsVersion) {
        super(grailsVersion)
    }

    GrailsAetherCoreDependencies(String grailsVersion, String servletVersion, boolean java5compatible = false, boolean isGrailsProject = true) {
        super(grailsVersion, servletVersion, java5compatible, isGrailsProject)
    }

    ExclusionDependencySelector exclusionDependencySelector

    /**
     * Returns a closure suitable for passing to a DependencyDefinitionParser that will configure
     * the necessary core dependencies for Grails.
     *
     * This method is used internally and should not be called in user code.
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Closure createDeclaration() {
        return  {

            AetherDsl dsl = getDelegate()

            // if the grails version ends in snapshot we need an extra repository in order for Grails to function. This is only used for development versions of Grails
            if (grailsVersion.endsWith("-SNAPSHOT")) {
                dsl.repositories {
                    RepositoriesConfiguration repositoryConfiguration = (RepositoriesConfiguration)getDelegate()
                    repositoryConfiguration.mavenRepo("http://repo.grails.org/grails/core")
                }
            }

            dsl.dependencies{
                DependenciesConfiguration dependenciesDelegate = getDelegate()
                def dependencyManager = dependenciesDelegate.getDependencyManager()

                boolean defaultDependenciesProvided = dependencyManager.getDefaultDependenciesProvided()
                String compileTimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "compile"
                String runtimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "runtime"

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
        for (Dependency d in dependencies) {
            if (scope == 'build') {
                configuration.addBuildDependency(d)
            }
            else {
                configuration.addDependency(d, scope)
            }
        }
    }
}
