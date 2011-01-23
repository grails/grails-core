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
package org.codehaus.groovy.grails.resolve.dsl

import grails.util.DslUtils

import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.module.id.ArtifactId
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor

import org.apache.ivy.util.Message
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.url.CredentialsStore

import org.apache.ivy.plugins.matcher.PatternMatcher
import org.apache.ivy.plugins.matcher.ExactPatternMatcher

import org.codehaus.groovy.grails.resolve.IvyDependencyManager

import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor

class IvyDomainSpecificLanguageEvaluator {

    static final String WILDCARD = '*'

    boolean inherited = false
    boolean pluginMode = false
	boolean repositoryMode = false
	
    String currentPluginBeingConfigured = null
    @Delegate IvyDependencyManager delegate

    IvyDomainSpecificLanguageEvaluator(IvyDependencyManager delegate, String currentPluginBeingConfigured = null) {
        this.delegate = delegate
        this.currentPluginBeingConfigured = currentPluginBeingConfigured
    }

    void useOrigin(boolean b) {
        ivySettings.setDefaultUseOrigin(b)
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
        delegate.readPom = b
    }

    void excludes(Map exclude) {
        def anyExpression = PatternMatcher.ANY_EXPRESSION
        def mid = ModuleId.newInstance(exclude.group ?: anyExpression, exclude.name.toString())
        def aid = new ArtifactId(mid, anyExpression, anyExpression, anyExpression)

        def excludeRule = new DefaultExcludeRule(aid, ExactPatternMatcher.INSTANCE, null)

        for (String conf in configurationNames) {
            excludeRule.addConfiguration conf
        }

        if (currentDependencyDescriptor == null) {
            moduleDescriptor.addExcludeRule(excludeRule)
        }
        else {
            for (String conf in configurationNames) {
                currentDependencyDescriptor.addExcludeRule(conf, excludeRule)
            }
        }
    }

    void excludes(String... excludeList) {
        for (exclude in excludeList) {
            excludes name:exclude
        }
    }

    void defaultDependenciesProvided(boolean b) {
        delegate.defaultDependenciesProvided = b
    }

    void inherits(String name, Closure configurer) {
        // plugins can't configure inheritance
        if (currentPluginBeingConfigured) return

        configurer?.delegate = this
        configurer?.call()

        def config = buildSettings?.config?.grails
        if (config) {
            def dependencies = config[name]?.dependency?.resolution
            if (dependencies instanceof Closure) {
                try {
                    inherited = true
                    dependencies.delegate = this
                    dependencies.call()
                    moduleExcludes.clear()
                }
                finally {
                    inherited = false
                }
            }
        }
    }

    void inherits(String name) {
        inherits name, null
    }
	

    void plugins(Closure callable) {
        try {
            pluginMode = true
            callable.call()
        }
        finally {
            pluginMode = false
        }
    }

    void plugin(String name, Closure callable) {
        configuredPlugins << name

        try {
            currentPluginBeingConfigured = name
            callable?.delegate = this
            callable?.call()
        }
        finally {
            currentPluginBeingConfigured = null
        }
    }

    void log(String level) {
        // plugins can't configure log
        if (currentPluginBeingConfigured) return

        switch(level) {
            case "warn":    setLogger(new DefaultMessageLogger(Message.MSG_WARN)); break
            case "error":   setLogger(new DefaultMessageLogger(Message.MSG_ERR)); break
            case "info":    setLogger(new DefaultMessageLogger(Message.MSG_INFO)); break
            case "debug":   setLogger(new DefaultMessageLogger(Message.MSG_DEBUG)); break
            case "verbose": setLogger(new DefaultMessageLogger(Message.MSG_VERBOSE)); break
            default:        setLogger(new DefaultMessageLogger(Message.MSG_WARN))
        }
        Message.setDefaultLogger logger
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
        repos.delegate = new RepositoriesEvaluator(delegate, currentPluginBeingConfigured)
        repos()
    }

    void dependencies(Closure deps) {
		if(pluginsOnly) return
        deps?.delegate = this
        deps?.call()
    }

    def invokeMethod(String name, args) {
        if (!args || !((args[0] instanceof CharSequence)||(args[0] instanceof Map)||(args[0] instanceof Collection))) {
            println "WARNING: Configurational method [$name] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring.."
        }
        else {
            def dependencies = args
            def callable
            if (dependencies && (dependencies[-1] instanceof Closure)) {
                callable = dependencies[-1]
                dependencies = dependencies[0..-2]
            }

            if (dependencies) {
                parseDependenciesInternal(dependencies, name, callable)
            }
        }
    }

    private parseDependenciesInternal(dependencies, String scope, Closure dependencyConfigurer) {

        boolean usedArgs = false
        

        

        def parseDep = { dependency ->
            if ((dependency instanceof CharSequence)) {
                def args = [:]
                if (dependencies[-1] instanceof Map) {
                    args = dependencies[-1]
                    usedArgs = true
                }
                def depDefinition = dependency.toString()

                def m = depDefinition =~ /([a-zA-Z0-9\-\/\._+=]*?):([a-zA-Z0-9\-\/\._+=]+?):([a-zA-Z0-9\-\/\._+=]+)/

                if (m.matches()) {

                    String name = m[0][2]
                    boolean isExcluded = currentPluginBeingConfigured ? isExcludedFromPlugin(currentPluginBeingConfigured, name) : isExcluded(name)
                    if (!isExcluded) {
                        def group = m[0][1]
                        def version = m[0][3]
                        if (pluginMode) {
                            group = group ?: 'org.grails.plugins'
                        }

                        def mrid = ModuleRevisionId.newInstance(group, name, version)

                        def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, DslUtils.getBooleanValueOrDefault(args, 'transitive', true), scope)

                        if (!pluginMode) {
                            addDependency mrid
                        }
                        else {
                            def artifact = new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, "zip", "zip", null, null )
                            dependencyDescriptor.addDependencyArtifact(scope, artifact)
                        }
                        dependencyDescriptor.exported = DslUtils.getBooleanValueOrDefault(args, 'export', true)
                        dependencyDescriptor.inherited = inherited || inheritsAll || currentPluginBeingConfigured

                        if (currentPluginBeingConfigured) {
                            dependencyDescriptor.plugin = currentPluginBeingConfigured
                        }
                        configureDependencyDescriptor(dependencyDescriptor, scope, dependencyConfigurer, pluginMode)
                    }
                }
                else {
                    println "WARNING: Specified dependency definition ${scope}(${depDefinition.inspect()}) is invalid! Skipping.."
                }
            }
            else if (dependency instanceof Map) {
                def name = dependency.name
				def group = dependency.group
				def version = dependency.version
				
                if (!group && pluginMode) group = "org.grails.plugins"

                if (group && name && version) {
                    boolean isExcluded = currentPluginBeingConfigured ? isExcludedFromPlugin(currentPluginBeingConfigured, name) : isExcluded(name)
                    if (!isExcluded) {
                        def attrs = [:]
                        if(dependency.classifier) {
                            attrs["m:classifier"] = dependency.classifier
                        }

                        def mrid
                        if (dependency.branch) {
                            mrid = ModuleRevisionId.newInstance(group, name, dependency.branch, version, attrs)
                        }
                        else {
                            mrid = ModuleRevisionId.newInstance(group, name, version, attrs)
                        }

                        def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, DslUtils.getBooleanValueOrDefault(dependency, 'transitive', true), scope)
                        if (!pluginMode) {
                            addDependency mrid
                        }
                        else {
                            def artifact
                            if (dependency.classifier == 'plugin') {
                                artifact = new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, "xml", "xml", null, null )
                            }
                            else {
                                artifact = new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, "zip", "zip", null, null )
                            }

                            dependencyDescriptor.addDependencyArtifact(scope, artifact)
                        }

                        dependencyDescriptor.exported = DslUtils.getBooleanValueOrDefault(dependency, 'export', true)
                        dependencyDescriptor.inherited = inherited || inheritsAll
                        if (currentPluginBeingConfigured) {
                            dependencyDescriptor.plugin = currentPluginBeingConfigured
                        }

                        configureDependencyDescriptor(dependencyDescriptor, scope, dependencyConfigurer, pluginMode)
                    }
                }
            }
        }

		
		for(dep in dependencies ) {
			if((dependencies[-1] == dep) && usedArgs) return 
			parseDep(dep) 
		}
    }
}

