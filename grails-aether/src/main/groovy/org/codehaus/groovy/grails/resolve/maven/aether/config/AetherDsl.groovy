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

import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.codehaus.plexus.logging.Logger
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.repository.Authentication
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector
import grails.util.Environment

/**
 * Core of the DSL for configuring Aether dependency resolution
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherDsl {
    AetherDependencyManager aetherDependencyManager
    @Delegate DefaultRepositorySystemSession session

    ExclusionDependencySelector exclusionDependencySelector

    AetherDsl(AetherDependencyManager dependencyManager) {
        this.aetherDependencyManager = dependencyManager
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
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.setDelegate(map)

        c.call()

        def id = map.id ?: map.host
        if (map.username && map.password && id) {
            final builder = new AuthenticationBuilder()
            builder.addUsername(map.username.toString()).addPassword( map.password.toString())

            if (map.privateKeyFile && map.passphrase) {
                builder.addPrivateKey(map.privateKeyFile.toString(), map.passphrase.toString())
            }

            Authentication a = builder.build()
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
        aetherDependencyManager.readPom = b
    }
    void cacheDir(File f) {
        aetherDependencyManager.cacheDir = f.canonicalPath
    }

    void cacheDir(String f) {
        aetherDependencyManager.cacheDir = f
    }

    void useOrigin(boolean b) {
        GrailsConsole.getInstance().warn("BuildConfig: Method [useOrigin] not supported by Aether dependency manager")
    }
    /**
     * Configures the checksum policy to either fail or ignore
     *
     * @param enable If enabled fail if checksums are invalid
     */
    void checksums(boolean enable) {
        if (enable) {
            aetherDependencyManager.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
        }
        else {
            aetherDependencyManager.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_IGNORE
        }
    }
    /**
     * Uses an explicit checksum policy.
     *
     * @param checksumPolicy The checksum policy
     * @see RepositoryPolicy
     */
    void checksums(String checksumPolicy) {
        aetherDependencyManager.checksumPolicy = checksumPolicy
    }

    /**
     * Configures the log level to use for Aether
     *
     * @param level The level, either "warn", "error", "info", "debug" or "verbose"
     */
    void log(String level) {
        switch(level) {
            case "warn":
                aetherDependencyManager.loggerManager.threshold = Logger.LEVEL_WARN; break
            case "error":
                aetherDependencyManager.loggerManager.threshold = Logger.LEVEL_ERROR; break
            case "info":
                aetherDependencyManager.loggerManager.threshold = Logger.LEVEL_INFO; break
            case "debug":
                aetherDependencyManager.loggerManager.threshold = Logger.LEVEL_DEBUG; break
            case "verbose":
                aetherDependencyManager.loggerManager.threshold = Logger.LEVEL_DEBUG; break
        }
    }

    /**
     * Whether to inherit dependenices from the framework or not
     *
     * @param name The named dependencies to inherit
     * @param customizer The customizer to use if excluding dependencies from the framework
     */
    void inherits(String name, @DelegatesTo(DependencyConfiguration) Closure customizer = null) {
        final callable = aetherDependencyManager.inheritedDependencies[name]

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

    /**
     * The repositories to configure
     *
     * @param callable A closure that defines the repositories
     */
    void repositories(@DelegatesTo(RepositoriesConfiguration) Closure callable) {
        def rc = new RepositoriesConfiguration(aetherDependencyManager, session)
        callable.delegate = rc
        callable.call()

        this.aetherDependencyManager.repositories.addAll(rc.repositories)
        this.aetherDependencyManager.repositories.unique()
    }

    /**
     * Defines the dependencies of the project
     *
     * @param callable A closure that delegate to {@link DependenciesConfiguration}
     */
    void dependencies(@DelegatesTo(DependenciesConfiguration) Closure callable) {
        def dc = new DependenciesConfiguration(aetherDependencyManager)
        dc.exclusionDependencySelector = exclusionDependencySelector
        callable.delegate = dc
        callable.call()
    }

    /**
     * Defines the managed dependencies of the project. See http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management
     *
     * @param callable A closure that delegate to {@link DependenciesConfiguration}
     */
    void management(@DelegatesTo(DependencyManagementConfiguration) Closure callable) {
        def dc = new DependencyManagementConfiguration(aetherDependencyManager)
        dc.exclusionDependencySelector = exclusionDependencySelector
        callable.delegate = dc
        callable.call()
    }

    /**
     * Defines the plugin dependencies of the project
     *
     * @param callable A closure that delegate to {@link PluginConfiguration}
     */
    void plugins(@DelegatesTo(PluginConfiguration) Closure callable) {
        def dc = new PluginConfiguration(aetherDependencyManager)
        callable.delegate = dc
        callable.call()
    }
}
