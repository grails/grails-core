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

import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.module.descriptor.Configuration
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
import org.apache.ivy.plugins.parser.m2.PomReader
import org.apache.ivy.plugins.repository.file.FileResource
import org.apache.ivy.plugins.repository.file.FileRepository
import org.apache.ivy.plugins.parser.m2.PomDependencyMgt
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.util.url.CredentialsStore

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


    private Set<ModuleId> modules = [] as Set
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
    Set moduleExcludes = [] as Set
    Map pluginExcludes = [:]
    
    boolean readPom = false
    boolean inheritsAll = false
    boolean resolveErrors = false


    /**
     * Creates a new IvyDependencyManager instance
     */
    IvyDependencyManager(String applicationName, String applicationVersion, BuildSettings settings=null) {
        ivySettings = new IvySettings()

        ivySettings.defaultInit()
        // don't cache for snapshots
        if(settings?.grailsVersion?.endsWith("SNAPSHOT")) {
            ivySettings.setDefaultUseOrigin(true) 
        }

        ivySettings.validate = false
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
     * Resets the Grails plugin resolver if it is used
     */
    void resetGrailsPluginsResolver() {
       def resolver = chainResolver.resolvers.find { it.name == 'grailsPlugins' }
       chainResolver.resolvers.remove(resolver)
       chainResolver.resolvers.add(new GrailsPluginsDirectoryResolver(buildSettings, ivySettings))
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
     * Obtains the default dependency definitions when using GRAILS_HOME or the Spring Bundle Repository
     */
    static Closure getBundleRepositoryDependencies(String grailsVersion) {

    }
    /**
     * Obtains the default dependency definitions for the given Grails version
     */
    static Closure getDefaultDependencies(String grailsVersion) {
        return {
            // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
            log "warn"
            repositories {
                grailsPlugins()
                grailsHome()
                // uncomment the below to enable remote dependency resolution
                // from public Maven repositories
                //mavenCentral()
                //mavenLocal()
                //mavenRepo "http://snapshots.repository.codehaus.org"
                //mavenRepo "http://repository.codehaus.org"
                //mavenRepo "http://download.java.net/maven/2/"
                //mavenRepo "http://repository.jboss.com/maven2/
            }
            dependencies {

                // dependencies needed by the Grails build system
                 build "org.gparallelizer:GParallelizer:0.8.3",
                       "org.tmatesoft.svnkit:svnkit:1.3.1",
                       "org.apache.ant:ant:1.7.1",
                       "org.apache.ant:ant-launcher:1.7.1",
                       "org.apache.ant:ant-junit:1.7.1",
                       "org.apache.ant:ant-nodeps:1.7.1",
                       "org.apache.ant:ant-trax:1.7.1",
                       "radeox:radeox:1.0-b2",
                       "jline:jline:0.9.91",
                       "xalan:serializer:2.7.1",
                       "org.grails:grails-bootstrap:$grailsVersion",
                       "org.grails:grails-scripts:$grailsVersion",
                       "org.grails:grails-core:$grailsVersion",
                       "org.grails:grails-resources:$grailsVersion",
                       "org.grails:grails-web:$grailsVersion",
                       "org.slf4j:slf4j-api:1.5.8",
                       "org.slf4j:slf4j-log4j12:1.5.8",
                       "org.springframework:org.springframework.test:3.0.0.RC3"

                // dependencies needed during development, but not for deployment
                provided "javax.servlet:servlet-api:2.5",
                         "javax.servlet:jsp-api:2.1",
                         "javax.servlet:jstl:1.1.2"

                // dependencies needed for compilation
                compile("org.codehaus.groovy:groovy-all:1.6.7-SNAPSHOT") {
                    excludes 'jline'
                }

                compile("commons-beanutils:commons-beanutils:1.8.0", "commons-el:commons-el:1.0", "commons-validator:commons-validator:1.3.1") {
                    excludes "commons-logging", "xml-apis"
                }

                compile( "aopalliance:aopalliance:1.0",
                         "commons-codec:commons-codec:1.3",
                         "commons-collections:commons-collections:3.2.1",
                         "commons-io:commons-io:1.4",
                         "commons-lang:commons-lang:2.4",
                         "javax.transaction:jta:1.1",
                         "org.hibernate:ejb3-persistence:1.0.2.GA",
                         "opensymphony:sitemesh:2.4",
                         "org.grails:grails-bootstrap:$grailsVersion",
                         "org.grails:grails-core:$grailsVersion",
                         "org.grails:grails-crud:$grailsVersion",
                         "org.grails:grails-docs:$grailsVersion",
                         "org.grails:grails-gorm:$grailsVersion",
                         "org.grails:grails-resources:$grailsVersion",
                         "org.grails:grails-spring:$grailsVersion",
                         "org.grails:grails-web:$grailsVersion",
                         "org.springframework:org.springframework.core:3.0.0.RC3",
                         "org.springframework:org.springframework.aop:3.0.0.RC3",
                         "org.springframework:org.springframework.aspects:3.0.0.RC3",
                         "org.springframework:org.springframework.asm:3.0.0.RC3",
                         "org.springframework:org.springframework.beans:3.0.0.RC3",
                         "org.springframework:org.springframework.context:3.0.0.RC3",
                         "org.springframework:org.springframework.context.support:3.0.0.RC3",
                         "org.springframework:org.springframework.expression:3.0.0.RC3",
                         "org.springframework:org.springframework.instrument:3.0.0.RC3",
                         "org.springframework:org.springframework.jdbc:3.0.0.RC3",
                         "org.springframework:org.springframework.jms:3.0.0.RC3",
                         "org.springframework:org.springframework.orm:3.0.0.RC3",
                         "org.springframework:org.springframework.oxm:3.0.0.RC3",
                         "org.springframework:org.springframework.transaction:3.0.0.RC3",
                         "org.springframework:org.springframework.web:3.0.0.RC3",
                         "org.springframework:org.springframework.web.servlet:3.0.0.RC3",
                         "org.slf4j:slf4j-api:1.5.8") {
                        transitive = false
                }


                // dependencies needed for running tests
                test "junit:junit:3.8.2",
                     "org.grails:grails-test:$grailsVersion",
                     "org.springframework:org.springframework.test:3.0.0.RC3"

                // dependencies needed at runtime only
                runtime "org.aspectj:aspectjweaver:1.6.6",
                        "org.aspectj:aspectjrt:1.6.6",
                        "cglib:cglib-nodep:2.1_3",
                        "commons-fileupload:commons-fileupload:1.2.1",
                        "oro:oro:2.0.8"

                // data source
                runtime "commons-dbcp:commons-dbcp:1.2.2",
                        "commons-pool:commons-pool:1.5.3",
                        "hsqldb:hsqldb:1.8.0.10"

                // caching
                runtime ("net.sf.ehcache:ehcache-core:1.7.0") {
                    excludes 'jms', 'commons-logging', 'servlet-api'
                }


                // logging
                runtime( "log4j:log4j:1.2.15",
                         "org.slf4j:jcl-over-slf4j:1.5.8",
                         "org.slf4j:jul-to-slf4j:1.5.8",
                         "org.slf4j:slf4j-log4j12:1.5.8" ) {
                    excludes 'mail', 'jms', 'jmxtools', 'jmxri'
                }

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
     * Returns all of the dependency descriptors for dependencies of the application and not
     * those inherited from frameworks or plugins
     */
    Set<DependencyDescriptor> getApplicationDependencyDescriptors(String scope = null) {
        dependencyDescriptors.findAll { EnhancedDefaultDependencyDescriptor dd ->
            !dd.inherited && (!scope || dd.scope == scope)
        }
    }

    /**
     * Returns all the dependency descriptors for dependencies of a plugin that have been exported for use in the application
     */
    Set<DependencyDescriptor> getExportedDependencyDescriptors(String scope = null) {
        getApplicationDependencyDescriptors(scope).findAll { it.exported }
    }

    /**
    * Obtains a list of dependency descriptors defined in the project
     */
    Set<DependencyDescriptor> getDependencyDescriptors() { dependencyDescriptors }



    /**
     * Adds a dependency to the project
     */
    void addDependency(ModuleRevisionId revisionId) {
        modules << revisionId.moduleId
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
             def transitive = getBooleanValue(args, 'transitive')
             def exported = getBooleanValue(args, 'export')
             def scope = args.conf ?: 'runtime'
             def mrid = ModuleRevisionId.newInstance(args.group, args.name, args.version)
             def dd = new EnhancedDefaultDependencyDescriptor(mrid, true, transitive, scope)
             dd.exported = exported
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
        if(dependencyDescriptor.isExportedToApplication())
            moduleDescriptor.addDependency dependencyDescriptor
    }


    Set<ModuleRevisionId> getModuleRevisionIds(String org) { orgToDepMap[org] }

    /**
     * Lists all known dependencies for the given configuration name (defaults to all dependencies)
     */
    IvyNode[] listDependencies(String conf = null) {
        def options = new ResolveOptions()
        if(conf) {
            options.confs = [conf] as String[]
        }



        resolveEngine.getDependencies(moduleDescriptor, options, new ResolveReport(moduleDescriptor))
    }


    public ResolveReport resolveDependencies(Configuration conf) {
        resolveDependencies(conf.name)
    }

    /**
     * Resolves only application dependencies and returns a list of the resolves JAR files
     */
    public List<ArtifactDownloadReport> resolveApplicationDependencies(String conf = '') {
        ResolveReport report = resolveDependencies(conf)

        def descriptors = getApplicationDependencyDescriptors(conf)
        report.allArtifactsReports.findAll { ArtifactDownloadReport downloadReport ->
            def mrid = downloadReport.artifact.moduleRevisionId
            descriptors.any { DependencyDescriptor dd -> mrid == dd.dependencyRevisionId}
        }
    }

    /**
     * Resolves only plugin dependencies that should be exported to the application
     */
    public List<ArtifactDownloadReport> resolveExportedDependencies(String conf='') {

        def descriptors = getExportedDependencyDescriptors(conf)
        resolveApplicationDependencies(conf)?.findAll { ArtifactDownloadReport downloadReport ->
            def mrid = downloadReport.artifact.moduleRevisionId
            descriptors.any { DependencyDescriptor dd -> mrid == dd.dependencyRevisionId}
        }
    }

    /**
     * Performs a resolve of all dependencies, potentially going out to the internet to download jars
     * if they are not found locally
     */
    public ResolveReport resolveDependencies() {
        resolveDependencies('')
    }


    /**
     * Performs a resolve of all dependencies for the given configuration,
     * potentially going out to the internet to download jars if they are not found locally
     */
    public ResolveReport resolveDependencies(String conf) {
        resolveErrors = false
        if(usedConfigurations.contains(conf) || conf == '') {
            def options = new ResolveOptions(checkIfChanged:false, outputReport:true, validate:false)
            if(conf)
                options.confs = [conf] as String[]


            ResolveReport resolve = resolveEngine.resolve(moduleDescriptor, options)
            resolveErrors = resolve.hasError()
            return resolve
        }
        else {
            // return an empty resolve report
            return new ResolveReport(moduleDescriptor)
        }
    }



    /**
     * Tests whether the given ModuleId is defined in the list of dependencies
     */
    boolean hasDependency(ModuleId mid) {
       return modules.contains(mid)
    }

    /**
     * Tests whether the given group and name are defined in the list of dependencies 
     */
    boolean hasDependency(String group, String name) {
        return hasDependency(ModuleId.newInstance(group, name))
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
			evaluator = null

            if(readPom && buildSettings) {
                List dependencies = readDependenciesFromPOM()

                if(dependencies!=null){
                    for(PomDependencyMgt dep in dependencies) {
                        final String scope = dep.scope ?: 'runtime'
                        Message.debug("Read dependency [$dep.groupId:$dep.artifactId:$dep.version] (scope $scope) from Maven pom.xml") 

                        def mrid = ModuleRevisionId.newInstance(dep.groupId, dep.artifactId, dep.version)
                        def mid = mrid.getModuleId()
                        if(!hasDependency(mid)) {
                            def dd = new EnhancedDefaultDependencyDescriptor(mrid, false, true, scope)
                            configureDependencyDescriptor(dd, scope)
                            addDependencyDescriptor dd
                        }
                    }
                }
            }

        }
    }

    List readDependenciesFromPOM() {
        def dependencies = null
        def pom = new File("${buildSettings.baseDir.path}/pom.xml")
        if (pom.exists()) {
            def reader = new PomReader(pom.toURL(), new FileResource(new FileRepository(), pom))

            dependencies = reader.getDependencies()
        }
        return dependencies
    }


    /**
     * Parses dependencies of a plugin
     *
     * @param pluginName the name of the plugin
     * @param definition the Ivy DSL definition
     */
    void parseDependencies(String pluginName,Closure definition) {
        if(definition) {
            if(moduleDescriptor == null) throw new IllegalStateException("Call parseDependencies(Closure) first to parse the application dependencies")

            def evaluator = new IvyDomainSpecificLanguageEvaluator(this)
            evaluator.plugin = pluginName
            definition.delegate = evaluator
            definition.resolveStrategy = Closure.DELEGATE_FIRST
            definition()
        }
    }


    boolean getBooleanValue(dependency, String name) {
        return dependency.containsKey(name) ? Boolean.valueOf(dependency[name]) : true
    }
    


}
class IvyDomainSpecificLanguageEvaluator {

    static final String WILDCARD = '*'

    boolean inherited = false
    String plugin = null
    @Delegate IvyDependencyManager delegate

    IvyDomainSpecificLanguageEvaluator(IvyDependencyManager delegate) {
        this.delegate = delegate
    }

    void useOrigin(boolean b) {
        ivySettings.setDefaultUseOrigin(b)
    }

    void credentials(Closure c) {
        def creds = [:]
        c.delegate = creds
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()

        if(creds) {
            CredentialsStore.INSTANCE.addCredentials(creds.realm ?: null, creds.host ?: 'localhost', creds.username ?: '', creds.password ?: '')
        }
    }

    void pom(boolean b) {
        delegate.readPom = b
    }

    void excludes(String... excludes) {
        if(plugin) {
              for(name in excludes) {
                  pluginExcludes[plugin] << name
              }
        }
        else {
            for(name in excludes ) {
                moduleExcludes << name
            }
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
        pluginExcludes[name] = new HashSet()

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
               def path = new File(dir?.toString()).absolutePath
               fileSystemResolver.addIvyPattern( "${path}/[module]-[revision](-[classifier]).pom")                
               fileSystemResolver.addArtifactPattern "${path}/[module]-[revision](-[classifier]).[ext]"
            }
            fileSystemResolver.settings = ivySettings

            chainResolver.add fileSystemResolver            
        }
    }
    

    void grailsPlugins() {
        if(isResolverNotAlreadyDefined('grailsPlugins')) {            
           repositoryData << ['type':'grailsPlugins', name:"grailsPlugins"]
           if(buildSettings!=null) {               
               def pluginResolver = new GrailsPluginsDirectoryResolver(buildSettings, ivySettings)

               chainResolver.add pluginResolver
           }
        }

    }

    void grailsHome() {
        if(isResolverNotAlreadyDefined('grailsHome')) {
            def grailsHome = buildSettings?.grailsHome?.absolutePath ?: System.getenv("GRAILS_HOME")
            if(grailsHome) {
                flatDir(name:"grailsHome", dirs:"${grailsHome}/lib")
                flatDir(name:"grailsHome", dirs:"${grailsHome}/dist")
            }
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

    void resolver(org.apache.ivy.plugins.resolver.DependencyResolver resolver) {
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

    void mavenLocal(String repoPath = "${System.getProperty('user.home')}/.m2/repository") {
        if (isResolverNotAlreadyDefined('mavenLocal')) {
            repositoryData << ['type':'mavenLocal']
            FileSystemResolver localMavenResolver = new FileSystemResolver(name:'localMavenResolver');
            localMavenResolver.local = true
            localMavenResolver.m2compatible = true
            localMavenResolver.addIvyPattern(
                                "${repoPath}/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).pom")

            localMavenResolver.addArtifactPattern(
                    "${repoPath}/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]")
            
            localMavenResolver.settings = ivySettings
            chainResolver.add localMavenResolver
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

        if(dependencies) {

            parseDependenciesInternal(dependencies, name, callable)
        }
    }

    private parseDependenciesInternal(dependencies, String scope, Closure dependencyConfigurer) {

        boolean usedArgs = false
        def parseDep = { dependency ->
                if ((dependency instanceof String) || (dependency instanceof GString)) {
                    def args = [:]
                    if(dependencies[-1] instanceof Map) {
                        args = dependencies[-1]
                        usedArgs = true
                    }
                    def depDefinition = dependency.toString()

                    def m = depDefinition =~ /([a-zA-Z0-9\-\/\._+=]*?):([a-zA-Z0-9\-\/\._+=]+?):([a-zA-Z0-9\-\/\._+=]+)/

                    if (m.matches()) {

                        def name = m[0][2]
                        if(!isExcluded(name)) {
                            def mrid = ModuleRevisionId.newInstance(m[0][1], name, m[0][3])

                            addDependency mrid

                            def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, getBooleanValue(args, 'transitive'), scope)
                            dependencyDescriptor.exported = getBooleanValue(args, 'export')
                            dependencyDescriptor.inherited = inherited || inheritsAll || plugin

                            if(plugin) {
                                if(!pluginExcludes[plugin]) {
                                    pluginExcludes[plugin] = new HashSet()
                                }
                                pluginExcludes[plugin] << name
                                dependencyDescriptor.plugin = plugin
                            }
                            configureDependencyDescriptor(dependencyDescriptor, scope, dependencyConfigurer)
                        }


                    }

                }
                else if(dependency instanceof Map) {
                    def name = dependency.name
                    if(dependency.group && name && dependency.version) {
                       if(!isExcluded(name)) {

                           def mrid
                           if(dependency.branch) {
                               mrid = ModuleRevisionId.newInstance(dependency.group, name, dependency.branch, dependency.version)
                           }
                           else {
                               mrid = ModuleRevisionId.newInstance(dependency.group, name, dependency.version)
                           }

                           addDependency mrid

                           def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, getBooleanValue(dependency, 'transitive'), scope)
                           dependencyDescriptor.exported = getBooleanValue(dependency, 'export')
                           dependencyDescriptor.inherited = inherited || inheritsAll
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
                if((dependencies[-1] == dep) && usedArgs) break
            }          
    }


    boolean isExcluded(name) {
        return moduleExcludes.contains(name) ||
                (plugin != null && pluginExcludes[plugin]?.contains(name) )
    }


}
