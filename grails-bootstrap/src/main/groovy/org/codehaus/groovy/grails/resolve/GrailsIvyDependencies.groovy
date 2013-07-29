/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.resolve

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.apache.ivy.core.module.id.ModuleRevisionId
import org.codehaus.groovy.grails.resolve.config.DependencyConfigurationConfigurer
import org.codehaus.groovy.grails.resolve.config.JarDependenciesConfigurer
import org.codehaus.groovy.grails.resolve.config.RepositoriesConfigurer

/**
 * @author Graeme Rocher
 */
@CompileStatic
class GrailsIvyDependencies extends GrailsCoreDependencies {

    GrailsIvyDependencies(String grailsVersion) {
        super(grailsVersion)
    }

    GrailsIvyDependencies(String grailsVersion, String servletVersion, boolean java5compatible = false, boolean isGrailsProject = true) {
        super(grailsVersion, servletVersion, java5compatible, isGrailsProject)
    }

    private void registerDependencies(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId[] dependencies, boolean transitive) {
        for (ModuleRevisionId mrid in dependencies) {
            def descriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false, scope)
            descriptor.inherited = true
            descriptor.transitive = transitive
            dependencyManager.registerDependency scope, descriptor
        }
    }

    private void registerDependencies(IvyDependencyManager dependencyManager, String scope, Collection<Dependency> dependencies, boolean transitive) {
        for (Dependency d : dependencies) {
            EnhancedDefaultDependencyDescriptor dd = registerDependency(dependencyManager, scope, ModuleRevisionId.newInstance(d.getGroup(), d.getName(), d.getVersion()), d.getExcludeArray())
            dd.setTransitive(transitive)
        }
    }

    private static void registerDependencies(IvyDependencyManager dependencyManager, String scope, Collection<Dependency> dependencies) {
        for (Dependency d : dependencies) {
            registerDependency(dependencyManager, scope, ModuleRevisionId.newInstance(d.getGroup(), d.getName(), d.getVersion()), d.getExcludeArray())
        }
    }

    private void registerDependencies(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId[] dependencies, String... excludes) {
        for (ModuleRevisionId mrid : dependencies) {
            registerDependency(dependencyManager, scope, mrid, excludes)
        }
    }

    private static EnhancedDefaultDependencyDescriptor registerDependency(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId mrid, String... excludes) {
        EnhancedDefaultDependencyDescriptor descriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, true, scope)
        descriptor.setInherited(true)
        if (excludes != null) {
            for (String exclude : excludes) {
                descriptor.exclude(exclude)
            }
        }
        dependencyManager.registerDependency(scope, descriptor)
        return descriptor
    }

    /**
     * Returns a closure suitable for passing to a DependencyDefinitionParser that will configure
     * the necessary core dependencies for Grails.
     *
     * This method is used internally and should not be called in user code.
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Closure createDeclaration() {
        return  {
            def rootDelegate = (DependencyConfigurationConfigurer)getDelegate()

            rootDelegate.log "warn"

            // Repositories
            rootDelegate.repositories  {
                def repositoriesDelegate = (RepositoriesConfigurer)getDelegate()

                repositoriesDelegate.grailsPlugins()
                repositoriesDelegate.grailsHome()
            }
            // Dependencies

            rootDelegate.dependencies{
                JarDependenciesConfigurer dependenciesDelegate = (JarDependenciesConfigurer)getDelegate()
                def dependencyManager = dependenciesDelegate.getDependencyManager()

                boolean defaultDependenciesProvided = dependencyManager.getDefaultDependenciesProvided()
                String compileTimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "compile"
                String runtimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "runtime"

                // dependencies needed by the Grails build system
                GrailsIvyDependencies.registerDependencies(dependencyManager, "build", buildDependencies)

                // dependencies needed when creating docs
                GrailsIvyDependencies.registerDependencies(dependencyManager, "docs", docDependencies)

                // dependencies needed during development, but not for deployment
                GrailsIvyDependencies.registerDependencies(dependencyManager, "provided", providedDependencies)

                // dependencies needed at compile time
                GrailsIvyDependencies.registerDependencies(dependencyManager, compileTimeDependenciesMethod, compileDependencies)

                // dependencies needed for running tests
                GrailsIvyDependencies.registerDependencies(dependencyManager, "test", testDependencies)

                // dependencies needed at runtime only
                GrailsIvyDependencies.registerDependencies(dependencyManager, runtimeDependenciesMethod, runtimeDependencies)
            }
        }
    }
}
