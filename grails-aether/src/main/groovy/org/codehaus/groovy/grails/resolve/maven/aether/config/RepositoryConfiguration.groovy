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

import groovy.transform.CompileStatic
import org.sonatype.aether.repository.ArtifactRepository
import org.sonatype.aether.repository.RemoteRepository

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class RepositoryConfiguration {
    List<RemoteRepository> repositories = []

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
    void mavenCentral() {
        if (! repositories.find{ ArtifactRepository ar -> ar.id == "mavenCentral"} )
            repositories << new RemoteRepository( "mavenCentral", "default", "http://repo1.maven.org/maven2/" );
    }

    void grailsCentral() {
        if (! repositories.find{ ArtifactRepository ar -> ar.id == "grailsCentral"} )
            repositories << new RemoteRepository( "grailsCentral", "default", "http://repo.grails.org/grails/plugins" );
    }

    void mavenRepo(String url) {
        if (! repositories.find{ ArtifactRepository ar -> ar.id == url} )
            repositories << new RemoteRepository( url, "default", url);
    }
}
