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

import org.sonatype.aether.repository.ArtifactRepository
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.repository.Proxy
import org.sonatype.aether.repository.RemoteRepository
import grails.util.Environment

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class RepositoriesConfiguration {
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
    void mavenLocal() {
        // noop.. enabled by default
    }
    RemoteRepository mavenCentral(Closure configurer = null) {
        final existing = repositories.find { ArtifactRepository ar -> ar.id == "mavenCentral" }
        if (!existing) {
            final repository = new RemoteRepository("mavenCentral", "default", "http://repo1.maven.org/maven2/")
            configureRepository(repository, configurer)
            repositories << repository
            return repository
        }
        else {
            return existing
        }
    }

    protected void configureRepository(RemoteRepository repository, Closure configurer) {
        final proxyHost = System.getProperty("http.proxyHost")
        final proxyPort = System.getProperty("http.proxyPort")
        if (proxyHost && proxyPort) {
            final proxyUser = System.getProperty("http.proxyUserName")
            final proxyPass = System.getProperty("http.proxyPassword")
            if (proxyUser && proxyPass) {
                repository.setProxy(new Proxy(Proxy.TYPE_HTTP, proxyHost, proxyPort.toInteger(),new Authentication(proxyUser, proxyPass)))
            }
            else {
                repository.setProxy(new Proxy(Proxy.TYPE_HTTP, proxyHost, proxyPort.toInteger(),null))
            }

        }
        if (configurer) {
            final rc = new RepositoryConfiguration(repository)
            configurer.setDelegate(rc)
            configurer.call()
        }
    }

    RemoteRepository grailsCentral(Closure configurer = null) {
        final existing = repositories.find { ArtifactRepository ar -> ar.id == "grailsCentral" }
        if (!existing) {
            final repository = new RemoteRepository("grailsCentral", "default", "http://repo.grails.org/grails/plugins")
            configureRepository(repository, configurer)
            repositories << repository
            return repository
        }
        else {
            configureRepository(existing, configurer)
            return existing
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

            final repository = new RemoteRepository(name, "default", url)
            configureRepository(repository, configurer)
            repositories << repository
            return repository
        }
        else {
            configureRepository(existing, configurer)
            return existing
        }
    }

    RemoteRepository mavenRepo(Map<String, String> properties, Closure configurer = null) {
        final url = properties.url
        def id = properties.id ?: properties.name ?: url

        if (id && properties.url) {
            final existing = repositories.find { ArtifactRepository ar -> ar.id == url }
            if (!existing) {
                final repository = new RemoteRepository(id, "default", url)
                configureRepository(repository, configurer)
                repositories << repository
                return repository
            }
            else {
                configureRepository(existing, configurer)
                return existing
            }
        }
    }
}
