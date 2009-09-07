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
import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveEngine
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.sort.SortEngine
import org.apache.ivy.plugins.resolver.ChainResolver
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.codehaus.groovy.grails.resolve.DependencyDefinitionParser
import org.codehaus.groovy.grails.resolve.DependencyResolver
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.Configuration
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import java.util.concurrent.ConcurrentHashMap
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.apache.ivy.core.module.descriptor.ExcludeRule
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule
import org.apache.ivy.plugins.matcher.PatternMatcher
import org.apache.ivy.core.module.id.ArtifactId
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.plugins.matcher.ExactPatternMatcher

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


    private Set<ModuleRevisionId> dependencies = [] as Set
    private Set<DependencyDescriptor> dependencyDescriptors = [] as Set
    private orgToDepMap = [:]

    Map configurationMappings = [ runtime:['runtime(*)','master(*)'],
                                  build:['default'],
                                  compile:['compile(*)', 'master(*)'],
                                  provided:['compile(*)', 'master(*)'],
                                  test:['runtime(*)', 'master(*)']]


    ResolveEngine resolveEngine

    String applicationName
    String applicationVersion
    IvySettings ivySettings
    ChainResolver chainResolver = new ChainResolver(name:"default",returnFirst:true)
    DefaultModuleDescriptor moduleDescriptor

    private static managers = new ConcurrentHashMap()
    private static currentManager

    /**
     * Obtain an Ivy dependency manager instance for the given application name and version
     */
    static IvyDependencyManager getInstance(String applicationName, String applicationVersion) {
        if(!applicationName) throw new IllegalArgumentException("Cannot supply a null application name to Ivy dependency manager")
        if(!applicationVersion) throw new IllegalArgumentException("Cannot supply a null application version to Ivy dependency manager")

        def cacheKey = [version:applicationVersion, name:applicationName]
        def manager = managers[cacheKey]
        if(!manager) {
            manager = new IvyDependencyManager(applicationName, applicationVersion)
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
    IvyDependencyManager(String applicationName, String applicationVersion) {
        ivySettings = new IvySettings()
        ivySettings.defaultInit()
        chainResolver.settings = ivySettings
        def eventManager = new EventManager()
        def sortEngine = new SortEngine(ivySettings)
        resolveEngine = new ResolveEngine(ivySettings,eventManager,sortEngine)
        resolveEngine.dictatorResolver = chainResolver

        this.applicationName = applicationName
        this.applicationVersion = applicationVersion
        
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
                 build "org.tmatesoft.svnkit:svnkit:1.2.0",
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
                       "org.sl4j:slf4j-log4j12:1.5.6"

                // dependencies needed during development, but not for deployment
                provided "javax.servlet:servlet-api:2.5",
                         "javax.servlet:jsp-api:2.1",
                         "javax.servlet:jstl:1.1.2"

                // dependencies needed for compilation
                compile("org.codehaus.groovy:groovy-all:1.6.4") {
                    excludes 'jline'
                }

                compile "aopalliance:aopalliance:1.0",
                        "commons-validator:commons-validator:1.3.1",
                        "commons-el:commons-el:1.0",
                        "commons-beanutils:commons-beanutils:1.8.0",
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
                        [transitive:false]

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
                         "org.slf4j:slf4j-api:1.5.6",
                         "org.sl4j:slf4j-log4j12:1.5.6"

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

    Set<ModuleRevisionId> getModuleRevisionIds(String org) { orgToDepMap[org] }

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
        def options = new ResolveOptions()
        if(conf)
            options.confs = [conf] as String[]


        resolveEngine.resolve(moduleDescriptor,options)
    }

    void parseDependencies(Closure definition) {
        if(definition && applicationName && applicationVersion) {
            this.moduleDescriptor =
                DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(applicationName, applicationName, applicationVersion))

            // TODO: make configurations extensible
            this.moduleDescriptor.addConfiguration BUILD_CONFIGURATION
            this.moduleDescriptor.addConfiguration COMPILE_CONFIGURATION
            this.moduleDescriptor.addConfiguration RUNTIME_CONFIGURATION
            this.moduleDescriptor.addConfiguration TEST_CONFIGURATION
            this.moduleDescriptor.addConfiguration PROVIDED_CONFIGURATION
            
            def evaluator = new IvyDomainSpecificLanguagerEvaluator(this)
            definition.delegate = evaluator
            definition.resolveStrategy = Closure.DELEGATE_FIRST
            definition()
        }
    }

}
class IvyDomainSpecificLanguagerEvaluator {

    static final String WILDCARD = '*'

    boolean inherited = false
    @Delegate IvyDependencyManager delegate

    IvyDomainSpecificLanguagerEvaluator(IvyDependencyManager delegate) {
        this.delegate = delegate
    }

    void inherits(String name) {
        def config = BuildSettingsHolder.settings?.config?.grails
        if(config) {
            def dependencies = config[name]?.dependency?.resolution
            if(dependencies instanceof Closure) {
                try {
                    inherited = true
                    dependencies.delegate = this
                    dependencies.call()
                }
                finally {
                    inherited = false
                }

            }
        }
    }

    void log(String level) {
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

    void mavenRepo(String url) {
        chainResolver.add new IBiblioResolver(name:url, root:url, m2compatible:true, settings:ivySettings)
    }

    void mavenRepo(Map args) {
        if(args) {
            args.settings = ivySettings
            chainResolver.add new IBiblioResolver(args)
        }
    }

    void resolver(DependencyResolver resolver) {
        if(resolver) {
            chainResolver.add resolver
        }        
    }

    void mavenCentral() {
        IBiblioResolver mavenResolver = new IBiblioResolver(name:"mavenCentral")
        mavenResolver.m2compatible = true
        mavenResolver.settings = ivySettings
        chainResolver.add mavenResolver
    }

        
    void dependencies(Closure deps) {
        deps?.delegate = this
        deps?.call()
    }

    def methodMissing(String name, args) {
        if(!args || !(args[0] instanceof String))
            throw new MissingMethodException(name, IvyDependencyManager, args)
        
        def dependencies = args
        def callable
        if(dependencies && (dependencies[-1] instanceof Closure)) {
            callable = dependencies[-1]
            dependencies = dependencies[0..-2]
        }

        parseDependencies(dependencies, name, callable)
    }

    private parseDependencies(dependencies, String scope, Closure dependencyConfigurer) {

        for (dependency in dependencies) {
            if ((dependency instanceof String) || (dependency instanceof GString)) {
                def depDefinition = dependency.toString()

                def m = depDefinition =~ /([a-zA-Z0-9\-\/\._+=]*?):([a-zA-Z0-9\-\/\._+=]+?):([a-zA-Z0-9\-\/\._+=]+)/

                if (m.matches()) {

                    def mrid = ModuleRevisionId.newInstance(m[0][1], m[0][2], m[0][3])

                    addDependency mrid

                    def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, scope)
                    dependencyDescriptor.inherited=inherited
                    def mappings = configurationMappings[scope]
                    mappings?.each {
                        dependencyDescriptor.addDependencyConfiguration scope, it
                    }

                    if(dependencyConfigurer) {
                        dependencyConfigurer.resolveStrategy = Closure.DELEGATE_ONLY
                        dependencyConfigurer.setDelegate(dependencyDescriptor)
                        dependencyConfigurer.call()
                    }

                    dependencyDescriptors << dependencyDescriptor
                    moduleDescriptor.addDependency dependencyDescriptor

                }

            }
        }
    }



}
