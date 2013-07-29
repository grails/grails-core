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

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic

import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.codehaus.plexus.logging.Logger
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.Exclusion
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.repository.RepositoryPolicy
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector
import org.sonatype.aether.util.repository.DefaultAuthenticationSelector
import grails.util.Environment

/**
 * Core of the DSL for configuring Aether dependency resolution
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherDsl {
    AetherDependencyManager dependencyManager
    @Delegate MavenRepositorySystemSession session

    ExclusionDependencySelector exclusionDependencySelector

    AetherDsl(AetherDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    /**
     * Environment support
     *
     * @param callable The callable
     * @return The result of the environments block
     */
    def environments(Closure callable) {
        final environmentCallable = Environment.getEnvironmentSpecificBlock(callable)
        if(environmentCallable) {
            environmentCallable.setDelegate(this)
            environmentCallable.call()
        }
    }

    /**
     * Sets up authentication for a repository
     *
     * @param c The credentials
     * @return The Authentication instance
     */
    Authentication credentials(Closure c) {
        def map = [:]
        c.setDelegate(map)

        c.call()

        def id = map.id ?: map.host
        if (map.username && map.password && id) {
            def a = new Authentication(map.username.toString(), map.password.toString())

            if (map.privateKeyFile) {
                a = a.setPrivateKeyFile(map.privateKeyFile.toString())
            }
            if (map.passphrase) {
                a = a.setPassphrase(map.passphrase.toString())
            }

            final selector = session.authenticationSelector
            if (selector instanceof DefaultAuthenticationSelector) {
                selector.add(id.toString(), a)
            }
            return a
        }
        return null
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
        switch(level) {
            case "warn":
                dependencyManager.loggerManager.threshold = Logger.LEVEL_WARN; break
            case "error":
                dependencyManager.loggerManager.threshold = Logger.LEVEL_ERROR; break
            case "info":
                dependencyManager.loggerManager.threshold = Logger.LEVEL_INFO; break
            case "debug":
                dependencyManager.loggerManager.threshold = Logger.LEVEL_DEBUG; break
            case "verbose":
                dependencyManager.loggerManager.threshold = Logger.LEVEL_DEBUG; break
        }
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
        def rc = new RepositoriesConfiguration()
        callable.delegate = rc
        callable.call()

        this.dependencyManager.repositories.addAll(rc.repositories)
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
