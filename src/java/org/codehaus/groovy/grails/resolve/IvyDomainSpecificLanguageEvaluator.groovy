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
package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.module.id.ArtifactId
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor

import org.apache.ivy.util.Message
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.url.CredentialsStore

import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.RepositoryResolver

import org.apache.ivy.plugins.matcher.PatternMatcher
import org.apache.ivy.plugins.matcher.ExactPatternMatcher

import org.apache.ivy.plugins.latest.LatestTimeStrategy

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
		try {
			repositoryMode = true
			repos?.delegate = this
			repos?.call()
		}
		finally {
			repositoryMode = false
		}
    }
	
	void inherit(boolean b) {
		if(repositoryMode) {
			inheritRepositories = b
		}
	}


    void flatDir(Map args) {
        def name = args.name?.toString()
        if (name && args.dirs) {
            def fileSystemResolver = new FileSystemResolver()
            fileSystemResolver.local = true
            fileSystemResolver.name = name

            def dirs = args.dirs instanceof Collection ? args.dirs : [args.dirs]

            repositoryData << ['type':'flatDir', name:name, dirs:dirs.join(',')]
            dirs.each { dir ->
                def path = new File(dir?.toString()).absolutePath
                fileSystemResolver.addIvyPattern( "${path}/[module]-[revision](-[classifier]).xml")
                fileSystemResolver.addArtifactPattern "${path}/[module]-[revision](-[classifier]).[ext]"
            }
            fileSystemResolver.settings = ivySettings

            addToChainResolver(fileSystemResolver)
        }
    }

    private addToChainResolver(org.apache.ivy.plugins.resolver.DependencyResolver resolver) {
		if(currentPluginBeingConfigured && !inheritRepositories) return 
		
		if (transferListener !=null && (resolver instanceof RepositoryResolver)) {
			((RepositoryResolver)resolver).repository.addTransferListener transferListener
		}
		// Fix for GRAILS-5805
		synchronized(chainResolver.resolvers) {
			chainResolver.add resolver
		}
		
    }

    void grailsPlugins() {
        if (isResolverNotAlreadyDefined('grailsPlugins')) {
            repositoryData << [type: 'grailsPlugins', name:"grailsPlugins"]
            if (buildSettings != null) {
                def pluginResolver = new GrailsPluginsDirectoryResolver(buildSettings, ivySettings)
                addToChainResolver(pluginResolver)
            }
        }
    }

    void grailsHome() {
        if (!isResolverNotAlreadyDefined('grailsHome')) {
            return
        }

        def grailsHome = buildSettings?.grailsHome?.absolutePath ?: System.getenv("GRAILS_HOME")
        if (!grailsHome) {
            return
        }

        flatDir(name:"grailsHome", dirs:"${grailsHome}/lib")
        flatDir(name:"grailsHome", dirs:"${grailsHome}/dist")
        if (grailsHome!='.') {
            def resolver = createLocalPluginResolver("grailsHome", grailsHome)
            addToChainResolver(resolver)
        }
    }

    FileSystemResolver createLocalPluginResolver(String name, String location) {
        def pluginResolver = new FileSystemResolver(name: name)
        pluginResolver.addArtifactPattern("${location}/plugins/grails-[artifact]-[revision].[ext]")
        pluginResolver.settings = ivySettings
        pluginResolver.latestStrategy = new LatestTimeStrategy()
        pluginResolver.changingPattern = ".*SNAPSHOT"
        pluginResolver.setCheckmodified(true)
        return pluginResolver
    }

    private boolean isResolverNotAlreadyDefined(String name) {
        def resolver
        // Fix for GRAILS-5805
        synchronized(chainResolver.resolvers) {
            resolver = chainResolver.resolvers.any { it.name == name }
        }
        if (resolver) {
            Message.debug("Dependency resolver $name already defined. Ignoring...")
            return false
        }
        return true
    }

    void mavenRepo(String url) {
        if (isResolverNotAlreadyDefined(url)) {
            repositoryData << ['type':'mavenRepo', root:url, name:url, m2compatbile:true]
            def resolver = new IBiblioResolver(name: url, root: url, m2compatible: true, settings: ivySettings, changingPattern: ".*SNAPSHOT")
            addToChainResolver(resolver)
        }
    }

    void mavenRepo(Map args) {
        if (args && args.name) {
            if (isResolverNotAlreadyDefined(args.name)) {
                repositoryData << ( ['type':'mavenRepo'] + args )
                args.settings = ivySettings
                def resolver = new IBiblioResolver(args)
                addToChainResolver(resolver)
            }
        }
        else {
            Message.warn("A mavenRepo specified doesn't have a name argument. Please specify one!")
        }
    }

    void resolver(org.apache.ivy.plugins.resolver.DependencyResolver resolver) {
        if (resolver) {
            resolver.setSettings(ivySettings)
            addToChainResolver(resolver)
        }
    }

    void ebr() {
        if (isResolverNotAlreadyDefined('ebr')) {
            repositoryData << ['type':'ebr']
            IBiblioResolver ebrReleaseResolver = new IBiblioResolver(name:"ebrRelease",
                                                                     root:"http://repository.springsource.com/maven/bundles/release",
                                                                     m2compatible:true,
                                                                     settings:ivySettings)
            addToChainResolver(ebrReleaseResolver)

            IBiblioResolver ebrExternalResolver = new IBiblioResolver(name:"ebrExternal",
                                                                      root:"http://repository.springsource.com/maven/bundles/external",
                                                                      m2compatible:true,
                                                                      settings:ivySettings)

            addToChainResolver(ebrExternalResolver)
        }
    }

    /**
     * Defines a repository that uses Grails plugin repository format. Grails repositories are
     * SVN repositories that follow a particular convention that is not Maven compatible.
     *
     * Ivy is flexible enough to allow the configuration of a resolver that resolves artifacts
     * against non-Maven repositories
     */
    void grailsRepo(String url, String name=null) {
        if (isResolverNotAlreadyDefined(name ?: url)) {
            repositoryData << ['type':'grailsRepo', url:url]
            def urlResolver = new GrailsRepoResolver(name ?: url, new URL(url) )
            urlResolver.addArtifactPattern("${url}/grails-[artifact]/tags/RELEASE_*/grails-[artifact]-[revision].[ext]")
            urlResolver.settings = ivySettings
            urlResolver.latestStrategy = new LatestTimeStrategy()
            urlResolver.changingPattern = ".*"
            urlResolver.setCheckmodified(true)
            addToChainResolver(urlResolver)
        }
    }

    void grailsCentral() {
        if (isResolverNotAlreadyDefined('grailsCentral')) {
            grailsRepo("http://svn.codehaus.org/grails-plugins", "grailsCentral")
            grailsRepo("http://svn.codehaus.org/grails/trunk/grails-plugins", "grailsCore")
        }
    }

    void mavenCentral() {
        if (isResolverNotAlreadyDefined('mavenCentral')) {
            repositoryData << ['type':'mavenCentral']
            IBiblioResolver mavenResolver = new IBiblioResolver(name:"mavenCentral")
            mavenResolver.m2compatible = true
            mavenResolver.settings = ivySettings
            mavenResolver.changingPattern = ".*SNAPSHOT"
            addToChainResolver(mavenResolver)
        }
    }

    void mavenLocal(String repoPath) {
        if (isResolverNotAlreadyDefined('mavenLocal')) {
            repositoryData << ['type':'mavenLocal']
            FileSystemResolver localMavenResolver = new FileSystemResolver(name:'localMavenResolver')
            localMavenResolver.local = true
            localMavenResolver.m2compatible = true
            localMavenResolver.changingPattern = ".*SNAPSHOT"

            String m2UserDir = "${System.getProperty('user.home')}/.m2"
            String repositoryPath = repoPath

            if (!repositoryPath) {
                repositoryPath = m2UserDir + "/repository"

                File mavenSettingsFile = new File("${m2UserDir}/settings.xml")
                if (mavenSettingsFile.exists()) {
                    def settingsXml = new XmlSlurper().parse(mavenSettingsFile)
                    String localRepository = settingsXml.localRepository.text()
                    
                    if (localRepository.trim()) {
                        repositoryPath = localRepository
                    }
                }
            }

            localMavenResolver.addIvyPattern(
                "${repositoryPath}/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).pom")

            localMavenResolver.addArtifactPattern(
                "${repositoryPath}/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]")

            localMavenResolver.settings = ivySettings
            addToChainResolver(localMavenResolver)
        }
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

                        def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, getBooleanValue(args, 'transitive'), scope)

                        if (!pluginMode) {
                            addDependency mrid
                        }
                        else {
                            def artifact = new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, "zip", "zip", null, null )
                            dependencyDescriptor.addDependencyArtifact(scope, artifact)
                        }
                        dependencyDescriptor.exported = getBooleanValue(args, 'export')
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

                        def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, getBooleanValue(dependency, 'transitive'), scope)
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

                        dependencyDescriptor.exported = getBooleanValue(dependency, 'export')
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

