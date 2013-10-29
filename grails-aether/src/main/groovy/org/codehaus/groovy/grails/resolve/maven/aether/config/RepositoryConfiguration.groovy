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
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.repository.Proxy
import org.sonatype.aether.repository.RemoteRepository

/**
 * Allows configurations a repository's policy
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class RepositoryConfiguration {
    @Delegate RemoteRepository remoteRepository

    RepositoryConfiguration(RemoteRepository remoteRepository) {
        this.remoteRepository = remoteRepository
    }

    void updatePolicy(String updatePolicy) {
        final policy = remoteRepository.getPolicy(true)

        remoteRepository.setPolicy(true, policy.setUpdatePolicy(updatePolicy))
    }

    void checksumPolicy(String checksumPolicy) {
        final policy = remoteRepository.getPolicy(true)

        if (['ignore', 'warn', 'fail'].contains(checksumPolicy)) {
            remoteRepository.setPolicy(true, policy.setChecksumPolicy(checksumPolicy))
        }
    }

    Authentication authentication(Map<String, String> credentials) {
        auth(credentials)
    }

    Authentication auth(Map<String, String> credentials) {
        Authentication auth
        if (credentials.privateKeyFile) {
            auth = new Authentication(credentials.username, credentials.password, credentials.privateKeyFile, credentials.passphrase)
        }
        else {
            auth = new Authentication(credentials.username, credentials.password)
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
