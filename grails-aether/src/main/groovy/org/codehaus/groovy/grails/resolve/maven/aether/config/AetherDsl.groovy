/* Copyright 2012 the original author or authors.
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

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.Exclusion
import org.sonatype.aether.repository.RepositoryPolicy
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector

/**
 * Core of the DSL for configuring Aether dependency resolution
 *
 * @author Graeme Rocher
 */
@CompileStatic
class AetherDsl {
    AetherDependencyManager dependencyManager
    @Delegate MavenRepositorySystemSession session


    ExclusionDependencySelector exclusionDependencySelector

    AetherDsl(AetherDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    @Deprecated
    void legacyResolve(boolean b) {}

    void pom(boolean b) {
        dependencyManager.readPom = b
    }
    void cacheDir(File f) {
        dependencyManager.cacheDir = f.canonicalPath
    }

    void cacheDir(String f) {
        dependencyManager.cacheDir = f
    }

    void useOrigin(boolean b) {
        GrailsConsole.getInstance().warn("BuildConfig: Method [useOrigin] not supported by Aether dependency manager")
    }
    void checksums(boolean enable) {
        if (enable) {
            dependencyManager.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
        }
        else {
            dependencyManager.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_IGNORE
        }
    }
    void checksums(String checksumConfig) {
        dependencyManager.checksumPolicy = checksumConfig
    }

    void log(String level) {
        // TODO: Handle logging activation
//        switch(level) {
//            case "warn":
//            case "error":
//            case "info":
//            case "debug":
//            case "verbose":
//            default:
//        }
    }

    void inherits(String name, Closure customizer = null) {
        final callable = dependencyManager.inheritedDependencies[name]

        if (callable) {

            if (customizer != null) {
                def tmp = new Dependency(new DefaultArtifact("$name:$name:1.0"), "compile")
                def dc = new DependencyConfiguration(tmp)
                customizer.setDelegate(dc)
                customizer.call()
                tmp = dc.dependency

                if (tmp.exclusions) {
                     exclusionDependencySelector = new ExclusionDependencySelector(new HashSet<Exclusion>(tmp.exclusions))
                }

            }
            try {
                callable.delegate = this
                callable.call()
            } finally {
                exclusionDependencySelector = null
            }
        }
    }
    void repositories(Closure callable) {
        def rc = new RepositoryConfiguration()
        callable.delegate = rc
        callable.call()

        this.dependencyManager.repositories = rc.repositories
    }

    void dependencies(Closure callable) {
        def dc = new DependenciesConfiguration(dependencyManager)
        dc.exclusionDependencySelector = exclusionDependencySelector
        callable.delegate = dc
        callable.call()
    }

    void plugins(Closure callable) {
        def dc = new PluginConfiguration(dependencyManager)
        callable.delegate = dc
        callable.call()
    }
}