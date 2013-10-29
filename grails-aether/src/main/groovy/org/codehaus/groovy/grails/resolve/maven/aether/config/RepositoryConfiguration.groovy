/*
 * Copyright 2013 the original author or authors.
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
import org.eclipse.aether.repository.Authentication
import org.eclipse.aether.repository.Proxy
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.util.repository.AuthenticationBuilder

/**
 * Allows configurations a repository's policy
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class RepositoryConfiguration {
    @Delegate RemoteRepository.Builder remoteRepository

    RepositoryPolicy defaultRepositoryPolicy = new RepositoryPolicy()

    RepositoryConfiguration(RemoteRepository.Builder remoteRepository) {
        this.remoteRepository = remoteRepository
    }

    void updatePolicy(String updatePolicy) {
        final newPolicy = new RepositoryPolicy(defaultRepositoryPolicy.enabled, updatePolicy, defaultRepositoryPolicy.checksumPolicy)
        this.defaultRepositoryPolicy = newPolicy
        remoteRepository.setPolicy(newPolicy)
    }

    void checksumPolicy(String checksumPolicy) {
        if (['ignore', 'warn', 'fail'].contains(checksumPolicy)) {
            final newPolicy = new RepositoryPolicy(defaultRepositoryPolicy.enabled, defaultRepositoryPolicy.updatePolicy,checksumPolicy)
            this.defaultRepositoryPolicy = newPolicy
            remoteRepository.setPolicy(newPolicy)
        }
    }

    Authentication authentication(Map<String, String> credentials) {
        auth(credentials)
    }

    Authentication auth(Map<String, String> credentials) {
        Authentication auth
        if (credentials.privateKeyFile && credentials.passphrase) {
            auth = new AuthenticationBuilder().addUsername(credentials.username).addPassword(credentials.password).addPrivateKey(credentials.privateKeyFile, credentials.passphrase).build()
        }
        else {
            auth = new AuthenticationBuilder().addUsername(credentials.username).addPassword(credentials.password).build()
        }

        remoteRepository.setAuthentication(auth)
        return auth
    }

    void proxy(String type, String host, int port, Authentication auth = null) {
        remoteRepository.setProxy(new Proxy(type, host, port, auth))
    }
    void proxy(String host, int port, Authentication auth = null) {
        proxy(null, host, port, auth)
    }
}
