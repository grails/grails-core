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
import org.sonatype.aether.repository.RemoteRepository

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class RepositoriesConfiguration {
    List<RemoteRepository> repositories = []

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
            final repository = new RemoteRepository(url, "default", url)
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
