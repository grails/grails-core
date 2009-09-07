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

    @Delegate IvyDependencyManager delegate

    IvyDomainSpecificLanguagerEvaluator(IvyDependencyManager delegate) {
        this.delegate = delegate
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
