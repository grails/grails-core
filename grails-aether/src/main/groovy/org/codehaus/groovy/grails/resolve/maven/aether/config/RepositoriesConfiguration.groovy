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
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.repository.ArtifactRepository
import org.eclipse.aether.repository.Proxy
import org.eclipse.aether.repository.RemoteRepository
import grails.util.Environment
import grails.build.logging.GrailsConsole
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.eclipse.aether.util.repository.AuthenticationBuilder

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class RepositoriesConfiguration {
    AetherDependencyManager aetherDependencyManager
    @Delegate DefaultRepositorySystemSession session

    RepositoriesConfiguration(AetherDependencyManager dependencyManager, DefaultRepositorySystemSession session) {
        this.aetherDependencyManager = dependencyManager
        this.session = session
    }

    List<RemoteRepository> repositories = []

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

    void inherit(boolean b) {
        inherits(b)
    }
    void inherits(boolean b) {
        // TODO
    }
    void grailsPlugins() {
        // noop.. not supported
    }
    void grailsHome() {
        // noop.. not supported
    }

    void grailsRepo(String location) {
        GrailsConsole.getInstance().warn("grailsRepo() method deprecated. Legacy Grails SVN repositories are not supported by Aether.")
    }

    void mavenLocal() {
        // noop.. enabled by default
    }

    void mavenLocal(String location) {
        aetherDependencyManager.cacheDir = location
    }

    RemoteRepository mavenCentral(Closure configurer = null) {
        final existing = repositories.find { ArtifactRepository ar -> ar.id == "mavenCentral" }
        if (!existing) {
            final repositoryBuilder = new RemoteRepository.Builder("mavenCentral", "default", "http://repo1.maven.org/maven2/")

            configureRepository(repositoryBuilder, configurer)
            final repository = repositoryBuilder.build()
            repositories << repository
            return repository
        }
        else {
            return existing
        }
    }

    RemoteRepository jCenter(Closure configurer = null) {
        final existing = repositories.find { ArtifactRepository ar -> ar.id == "jCenter" }
        if (!existing) {
            final repositoryBuilder = new RemoteRepository.Builder("jCenter", null, "http://jcenter.bintray.com")

            configureRepository(repositoryBuilder, configurer)
            final repository = repositoryBuilder.build()
            repositories << repository
            repository
        } else {
            existing
        }
    }

    protected void configureRepository(RemoteRepository.Builder repositoryBuilder, Closure configurer) {
        final proxyHost = System.getProperty("http.proxyHost")
        final proxyPort = System.getProperty("http.proxyPort")
        if (proxyHost && proxyPort) {
            final proxyUser = System.getProperty("http.proxyUser")
            final proxyPass = System.getProperty("http.proxyPassword")
            if (proxyUser && proxyPass) {
                repositoryBuilder.setProxy(new Proxy(Proxy.TYPE_HTTP, proxyHost, proxyPort.toInteger(),new AuthenticationBuilder().addUsername(proxyUser).addPassword( proxyPass).build()))
            }
            else {
                repositoryBuilder.setProxy(new Proxy(Proxy.TYPE_HTTP, proxyHost, proxyPort.toInteger(),null))
            }

        }

        final auth = session.authenticationSelector.getAuthentication(repositoryBuilder.build())
        if(auth) {
            repositoryBuilder.setAuthentication(auth)
        }
        if (configurer) {
            final rc = new RepositoryConfiguration(repositoryBuilder)
            configurer.setDelegate(rc)
            configurer.call()
        }
    }

    RemoteRepository grailsCentral(Closure configurer = null) {
        final existing = repositories.find { ArtifactRepository ar -> ar.id == "grailsCentral" }
        if (!existing) {
            final repositoryBuilder = new RemoteRepository.Builder("grailsCentral", "default", "http://repo.grails.org/grails/plugins")
            configureRepository(repositoryBuilder, configurer)
            final repository = repositoryBuilder.build()
            repositories << repository
            return repository

        }
        else {
            return reconfigureExisting(existing, configurer)
        }
    }

    RemoteRepository mavenRepo(String url, Closure configurer = null) {
        final existing = repositories.find { ArtifactRepository ar -> ar.id == url }
        if (!existing) {
            final i = url.indexOf('//')
            String name = url
            if(i > -1)
                name = url[i+2..-1]
            name.replaceAll(/[\.\/]/,'-')

            final repositoryBuilder = new RemoteRepository.Builder(name, "default", url)
            configureRepository(repositoryBuilder, configurer)
            final repository = repositoryBuilder.build()
            repositories << repository
            return repository
        }
        else {
            return reconfigureExisting(existing, configurer)
        }
    }

    RemoteRepository mavenRepo(Map<String, String> properties, Closure configurer = null) {
        final url = properties.url
        def id = properties.id ?: properties.name ?: url

        if (id && properties.url) {
            final existing = repositories.find { ArtifactRepository ar -> ar.id == url }
            if (!existing) {
                final repositoryBuilder = new RemoteRepository.Builder(id, "default", url)
                configureRepository(repositoryBuilder, configurer)
                final repository = repositoryBuilder.build()
                repositories << repository
                return repository
            }
            else {
                return reconfigureExisting(existing, configurer)
            }
        }
    }

    protected RemoteRepository reconfigureExisting(RemoteRepository existing, Closure configurer) {
        repositories.remove(existing)
        final repositoryBuilder = new RemoteRepository.Builder(existing)
        configureRepository(repositoryBuilder, configurer)
        final newRepo = repositoryBuilder.build()
        repositories << newRepo
        return newRepo
    }
}
