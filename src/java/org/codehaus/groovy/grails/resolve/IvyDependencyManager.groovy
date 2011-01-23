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

import grails.util.DslUtils

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
import org.apache.ivy.util.Message

import grails.util.BuildSettings
import org.apache.ivy.core.module.descriptor.ExcludeRule
import grails.util.GrailsNameUtils

import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor
import grails.util.Metadata

import org.apache.ivy.util.MessageLogger
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.report.ConfigurationResolveReport
import org.apache.ivy.core.report.DownloadReport
import org.apache.ivy.core.report.DownloadStatus
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.plugins.repository.TransferListener
import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser
import org.codehaus.groovy.grails.plugins.VersionComparator

import org.codehaus.groovy.grails.resolve.dsl.IvyDomainSpecificLanguageEvaluator

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
            def transitive = DslUtils.getBooleanValueOrDefault(args, 'transitive', true)
            def exported = DslUtils.getBooleanValueOrDefault(args, 'export', true)
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
    
    protected addMetadataPluginDependencies(Map<String, String> plugins) {
        for (plugin in plugins) {
            addMetadataPluginDependency(plugin.key, plugin.value)
        }
    }
    
    protected addMetadataPluginDependency(String name, String version) {
        if (!pluginDependencyNames.contains(entry.key)) {
            def scope = "runtime"
            def mrid = ModuleRevisionId.newInstance("org.grails.plugins", name, version)
            def dd = new EnhancedDefaultDependencyDescriptor(mrid, true, true, scope)
            def artifact = new DefaultDependencyArtifactDescriptor(dd, name, "zip", "zip", null, null )
            dd.addDependencyArtifact(scope, artifact)
            metadataRegisteredPluginNames << name
            configureDependencyDescriptor(dd, scope, null, true)
            pluginDependencyDescriptors << dd
        }
    }
    
    protected addDependencies(DependencyDescriptor[] dependencyDescriptors) {
        for (dependencyDescriptor in dependencyDescriptors) {
            addDependency(dependencyDescriptor)
        }
    }
    
    protected addDependency(DependencyDescriptor dependencyDescriptor) {
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

            doParseDependencies(definition)
            
            // The dependency config can use the pom(Boolean) method to declare
            // that this project has a POM and it has the dependencies, which means
            // we now have to inspect it for the dependencies to use.
            if (readPom && buildSettings) {
                List<DependencyDescriptor> dependencies = readDependenciesFromPOM()
                if (dependencies != null) {
                    addDependencies(dependencies as DependencyDescriptor[])
                }
            }

            // Legacy support for the old mechanism of plugin dependencies being
            // declared in the application.properties file.
            def metadataDeclaredPlugins = metadata?.getInstalledPlugins()
            if (metadataDeclaredPlugins) {
                addMetadataPluginDependencies(metadataDeclaredPlugins)
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

            doParseDependencies(definition, pluginName)
        }
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
    
    /**
     * Evaluates the given DSL definition.
     * 
     * If pluginName is not null, all dependencies will record that they were defined by this plugin.
     * 
     * @see EnhancedDefaultDependencyDescriptor#plugin
     */
    protected doParseDependencies(Closure definition, String pluginName = null) {
        definition.delegate = new IvyDomainSpecificLanguageEvaluator(this, pluginName)
        definition.resolveStrategy = Closure.DELEGATE_FIRST
        definition()
    }
}