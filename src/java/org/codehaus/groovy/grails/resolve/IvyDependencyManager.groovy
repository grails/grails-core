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

import grails.util.BuildSettings
import org.apache.ivy.core.module.descriptor.ExcludeRule
import grails.util.GrailsNameUtils

import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.util.url.CredentialsStore
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor
import grails.util.Metadata

import org.apache.ivy.plugins.latest.LatestTimeStrategy
import org.apache.ivy.util.MessageLogger
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.report.ConfigurationResolveReport
import org.apache.ivy.core.report.DownloadReport
import org.apache.ivy.core.report.DownloadStatus
import org.apache.ivy.plugins.matcher.PatternMatcher
import org.apache.ivy.plugins.matcher.ExactPatternMatcher
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule
import org.apache.ivy.core.module.id.ArtifactId
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.plugins.repository.TransferListener
import java.util.concurrent.ConcurrentLinkedQueue



import org.apache.ivy.plugins.resolver.RepositoryResolver
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser
import org.codehaus.groovy.grails.plugins.VersionComparator

/**
 * Implementation that uses Apache Ivy under the hood.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class IvyDependencyManager extends AbstractIvyDependencyManager implements DependencyResolver, DependencyDefinitionParser{

    
    ResolveEngine resolveEngine
    BuildSettings buildSettings
    IvySettings ivySettings
    MessageLogger logger
    Metadata metadata
    ChainResolver chainResolver = new ChainResolver(name:"default",returnFirst:true)  
    DefaultDependencyDescriptor currentDependencyDescriptor
    Collection repositoryData = new ConcurrentLinkedQueue()
    Collection<String> configuredPlugins = new ConcurrentLinkedQueue()
    
    Collection moduleExcludes = new ConcurrentLinkedQueue()
    TransferListener transferListener

    boolean readPom = false
    boolean inheritsAll = false
    boolean resolveErrors = false
	boolean defaultDependenciesProvided = false
	boolean pluginsOnly = false
	boolean inheritRepositories = true
	
    /**
     * Creates a new IvyDependencyManager instance
     */
    IvyDependencyManager(String applicationName, String applicationVersion, BuildSettings settings=null, Metadata metadata = null) {
        ivySettings = new IvySettings()

        ivySettings.defaultInit()
        // don't cache for snapshots
        if (settings?.grailsVersion?.endsWith("SNAPSHOT")) {
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
        this.metadata = metadata
    }

    /**
     * Allows settings an alternative chain resolver to be used
     * @param resolver The resolver to be used
     */
    void setChainResolver(ChainResolver resolver) {
        this.chainResolver = resolver
        resolveEngine.dictatorResolver = chainResolver
    }

    /**
     * Sets the default message logger used by Ivy
     *
     * @param logger
     */
    void setLogger(MessageLogger logger) {
        Message.setDefaultLogger logger
        this.logger = logger
    }

    MessageLogger getLogger() { this.logger }

    /**
     * @return The current chain resolver
     */
    ChainResolver getChainResolver() { chainResolver }

    /**
     * Resets the Grails plugin resolver if it is used
     */
    void resetGrailsPluginsResolver() {
        def resolver = chainResolver.resolvers.find { it.name == 'grailsPlugins' }
        chainResolver.resolvers.remove(resolver)
        chainResolver.resolvers.add(new GrailsPluginsDirectoryResolver(buildSettings, ivySettings))
    }


    /**
     * Serializes the parsed dependencies using the given builder.
     *
     * @param builder A builder such as groovy.xml.MarkupBuilder
     */
    void serialize(builder, boolean createRoot = true) {
        if (createRoot) {
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
            for (resolverData in repositoryData) {
                if (resolverData.name=='grailsHome') continue
                builder.resolver resolverData
            }
        }
    }

    private serializeDependencies(builder) {
        for (EnhancedDefaultDependencyDescriptor dd in dependencyDescriptors) {
            // dependencies inherited by Grails' global config are not included
            if (dd.inherited) continue

            def mrid = dd.dependencyRevisionId
            builder.dependency( group: mrid.organisation, name: mrid.name, version: mrid.revision, conf: dd.scope, transitive: dd.transitive) {
                for (ExcludeRule er in dd.allExcludeRules) {
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
				def compileTimeDependenciesMethod = defaultDependenciesProvided ? 'provided' : 'compile'
				def runtimeDependenciesMethod = defaultDependenciesProvided ? 'provided' : 'runtime'
				
				// dependencies needed by the Grails build system
				for(mrid in [ ModuleRevisionId.newInstance("org.tmatesoft.svnkit", "svnkit", "1.3.1"),
							  ModuleRevisionId.newInstance("org.apache.ant","ant","1.7.1"),
							  ModuleRevisionId.newInstance("org.apache.ant","ant-launcher","1.7.1"),
							  ModuleRevisionId.newInstance("org.apache.ant","ant-junit","1.7.1"),
							  ModuleRevisionId.newInstance("org.apache.ant","ant-nodeps","1.7.1"),
							  ModuleRevisionId.newInstance("org.apache.ant","ant-trax","1.7.1"),
							  ModuleRevisionId.newInstance("jline","jline","0.9.94"),
							  ModuleRevisionId.newInstance("org.fusesource.jansi","jansi","1.2.1"),
							  ModuleRevisionId.newInstance("xalan","serializer","2.7.1"),
							  ModuleRevisionId.newInstance("org.grails","grails-docs",grailsVersion),
							  ModuleRevisionId.newInstance("org.grails","grails-bootstrap", grailsVersion),
							  ModuleRevisionId.newInstance("org.grails","grails-scripts",grailsVersion),
							  ModuleRevisionId.newInstance("org.grails","grails-core",grailsVersion),
							  ModuleRevisionId.newInstance("org.grails","grails-resources",grailsVersion),
							  ModuleRevisionId.newInstance("org.grails","grails-web",grailsVersion),
							  ModuleRevisionId.newInstance("org.slf4j","slf4j-api","1.5.8"),
							  ModuleRevisionId.newInstance("org.slf4j","slf4j-log4j12","1.5.8"),
							  ModuleRevisionId.newInstance("org.springframework","org.springframework.test","3.0.3.RELEASE"),
							  ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap","concurrentlinkedhashmap-lru","1.0_jdk5")] ) {
						def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"build")
 					    addDependency mrid
						configureDependencyDescriptor(dependencyDescriptor, "build", null, false)
				  }

				
				for(mrid in [ModuleRevisionId.newInstance("org.xhtmlrenderer","core-renderer","R8"),
							 ModuleRevisionId.newInstance("com.lowagie","itext","2.0.8"),
							 ModuleRevisionId.newInstance("radeox","radeox","1.0-b2")]) {
				   def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"docs")
				   addDependency mrid
				   configureDependencyDescriptor(dependencyDescriptor, "docs", null, false)
				}

				// dependencies needed during development, but not for deployment
				for(mrid in [ModuleRevisionId.newInstance("javax.servlet","servlet-api","2.5"),
				 			 ModuleRevisionId.newInstance( "javax.servlet","jsp-api","2.1")]) {
				   def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"provided")
				   addDependency mrid
				   configureDependencyDescriptor(dependencyDescriptor, "provided", null, false)
				}

				// dependencies needed for compilation
				"${compileTimeDependenciesMethod}"("org.codehaus.groovy:groovy-all:1.8.0-beta-4-SNAPSHOT") {
					excludes 'jline'
				}

				"${compileTimeDependenciesMethod}"("commons-beanutils:commons-beanutils:1.8.0", "commons-el:commons-el:1.0", "commons-validator:commons-validator:1.3.1") {
					excludes "commons-logging", "xml-apis"
				}

				for(mrid in [	ModuleRevisionId.newInstance("org.coconut.forkjoin","jsr166y","070108"),
								ModuleRevisionId.newInstance("org.codehaus.gpars","gpars","0.9"),
								ModuleRevisionId.newInstance("aopalliance","aopalliance","1.0"),
								ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap","concurrentlinkedhashmap-lru","1.0_jdk5"),
								ModuleRevisionId.newInstance("commons-codec","commons-codec","1.4"),
								ModuleRevisionId.newInstance("commons-collections","commons-collections","3.2.1"),
								ModuleRevisionId.newInstance("commons-io","commons-io","1.4"),
								ModuleRevisionId.newInstance("commons-lang","commons-lang","2.4"),
								ModuleRevisionId.newInstance("javax.transaction","jta","1.1"),
								ModuleRevisionId.newInstance("org.hibernate","ejb3-persistence","1.0.2.GA"),
								ModuleRevisionId.newInstance("opensymphony","sitemesh","2.4"),
								ModuleRevisionId.newInstance("org.grails","grails-bootstrap","$grailsVersion"),
								ModuleRevisionId.newInstance("org.grails","grails-core","$grailsVersion"),
								ModuleRevisionId.newInstance("org.grails","grails-crud","$grailsVersion"),
								ModuleRevisionId.newInstance("org.grails","grails-gorm","$grailsVersion"),
								ModuleRevisionId.newInstance("org.grails","grails-resources","$grailsVersion"),
								ModuleRevisionId.newInstance("org.grails","grails-spring","$grailsVersion"),
								ModuleRevisionId.newInstance("org.grails","grails-web","$grailsVersion"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.core","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.aop","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.aspects","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.asm","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.beans","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.context","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.context.support","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.expression","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.instrument","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.jdbc","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.jms","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.orm","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.oxm","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.transaction","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.web","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.springframework","org.springframework.web.servlet","3.0.3.RELEASE"),
								ModuleRevisionId.newInstance("org.slf4j","slf4j-api","1.5.8")] ) {
						   def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,compileTimeDependenciesMethod)
						   addDependency mrid
						   configureDependencyDescriptor(dependencyDescriptor, compileTimeDependenciesMethod, null, false)
					}


					// dependencies needed for running tests
					for(mrid in [	ModuleRevisionId.newInstance("junit","junit","4.8.1"),
								 	ModuleRevisionId.newInstance("org.grails","grails-test","$grailsVersion"),
								 	ModuleRevisionId.newInstance("org.springframework","org.springframework.test","3.0.3.RELEASE")] ) {
						   def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,"test")
						   addDependency mrid
						   configureDependencyDescriptor(dependencyDescriptor, "test", null, false)
					}

					// dependencies needed at runtime only
					for( mrid in [ 		ModuleRevisionId.newInstance("org.aspectj","aspectjweaver","1.6.8"),
										ModuleRevisionId.newInstance("org.aspectj","aspectjrt","1.6.8"),
										ModuleRevisionId.newInstance("cglib","cglib-nodep","2.1_3"),
										ModuleRevisionId.newInstance("commons-fileupload","commons-fileupload","1.2.1"),
										ModuleRevisionId.newInstance("oro","oro","2.0.8"),
										ModuleRevisionId.newInstance("javax.servlet","jstl","1.1.2"),
										// data source
										ModuleRevisionId.newInstance("commons-dbcp","commons-dbcp","1.3"),
										ModuleRevisionId.newInstance("commons-pool","commons-pool","1.5.5"),
										ModuleRevisionId.newInstance("hsqldb","hsqldb","1.8.0.10"),
										ModuleRevisionId.newInstance("com.h2database","h2","1.2.144"),
										// JSP support
										ModuleRevisionId.newInstance("apache-taglibs","standard","1.1.2"),
										ModuleRevisionId.newInstance("xpp3","xpp3_min","1.1.3.4.O") ] ) {
						   def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false ,runtimeDependenciesMethod)
						   addDependency mrid
						   configureDependencyDescriptor(dependencyDescriptor, runtimeDependenciesMethod, null, false)
					}

					// caching
					"${runtimeDependenciesMethod}" ("net.sf.ehcache:ehcache-core:1.7.1") {
						excludes 'jms', 'commons-logging', 'servlet-api'
					}

					// logging
					"${runtimeDependenciesMethod}"("log4j:log4j:1.2.16",
							"org.slf4j:jcl-over-slf4j:1.5.8",
							"org.slf4j:jul-to-slf4j:1.5.8",
							"org.slf4j:slf4j-log4j12:1.5.8" ) {
						excludes 'mail', 'jms', 'jmxtools', 'jmxri'
					}
            }
        }
    }

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

    boolean isExcluded(String name) {
        def aid = createExcludeArtifactId(name)
        return moduleDescriptor.doesExclude(configurationNames, aid)
    }

    /**
     * For usages such as addPluginDependency("foo", [group:"junit", name:"junit", version:"4.8.1"])
     *
     * This method is designed to be used by the internal framework and plugins and not be end users.
     * The idea is that plugins can provide dependencies at runtime which are then inherited by
     * the user's dependency configuration
     *
     * A user can however override a plugin's dependencies inside the dependency resolution DSL
     */
    void addPluginDependency(String pluginName, Map args) {
        // do nothing if the dependencies of the plugin are configured by the application
        if (isPluginConfiguredByApplication(pluginName)) return
        if (args?.group && args?.name && args?.version) {
            def transitive = getBooleanValue(args, 'transitive')
            def exported = getBooleanValue(args, 'export')
            def scope = args.conf ?: 'runtime'
            def mrid = ModuleRevisionId.newInstance(args.group, args.name, args.version)
            def dd = new EnhancedDefaultDependencyDescriptor(mrid, true, transitive, scope)
            dd.exported = exported
            dd.inherited = true
            dd.plugin = pluginName
            configureDependencyDescriptor(dd, scope)
            if (args.excludes) {
                for (ex in excludes) {
                    dd.exclude(ex)
                }
            }
            addDependencyDescriptor dd
        }
    }

    boolean isPluginConfiguredByApplication(String name) {
        (configuredPlugins.contains(name) || configuredPlugins.contains(GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name)))
    }

    Set<ModuleRevisionId> getModuleRevisionIds(String org) { orgToDepMap[org] }

    /**
     * Lists all known dependencies for the given configuration name (defaults to all dependencies)
     */
    IvyNode[] listDependencies(String conf = null) {
        def options = new ResolveOptions()
        if (conf) {
            options.confs = [conf] as String[]
        }

        resolveEngine.getDependencies(moduleDescriptor, options, new ResolveReport(moduleDescriptor))
    }

    ResolveReport resolveDependencies(Configuration conf) {
        resolveDependencies(conf.name)
    }

    /**
     * Performs a resolve of all dependencies for the given configuration,
     * potentially going out to the internet to download jars if they are not found locally
     */
    ResolveReport resolveDependencies(String conf) {
        resolveErrors = false
        if (usedConfigurations.contains(conf) || conf == '') {
            def options = new ResolveOptions(checkIfChanged:false, outputReport:true, validate:false)
            if (conf) {
                options.confs = [conf] as String[]
            }

            ResolveReport resolve = resolveEngine.resolve(moduleDescriptor, options)
            resolveErrors = resolve.hasError()
            return resolve
        }

        // return an empty resolve report
        return new ResolveReport(moduleDescriptor)
    }

    /**
     * Similar to resolveDependencies, but will load the resolved dependencies into the
     * application RootLoader if it exists
     *
     * @return The ResolveReport
     * @throws IllegalStateException If no RootLoader exists
     */
    ResolveReport loadDependencies(String conf = '') {

        URLClassLoader rootLoader = getClass().classLoader.rootLoader
        if (rootLoader) {
            def urls = rootLoader.URLs.toList()
            ResolveReport report = resolveDependencies(conf)
            for (ArtifactDownloadReport downloadReport in report.allArtifactsReports) {
                def url = downloadReport.localFile.toURL()
                if (!urls.contains(url)) {
                    rootLoader.addURL(url)
                }
            }
        }
        else {
            throw new IllegalStateException("No root loader found. Could not load dependencies. Note this method cannot be called when running in a WAR.")
        }
    }

    /**
     * Resolves only application dependencies and returns a list of the resolves JAR files
     */
    List<ArtifactDownloadReport> resolveApplicationDependencies(String conf = '') {
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
    List<ArtifactDownloadReport> resolveExportedDependencies(String conf='') {

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
    ResolveReport resolveDependencies() {
        resolveDependencies('')
    }

    /**
     * Performs a resolve of declared plugin dependencies (zip files containing plugin distributions)
     */
    ResolveReport resolvePluginDependencies(String conf = '', Map args = [:]) {
        resolveErrors = false
        if (usedConfigurations.contains(conf) || conf == '') {

            if (args.checkIfChanged == null) args.checkIfChanged = true
            if (args.outputReport == null) args.outputReport = true
            if (args.validate == null) args.validate = false

            def options = new ResolveOptions(args)
            if (conf) {
                options.confs = [conf] as String[]
            }

            def md = createModuleDescriptor()
            for (dd in pluginDependencyDescriptors) {
                md.addDependency dd
            }
            if (!options.download) {
                def date = new Date()
                def report = new ResolveReport(md)
                def ivyNodes = resolveEngine.getDependencies(md, options, report)
                for (IvyNode node in ivyNodes) {
                    if (node.isLoaded()) {
                        for (Artifact a in node.allArtifacts) {
                            def origin = resolveEngine.locate(a)
                            def cr = new ConfigurationResolveReport(resolveEngine, md, conf, date, options)
                            def dr = new DownloadReport()
                            def adr = new ArtifactDownloadReport(a)
                            adr.artifactOrigin = origin
                            adr.downloadStatus = DownloadStatus.NO
                            dr.addArtifactReport(adr)
                            cr.addDependency(node, dr)
                            report.addReport(conf, cr)
                        }
                    }
                }
                return report
            }

            ResolveReport resolve = resolveEngine.resolve(md, options)
            resolveErrors = resolve.hasError()
            return resolve
        }

        // return an empty resolve report
        return new ResolveReport(createModuleDescriptor())
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
		if (definition && applicationName && applicationVersion) {
			if (this.moduleDescriptor == null) {
				this.moduleDescriptor = createModuleDescriptor()
			}

			def evaluator = new IvyDomainSpecificLanguageEvaluator(this)
			definition.delegate = evaluator
			definition.resolveStrategy = Closure.DELEGATE_FIRST
			definition()
			evaluator = null

			if (readPom && buildSettings) {
				List dependencies = readDependenciesFromPOM()

				if (dependencies != null) {
					for (DependencyDescriptor dependencyDescriptor in dependencies) {
						ModuleRevisionId moduleRevisionId = dependencyDescriptor.getDependencyRevisionId()
						ModuleId moduleId = moduleRevisionId.getModuleId()

						String groupId = moduleRevisionId.getOrganisation()
						String artifactId = moduleRevisionId.getName()
						String version = moduleRevisionId.getRevision()
						String scope = Arrays.asList(dependencyDescriptor.getModuleConfigurations()).get(0)

						if (!hasDependency(moduleId)) {
							def enhancedDependencyDescriptor = new EnhancedDefaultDependencyDescriptor(moduleRevisionId, false, true, scope)
							for (ExcludeRule excludeRule in dependencyDescriptor.getAllExcludeRules()) {
								ModuleId excludedModule = excludeRule.getId().getModuleId()
								enhancedDependencyDescriptor.addRuleForModuleId(excludedModule, scope)
							}
							configureDependencyDescriptor(enhancedDependencyDescriptor, scope)
							addDependencyDescriptor enhancedDependencyDescriptor
						}
					}
				}
			}

			def installedPlugins = metadata?.getInstalledPlugins()
			if (installedPlugins) {
				for (entry in installedPlugins) {
					if (!pluginDependencyNames.contains(entry.key)) {
						def name = entry.key
						def scope = "runtime"
						def mrid = ModuleRevisionId.newInstance("org.grails.plugins", name, entry.value)
						def dd = new EnhancedDefaultDependencyDescriptor(mrid, true, true, scope)
						def artifact = new DefaultDependencyArtifactDescriptor(dd, name, "zip", "zip", null, null )
						dd.addDependencyArtifact(scope, artifact)
						metadataRegisteredPluginNames << name
						configureDependencyDescriptor(dd, scope, null, true)
						pluginDependencyDescriptors << dd
					}
				}
			}
		}
    }

    List readDependenciesFromPOM() {
      List fixedDependencies = null
      def pom = new File("${buildSettings.baseDir.path}/pom.xml")
      if (pom.exists()) {
          PomModuleDescriptorParser parser = PomModuleDescriptorParser.getInstance()
          ModuleDescriptor md = parser.parseDescriptor(ivySettings, pom.toURL(), false)

          fixedDependencies = md.getDependencies()
      }

      return fixedDependencies
    }

    /**
     * Parses dependencies of a plugin
     *
     * @param pluginName the name of the plugin
     * @param definition the Ivy DSL definition
     */
    void parseDependencies(String pluginName,Closure definition) {
		if (definition) {
			if (moduleDescriptor == null) {
				throw new IllegalStateException("Call parseDependencies(Closure) first to parse the application dependencies")
			}

			def evaluator = new IvyDomainSpecificLanguageEvaluator(this)
			evaluator.currentPluginBeingConfigured = pluginName
			definition.delegate = evaluator
			definition.resolveStrategy = Closure.DELEGATE_FIRST
			definition()
		}
    }

    boolean getBooleanValue(dependency, String name) {
        return dependency.containsKey(name) ? Boolean.valueOf(dependency[name]) : true
    }
    
    /**
     * The plugin dependencies excluding non-exported transitive deps and
     * collapsed to the highest version of each dependency.
     */
    Set<DependencyDescriptor> getEffectivePluginDependencyDescriptors() {
        def versionComparator = new VersionComparator()
        def candidates = getPluginDependencyDescriptors().findAll { it.exportedToApplication }
        def groupedByModule = candidates.groupBy { it.dependencyRevisionId.moduleId }

        groupedByModule.collect {
            it.value.max { lhs, rhs -> 
                def versionComparison = versionComparator.compare(lhs.dependencyRevisionId.revision, rhs.dependencyRevisionId.revision)
                versionComparison ?: (rhs.plugin <=> lhs.plugin)
            }
        }
    }
    
}

class IvyDomainSpecificLanguageEvaluator {

    static final String WILDCARD = '*'

    boolean inherited = false
    boolean pluginMode = false
	boolean repositoryMode = false
	
    String currentPluginBeingConfigured = null
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
            urlResolver.latestStrategy = new org.apache.ivy.plugins.latest.LatestTimeStrategy()
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

