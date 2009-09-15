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

import grails.util.BuildSettingsHolder
import java.util.concurrent.ConcurrentHashMap
import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.module.descriptor.Configuration
import org.apache.ivy.core.module.descriptor.Configuration.Visibility
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.core.resolve.ResolveEngine
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.sort.SortEngine
import org.apache.ivy.plugins.resolver.ChainResolver
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.codehaus.groovy.grails.resolve.DependencyDefinitionParser
import org.codehaus.groovy.grails.resolve.DependencyResolver
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor
import grails.util.BuildSettings
import org.apache.ivy.core.module.descriptor.ExcludeRule
import grails.util.GrailsNameUtils

/**
 * Implementation that uses Apache Ivy under the hood
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class IvyDependencyManager implements DependencyResolver, DependencyDefinitionParser{

    /*
    * Out of the box Ivy configurations are:
    *
    * - build: Dependencies for the build system only
    * - compile: Dependencies for the compile step
    * - runtime: Dependencies needed at runtime but not for compilation (see above)
    * - test: Dependencies needed for testing but not at runtime (see above)
    * - provided: Dependencies needed at development time, but not during WAR deployment
    */
    static Configuration BUILD_CONFIGURATION  = new Configuration("build",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Build system dependencies",
                                                                ['default'] as String[],
                                                                true, null)

    static Configuration COMPILE_CONFIGURATION = new Configuration("compile",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Compile time dependencies",
                                                                ['default'] as String[],
                                                                true, null)

    static Configuration RUNTIME_CONFIGURATION = new Configuration("runtime",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Runtime time dependencies",
                                                                ['compile'] as String[],
                                                                true, null)

    static Configuration TEST_CONFIGURATION = new Configuration("test",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Testing dependencies",
                                                                ['runtime'] as String[],
                                                                true, null)

    static Configuration PROVIDED_CONFIGURATION = new Configuration("provided",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Dependencies provided by the container",
                                                                ['default'] as String[],
                                                                true, null)

    static List<Configuration> ALL_CONFIGURATIONS = [BUILD_CONFIGURATION, COMPILE_CONFIGURATION, RUNTIME_CONFIGURATION, TEST_CONFIGURATION, PROVIDED_CONFIGURATION]


    private Set<ModuleRevisionId> dependencies = [] as Set
    private Set<DependencyDescriptor> dependencyDescriptors = [] as Set
    private orgToDepMap = [:]
    private hasApplicationDependencies = false

    Map configurationMappings = [ runtime:['runtime(*)','master(*)'],
                                  build:['default'],
                                  compile:['compile(*)', 'master(*)'],
                                  provided:['compile(*)', 'master(*)'],
                                  test:['runtime(*)', 'master(*)']]


    ResolveEngine resolveEngine
    BuildSettings buildSettings
    String applicationName
    String applicationVersion
    IvySettings ivySettings
    ChainResolver chainResolver = new ChainResolver(name:"default",returnFirst:true)
    DefaultModuleDescriptor moduleDescriptor
    List repositoryData = []
    Set<String> configuredPlugins = [] as Set
    Set<String> usedConfigurations = [] as Set

    private static managers = new ConcurrentHashMap()
    private static currentManager

    /**
     * Obtain an Ivy dependency manager instance for the given application name and version
     */
    static IvyDependencyManager getInstance(String applicationName, String applicationVersion, BuildSettings settings = null) {
        if(!applicationName) throw new IllegalArgumentException("Cannot supply a null application name to Ivy dependency manager")
        if(!applicationVersion) throw new IllegalArgumentException("Cannot supply a null application version to Ivy dependency manager")

        def cacheKey = [version:applicationVersion, name:applicationName]
        def manager = managers[cacheKey]
        if(!manager) {
            manager = new IvyDependencyManager(applicationName, applicationVersion, settings)
            managers[cacheKey] = manager
        }
        currentManager = manager
        return manager
    }



    /**
     * Get the current (last one obtained via getInstance) ivy dependency manager 
     */
    static IvyDependencyManager getCurrent() { currentManager }

    /**
     * Creates a new IvyDependencyManager instance
     */
    IvyDependencyManager(String applicationName, String applicationVersion, BuildSettings settings=null) {
        ivySettings = new IvySettings()
        ivySettings.defaultInit()
        chainResolver.settings = ivySettings
        def eventManager = new EventManager()
        def sortEngine = new SortEngine(ivySettings)
        resolveEngine = new ResolveEngine(ivySettings,eventManager,sortEngine)
        resolveEngine.dictatorResolver = chainResolver

        this.applicationName = applicationName
        this.applicationVersion = applicationVersion
        this.buildSettings = settings
    }

    /**
     * Returns true if the application has any dependencies that are not inherited
     * from the framework or other plugins
     */
    boolean hasApplicationDependencies() { this.hasApplicationDependencies }
    /**
     * Serializes the parsed dependencies using the given builder.
     *
     * @param builder A builder such as groovy.xml.MarkupBuilder
     */
    void serialize(builder, boolean createRoot = true) {

        if(createRoot) {
            builder.dependencies {
                serializeResolvers(builder)
                serializeDependencies(builder)
            }
        }
        else {
            serializeResolvers(builder)
            serializeDependencies(builder)
        }
    }

    private serializeResolvers(builder) {
        builder.resolvers {
            for(resolverData in repositoryData) {
                if(resolverData.name=='grailsHome') continue

                builder.resolver resolverData
            }
        }
    }

    private serializeDependencies(builder) {
        for (EnhancedDefaultDependencyDescriptor dd in dependencyDescriptors) {
            // dependencies inherited by Grails' global config are not included
            if(dd.inherited) continue
            
            def mrid = dd.dependencyRevisionId
            builder.dependency( group: mrid.organisation, name: mrid.name, version: mrid.revision, conf: dd.scope, transitive: dd.transitive ) {
                for(ExcludeRule er in dd.allExcludeRules) {
                   def mid = er.id.moduleId
                   excludes group:mid.organisation,name:mid.name
                }
            }
        }
    }

    /**
     * Obtains the default dependency definitions for the given Grails version
     */
    static Closure getDefaultDependencies(String grailsVersion) {
        return {
            // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
            log "warn"
            repositories {
                grailsHome()

                // uncomment the below to enable remote dependency resolution
                // from public Maven repositories
                //mavenCentral()
                //mavenRepo "http://snapshots.repository.codehaus.org"
                //mavenRepo "http://repository.codehaus.org"
                //mavenRepo "http://download.java.net/maven/2/"
                //mavenRepo "http://repository.jboss.com/maven2/
            }
            dependencies {

                // dependencies needed by the Grails build system
                 build "org.gparallelizer:GParallelizer:0.8.3",
                       "org.tmatesoft.svnkit:svnkit:1.2.0",
                       "org.apache.ant:ant:1.7.1",
                       "org.apache.ant:ant-launcher:1.7.1",
                       "org.apache.ant:ant-junit:1.7.1",
                       "org.apache.ant:ant-nodeps:1.7.1",
                       "org.apache.ant:ant-trax:1.7.1",
                       "radeox:radeox:1.0-b2",
                       "apache-tomcat:jasper-compiler:5.5.15",
                       "jline:jline:0.9.91",
                       "xalan:serializer:2.7.1",
                       "org.grails:grails-bootstrap:$grailsVersion",
                       "org.grails:grails-scripts:$grailsVersion",
                       "org.grails:grails-core:$grailsVersion",
                       "org.grails:grails-resources:$grailsVersion",
                       "org.grails:grails-web:$grailsVersion",
                       "org.slf4j:slf4j-api:1.5.6",
                       "org.slf4j:slf4j-log4j12:1.5.6",
                       "org.springframework:org.springframework.test:3.0.0.M4"

                // dependencies needed during development, but not for deployment
                provided "javax.servlet:servlet-api:2.5",
                         "javax.servlet:jsp-api:2.1",
                         "javax.servlet:jstl:1.1.2"

                // dependencies needed for compilation
                compile("org.codehaus.groovy:groovy-all:1.6.4") {
                    excludes 'jline'
                }

                compile("commons-beanutils:commons-beanutils:1.8.0", "commons-el:commons-el:1.0", "commons-validator:commons-validator:1.3.1") {
                    excludes "commons-logging", "xml-apis"
                }

                compile( "aopalliance:aopalliance:1.0",
                         "commons-collections:commons-collections:3.2.1",
                         "commons-io:commons-io:1.4",
                         "commons-lang:commons-lang:2.4",
                         "javax.transaction:jta:1.1",
                         "opensymphony:sitemesh:2.4",
                         "org.grails:grails-bootstrap:$grailsVersion",
                         "org.grails:grails-core:$grailsVersion",
                         "org.grails:grails-crud:$grailsVersion",
                         "org.grails:grails-docs:$grailsVersion",
                         "org.grails:grails-gorm:$grailsVersion",
                         "org.grails:grails-resources:$grailsVersion",
                         "org.grails:grails-spring:$grailsVersion",
                         "org.grails:grails-web:$grailsVersion",
                         "org.springframework:org.springframework.core:3.0.0.M4",
                         "org.springframework:org.springframework.aop:3.0.0.M4",
                         "org.springframework:org.springframework.aspects:3.0.0.M4",
                         "org.springframework:org.springframework.asm:3.0.0.M4",
                         "org.springframework:org.springframework.beans:3.0.0.M4",
                         "org.springframework:org.springframework.context:3.0.0.M4",
                         "org.springframework:org.springframework.context.support:3.0.0.M4",
                         "org.springframework:org.springframework.expression:3.0.0.M4",
                         "org.springframework:org.springframework.instrument:3.0.0.M4",
                         "org.springframework:org.springframework.instrument.classloading:3.0.0.M4",
                         "org.springframework:org.springframework.jdbc:3.0.0.M4",
                         "org.springframework:org.springframework.jms:3.0.0.M4",
                         "org.springframework:org.springframework.orm:3.0.0.M4",
                         "org.springframework:org.springframework.oxm:3.0.0.M4",
                         "org.springframework:org.springframework.transaction:3.0.0.M4",
                         "org.springframework:org.springframework.web:3.0.0.M4",
                         "org.springframework:org.springframework.web.servlet:3.0.0.M4",
                         "org.slf4j:slf4j-api:1.5.6") {
                        transitive = false
                }


                // dependencies needed for running tests
                test "junit:junit:3.8.2",
                     "org.grails:grails-test:$grailsVersion",
                     "org.springframework:org.springframework.integration-tests:3.0.0.M4",
                     "org.springframework:org.springframework.test:3.0.0.M4"

                // dependencies needed at runtime only
                runtime "aspectj:aspectjweaver:1.6.2",
                        "aspectj:aspectjrt:1.6.2",
                        "cglib:cglib-nodep:2.1_3",
                        "commons-fileupload:commons-fileupload:1.2.1",
                        "oro:oro:2.0.8"

                // data source
                runtime "commons-dbcp:commons-dbcp:1.2.2",
                        "commons-pool:commons-pool:1.5.2",
                        "hsqldb:hsqldb:1.8.0.10"

                // caching
                runtime ("net.sf.ehcache:ehcache:1.6.1",
                         "opensymphony:oscache:2.4.1") {
                    excludes 'jms', 'commons-logging', 'servlet-api'
                }

                // logging
                runtime  "log4j:log4j:1.2.15",
                         "org.slf4j:jcl-over-slf4j:1.5.6",
                         "org.slf4j:jul-to-slf4j:1.5.6",

                         "org.slf4j:slf4j-log4j12:1.5.6"

                // JSP support
                runtime "apache-taglibs:standard:1.1.2",
                        "xpp3:xpp3_min:1.1.3.4.O"

            }
      }

    }
    /**
     * Obtains a list of dependencies defined in the project
     */
    Set<ModuleRevisionId> getDependencies() { dependencies }



    /**
    * Obtains a list of dependency descriptors defined in the project
     */
    Set<DependencyDescriptor> getDependencyDescriptors() { dependencyDescriptors }



    /**
     * Adds a dependency to the project
     */
    void addDependency(ModuleRevisionId revisionId) {
        dependencies << revisionId
        if(orgToDepMap[revisionId.organisation]) {
            orgToDepMap[revisionId.organisation] << revisionId
        }
        else {
            orgToDepMap[revisionId.organisation] = [revisionId] as Set
        }

    }

    /**
     * Adds a dependency descriptor to the project
     */
    void addDependencyDescriptor(DependencyDescriptor dd) {
        if(dd) {
            dependencyDescriptors << dd
            addDependency(dd.dependencyRevisionId)
        }
    }



    /**
     * For usages such as addPluginDependency("foo", [group:"junit", name:"junit", version:"3.8.2"])
     *
     * This method is designed to be used by the internal framework and plugins and not be end users.
     * The idea is that plugins can provide dependencies at runtime which are then inherited by
     * the user's dependency configuration
     *
     * A user can however override a plugin's dependencies inside the dependency resolution DSL
     */
    void addPluginDependency(String pluginName, Map args) {
        // do nothing if the dependencies of the plugin are configured by the application
        if(isPluginConfiguredByApplication(pluginName)) return
        if(args?.group && args?.name && args?.version) {
             def transitive = !!args.transitive
             def scope = args.conf ?: 'runtime'
             def mrid = ModuleRevisionId.newInstance(args.group, args.name, args.version)
             def dd = new EnhancedDefaultDependencyDescriptor(mrid, true, transitive, scope)
             dd.inherited=true
             dd.plugin = pluginName
             configureDependencyDescriptor(dd, scope)
             if(args.excludes) {
                 for(ex in excludes) {
                     dd.exclude(ex)
                 }                 
             }
             addDependencyDescriptor dd
        }
    }

    boolean isPluginConfiguredByApplication(String name) {
        (configuredPlugins.contains(name) || configuredPlugins.contains(GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name)))        
    }

    def configureDependencyDescriptor(EnhancedDefaultDependencyDescriptor dependencyDescriptor, String scope, Closure dependencyConfigurer=null) {
        if(!usedConfigurations.contains(scope)) {
            usedConfigurations << scope
        }

        def mappings = configurationMappings[scope]
        mappings?.each {
            dependencyDescriptor.addDependencyConfiguration scope, it
        }

        if (dependencyConfigurer) {
            dependencyConfigurer.resolveStrategy = Closure.DELEGATE_ONLY
            dependencyConfigurer.setDelegate(dependencyDescriptor)
            dependencyConfigurer.call()
        }
        if(!dependencyDescriptor.inherited) {
            hasApplicationDependencies = true
        }
        dependencyDescriptors << dependencyDescriptor
        moduleDescriptor.addDependency dependencyDescriptor
    }


    Set<ModuleRevisionId> getModuleRevisionIds(String org) { orgToDepMap[org] }

    /**
     * Lists all known dependencies for the given configuration name (defaults to all dependencies)
     */
    IvyNode[] listDependencies(String conf = null) {
        def options = new ResolveOptions()
        if(conf)
            options.confs = [conf] as String[]


        resolveEngine.getDependencies(moduleDescriptor, options, new ResolveReport(moduleDescriptor))
    }


    public ResolveReport resolveDependencies(Configuration conf) {
        resolveDependencies(conf.name)
    }

    public ResolveReport resolveDependencies() {
        resolveDependencies('')
    }
    public ResolveReport resolveDependencies(String conf) {
        if(usedConfigurations.contains(conf)) {
            def options = new ResolveOptions(checkIfChanged:false, outputReport:true, validate:false)
            if(conf)
                options.confs = [conf] as String[]


            return resolveEngine.resolve(moduleDescriptor,options)
        }
        else {
            // return an empty resolve report
            return new ResolveReport(moduleDescriptor)
        }
    }

    /**
     * Parses the Ivy DSL definition
     */
    void parseDependencies(Closure definition) {
        if(definition && applicationName && applicationVersion) {
            if(this.moduleDescriptor == null) {                
                this.moduleDescriptor =
                    DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(applicationName, applicationName, applicationVersion))

                // TODO: make configurations extensible
                this.moduleDescriptor.addConfiguration BUILD_CONFIGURATION
                this.moduleDescriptor.addConfiguration COMPILE_CONFIGURATION
                this.moduleDescriptor.addConfiguration RUNTIME_CONFIGURATION
                this.moduleDescriptor.addConfiguration TEST_CONFIGURATION
                this.moduleDescriptor.addConfiguration PROVIDED_CONFIGURATION
            }

            def evaluator = new IvyDomainSpecificLanguageEvaluator(this)
            definition.delegate = evaluator
            definition.resolveStrategy = Closure.DELEGATE_FIRST
            definition()

        }
    }


    /**
     * Parses dependencies of a plugin
     *
     * @param pluginName the name of the plugin
     * @param definition the Ivy DSL definition
     */
    void parseDependencies(String pluginName,Closure definition) {
        if(!isPluginConfiguredByApplication(pluginName) && definition) {
            if(moduleDescriptor == null) throw new IllegalStateException("Call parseDependencies(Closure) first to parse the application dependencies")

            def evaluator = new IvyDomainSpecificLanguageEvaluator(this)
            evaluator.plugin = pluginName
            definition.delegate = evaluator
            definition.resolveStrategy = Closure.DELEGATE_FIRST
            definition()
        }
    }



}
class IvyDomainSpecificLanguageEvaluator {

    static final String WILDCARD = '*'

    boolean inherited = false
    String plugin = null
    @Delegate IvyDependencyManager delegate
    Set moduleExcludes = [] as Set

    IvyDomainSpecificLanguageEvaluator(IvyDependencyManager delegate) {
        this.delegate = delegate
    }

    void excludes(String... excludes) {
        for(name in excludes ) {            
            moduleExcludes << name
        }
    }

    void inherits(String name, Closure configurer) {
        // plugins can't configure inheritance
        if(plugin) return

        configurer?.delegate=this
        configurer?.call()

        def config = buildSettings?.config?.grails
        if(config) {
            def dependencies = config[name]?.dependency?.resolution
            if(dependencies instanceof Closure) {
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

    void plugin(String name, Closure callable) {
        configuredPlugins << name

        try {
            plugin = name
            callable?.delegate = this
            callable?.call()
        }
        finally {
            plugin = null
        }

    }

    void log(String level) {
        // plugins can't configure log
        if(plugin) return
        switch(level) {
            case "warn":
                Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_WARN); break
            case "error":
                Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_ERR); break
            case "info":
                Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO); break
            case "debug":
                Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_DEBUG); break
            case "verbose":
                Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_VERBOSE); break
        }
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
        repos?.delegate = this
        repos?.call()
    }



    void flatDir(Map args) {
        def name = args.name?.toString()
        if(name && args.dirs) {
            def fileSystemResolver = new FileSystemResolver()
            fileSystemResolver.local = true
            fileSystemResolver.name = name

            def dirs = args.dirs instanceof Collection ? args.dirs : [args.dirs]

            repositoryData << ['type':'flatDir', name:name, dirs:dirs.join(',')]
            dirs.each { dir ->
               fileSystemResolver.addArtifactPattern "${new File(dir?.toString()).absolutePath}/[artifact]-[revision].[ext]"
            }
            fileSystemResolver.settings = ivySettings

            chainResolver.add fileSystemResolver            
        }
    }
    

    void grailsHome() {
        def grailsHome = BuildSettingsHolder.settings?.grailsHome?.absolutePath ?: System.getenv("GRAILS_HOME")
        if(grailsHome) {
            flatDir(name:"grailsHome", dirs:"${grailsHome}/lib")
            flatDir(name:"grailsHome", dirs:"${grailsHome}/dist")
        }
    }

    private boolean isResolverNotAlreadyDefined(String name) {
        if(chainResolver.resolvers.any { it.name == name }) {
            Message.debug("Dependency resolver $name already defined. Ignoring...")
            return false
        }
        return true
    }
    void mavenRepo(String url) {
        if(isResolverNotAlreadyDefined(url)) {
            repositoryData << ['type':'mavenRepo', root:url, name:url, m2compatbile:true]
            chainResolver.add new IBiblioResolver(name:url, root:url, m2compatible:true, settings:ivySettings)
        }
    }

    void mavenRepo(Map args) {
        if(args && args.name) {
            if(isResolverNotAlreadyDefined(args.name)) {
                repositoryData << ( ['type':'mavenRepo'] + args )
                args.settings = ivySettings
                chainResolver.add new IBiblioResolver(args)
            }
        }
        else {
            Message.warn("A mavenRepo specified doesn't have a name argument. Please specify one!")
        }
    }

    void resolver(DependencyResolver resolver) {
        if(resolver) {
            chainResolver.add resolver
        }        
    }

    void mavenCentral() {
        if(isResolverNotAlreadyDefined('mavenCentral')) {
            repositoryData << ['type':'mavenCentral']
            IBiblioResolver mavenResolver = new IBiblioResolver(name:"mavenCentral")
            mavenResolver.m2compatible = true
            mavenResolver.settings = ivySettings
            chainResolver.add mavenResolver

        }
    }

        
    void dependencies(Closure deps) {
        deps?.delegate = this
        deps?.call()
    }

    def invokeMethod(String name, args) {
        if(!args || !((args[0] instanceof String)||(args[0] instanceof Map)))
            throw new MissingMethodException(name, IvyDependencyManager, args)
        
        def dependencies = args
        def callable
        if(dependencies && (dependencies[-1] instanceof Closure)) {
            callable = dependencies[-1]
            dependencies = dependencies[0..-2]
        }

        parseDependenciesInternal(dependencies, name, callable)
    }

    private parseDependenciesInternal(dependencies, String scope, Closure dependencyConfigurer) {

        def parseDep = { dependency ->
                if ((dependency instanceof String) || (dependency instanceof GString)) {
                    def depDefinition = dependency.toString()

                    def m = depDefinition =~ /([a-zA-Z0-9\-\/\._+=]*?):([a-zA-Z0-9\-\/\._+=]+?):([a-zA-Z0-9\-\/\._+=]+)/

                    if (m.matches()) {

                        def name = m[0][2]
                        if(!moduleExcludes.contains(name)) {
                            def mrid = ModuleRevisionId.newInstance(m[0][1], name, m[0][3])

                            addDependency mrid

                            def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, scope)
                            dependencyDescriptor.inherited = inherited
                            if(plugin) {
                                dependencyDescriptor.plugin = plugin
                            }
                            configureDependencyDescriptor(dependencyDescriptor, scope, dependencyConfigurer)
                        }


                    }

                }
                else if(dependency instanceof Map) {
                    def name = dependency.name
                    if(dependency.group && name && dependency.version) {
                       if(!moduleExcludes.contains(name)) {
                           def mrid = ModuleRevisionId.newInstance(dependency.group, name, dependency.version)

                           addDependency mrid

                           def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, dependency.containsKey('transitive') ? !!dependency.transitive : true, scope)
                           dependencyDescriptor.inherited = inherited
                           if(plugin) {
                               dependencyDescriptor.plugin = plugin
                           }

                           configureDependencyDescriptor(dependencyDescriptor, scope, dependencyConfigurer)
                       }

                    }
                }
            }

            for(dep in dependencies) {
                parseDep dep
            }          
    }



}
