/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.config

import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.apache.ivy.util.url.CredentialsStore

class DependencyConfigurationConfigurer extends AbstractDependencyManagementConfigurer {

    static final String WILDCARD = '*'

    boolean pluginMode = false
    boolean repositoryMode = false

    DependencyConfigurationConfigurer(DependencyConfigurationContext context) {
        super(context)
    }

    void checksums(String checksumConfig) {
        dependencyManager.ivySettings.setVariable("ivy.checksums", checksumConfig)
    }

    void checksums(boolean enable) {
        if (!enable) {
            dependencyManager.ivySettings.setVariable("ivy.checksums", "")
        }
    }

    void useOrigin(boolean b) {
        dependencyManager.ivySettings.setDefaultUseOrigin(b)
    }

    void credentials(Closure c) {
        def creds = [:]
        c.delegate = creds
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()

        if (creds) {
            CredentialsStore.INSTANCE.addCredentials(creds.realm ?: null, creds.host ?: 'localhost', creds.username ?: '', creds.password ?: '')
        }
    }

    void pom(boolean b) {
        dependencyManager.readPom = b
    }

    void defaultDependenciesProvided(boolean b) {
        dependencyManager.defaultDependenciesProvided = b
    }

    void inherits(String name, Closure configurer) {
        // plugins can't configure inheritance
        if (context.pluginName) return

        if (configurer) {
            configurer.delegate = new InheritanceConfigurer(context)
            configurer.resolveStrategy = Closure.DELEGATE_FIRST
            configurer.call()
        }

        def config = dependencyManager.buildSettings?.config?.grails
        if (config) {
            def dependencies = config[name]?.dependency?.resolution
            if (dependencies instanceof Closure) {
                // Create a new configurer with an 'inherited' context
                dependencies.delegate = new DependencyConfigurationConfigurer(context.createInheritedContext())
                dependencies.resolveStrategy = Closure.DELEGATE_FIRST
                dependencies.call()
                dependencyManager.moduleExcludes.clear()
            }
        }
    }

    void inherits(String name) {
        inherits name, null
    }

    void plugins(Closure callable) {
        callable.delegate = new PluginDependenciesConfigurer(context)
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call()
    }

    void log(String level) {
        // plugins can't configure log
        if (context.pluginName) return

        switch(level) {
            case "warn":    dependencyManager.setLogger(new DefaultMessageLogger(Message.MSG_WARN)); break
            case "error":   dependencyManager.setLogger(new DefaultMessageLogger(Message.MSG_ERR)); break
            case "info":    dependencyManager.setLogger(new DefaultMessageLogger(Message.MSG_INFO)); break
            case "debug":   dependencyManager.setLogger(new DefaultMessageLogger(Message.MSG_DEBUG)); break
            case "verbose": dependencyManager.setLogger(new DefaultMessageLogger(Message.MSG_VERBOSE)); break
            default:        dependencyManager.setLogger(new DefaultMessageLogger(Message.MSG_WARN))
        }
        Message.setDefaultLogger dependencyManager.logger
    }

    /**
     * Defines dependency resolvers
     */
    void resolvers(Closure resolvers) {
        repositories resolvers
    }

    /**
     * Same as #resolvers(Closure)
     */
    void repositories(Closure repos) {
        repos.delegate = new RepositoriesConfigurer(context)
        repos.resolveStrategy = Closure.DELEGATE_FIRST
        repos()
    }

    void dependencies(Closure deps) {
        if (deps && !dependencyManager.pluginsOnly) {
            deps.delegate = new JarDependenciesConfigurer(context)
            deps.resolveStrategy = Closure.DELEGATE_FIRST
            deps.call()
        }
    }
}
