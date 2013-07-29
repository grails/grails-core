/*
 * Copyright 2004-2005 the original author or authors.
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

import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.util.Metadata
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import org.codehaus.groovy.grails.resolve.ivy.IvyExcludeResolver

import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.descriptor.Configuration
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.descriptor.ExcludeRule
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.*
import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.core.resolve.ResolveEngine
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.core.sort.SortEngine
import org.apache.ivy.plugins.repository.TransferListener
import org.codehaus.groovy.grails.plugins.VersionComparator
import org.codehaus.groovy.grails.resolve.ivy.IvyGraphNode
import org.codehaus.groovy.grails.resolve.reporting.SimpleGraphRenderer

/**
 * Implementation that uses Apache Ivy under the hood.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class IvyDependencyManager extends AbstractIvyDependencyManager implements DependencyResolver, DependencyDefinitionParser, DependencyManager{

    Collection repositoryData = new ConcurrentLinkedQueue()
    Collection moduleExcludes = new ConcurrentLinkedQueue()
    TransferListener transferListener

    boolean inheritsAll = false
    boolean resolveErrors = false
    boolean defaultDependenciesProvided = false
    boolean pluginsOnly = false
    boolean inheritRepositories = true

    /**
     * Creates a new IvyDependencyManager instance
     */
    IvyDependencyManager(String applicationName, String applicationVersion, BuildSettings settings=null, Metadata metadata = null, IvySettings ivySettings = new IvySettings()) {
        super(ivySettings, settings, metadata)

        ivySettings.defaultInit()
        // don't cache for snapshots
        if (settings?.grailsVersion?.endsWith("SNAPSHOT")) {
            ivySettings.setDefaultUseOrigin(true)
        }

        ivySettings.validate = false
        ivySettings.setDefaultCache(new File("${System.getProperty("user.home")}/.grails/ivy-cache"))
        chainResolver.settings = ivySettings
        def eventManager = new EventManager()
        def sortEngine = new SortEngine(ivySettings)
        resolveEngine = new ResolveEngine(ivySettings,eventManager,sortEngine)
        resolveEngine.dictatorResolver = chainResolver

        this.applicationName = applicationName
        this.applicationVersion = applicationVersion
    }

    GPathResult downloadPluginList(File localFile) {
        DependencyManagerUtils.downloadPluginList(localFile)
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
     * Serializes the parsed dependencies using the given builder.
     *
     * @param builder A builder such as groovy.xml.MarkupBuilder
     * @deprecated Will be removed in a future release
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
            builder.dependency(group: mrid.organisation, name: mrid.name, version: mrid.revision, conf: dd.scope, transitive: dd.transitive) {
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
     * Returns all of the dependency descriptors for dependencies of the application and not
     * those inherited from frameworks or plugins
     */
    Set<DependencyDescriptor> getApplicationPluginDependencyDescriptors(String scope = null) {
        pluginDependencyDescriptors.findAll { EnhancedDefaultDependencyDescriptor dd ->
            !dd.inherited && dd.exported && (!scope || dd.scope == scope)
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

    boolean isPluginConfiguredByApplication(String name) {
        def propertyName = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name)
        def descriptor = pluginNameToDescriptorMap[name] ?: pluginNameToDescriptorMap[propertyName]
        descriptor?.plugin == null
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
     * Performs a resolve of all dependencies for the given configuration,
     * potentially going out to the internet to download jars if they are not found locally
     */
    ResolveReport resolveDependencies(String conf, Map args) {
        resolveErrors = false
        if (usedConfigurations.contains(conf) || conf == '') {

            if (args.checkIfChanged == null) args.checkIfChanged = true
            if (args.outputReport == null) args.outputReport = true
            if (args.validate == null) args.validate = false

            def options = new ResolveOptions(args)
            if (conf) {
                options.confs = [conf] as String[]
            }

            if (!options.download) {
                def date = new Date()
                def report = new ResolveReport(moduleDescriptor)
                def ivyNodes = resolveEngine.getDependencies(moduleDescriptor, options, report)
                for (IvyNode node in ivyNodes) {
                    if (node.isLoaded()) {
                        for (Artifact a in node.allArtifacts) {
                            def origin = resolveEngine.locate(a)
                            def cr = new ConfigurationResolveReport(resolveEngine, moduleDescriptor, conf, date, options)
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

            ResolveReport resolve = resolveEngine.resolve(moduleDescriptor, options)
            resolveErrors = resolve.hasError()
            return resolve
        }

        // return an empty resolve report
        return new ResolveReport(moduleDescriptor)
    }

    /**
     * Resolves all dependencies for all known configurations
     *
     * @return A ResolveReport containing all of the configurations
     */
    ResolveReport resolveAllDependencies() {
        resolveErrors = false
        def options = new ResolveOptions(checkIfChanged: false, outputReport: true, validate: false)
        options.confs = usedConfigurations as String[]

        ResolveReport resolve = resolveEngine.resolve(moduleDescriptor, options)
        resolveErrors = resolve.hasError()
        return resolve
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
        if (!rootLoader) {
            throw new IllegalStateException("No root loader found. Could not load dependencies. " +
                "Note this method cannot be called when running in a WAR.")
        }

        def urls = rootLoader.URLs.toList()
        ResolveReport report = resolveDependencies(conf)
        for (ArtifactDownloadReport downloadReport in report.allArtifactsReports) {
            def url = downloadReport.localFile.toURI().toURL()
            if (!urls.contains(url)) {
                rootLoader.addURL(url)
            }
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

    @Override
    @CompileStatic
    Collection<Dependency> getPluginDependencies() {
        final descriptors = getEffectivePluginDependencyDescriptors()
        convertToGrailsDependencies(descriptors)
    }

    @Override
    DependencyReport resolve(String scope) {
        final resolveReport = resolveDependencies(scope)
        return new IvyDependencyReport(scope, resolveReport)
    }

    @Override
    DependencyReport resolveAgent() {
        resolve('agent')
    }

    @Override
    DependencyReport resolve() {
        final resolveReport = resolveDependencies()
        return new IvyDependencyReport("compile", resolveReport)

    }

    @Override
    Collection<Dependency> getApplicationDependencies() {
        Set<DependencyDescriptor> descriptors = this.getApplicationDependencyDescriptors()
        return convertToGrailsDependencies(descriptors)
    }

    @CompileStatic
    Set<Dependency> convertToGrailsDependencies(Set<DependencyDescriptor> descriptors) {
        Set<Dependency> dependencies = []
        for (DependencyDescriptor dd in descriptors) {
            EnhancedDefaultDependencyDescriptor edd = (EnhancedDefaultDependencyDescriptor)dd
            final drid = dd.dependencyRevisionId
            def d = new Dependency(drid.organisation, drid.name, drid.revision)
            d.transitive = dd.transitive
            d.exported = edd.exported

            dependencies << d
        }
        dependencies
    }

    @Override
    Collection<Dependency> getAllDependencies() {
        return convertToGrailsDependencies(dependencyDescriptors)
    }

    @Override
    Collection<Dependency> getApplicationDependencies(String scope) {
        convertToGrailsDependencies(getApplicationDependencyDescriptors().findAll { it.scope == scope })
    }

    Collection<Dependency> getPluginDependencies(String scope) {
        convertToGrailsDependencies(effectivePluginDependencyDescriptors.findAll { EnhancedDefaultDependencyDescriptor dd -> dd.scope == scope})
    }

    @Override
    Collection<Dependency> getAllDependencies(String scope) {
        convertToGrailsDependencies(dependencyDescriptors.findAll { it.scope == scope })
    }

    @Override
    ExcludeResolver getExcludeResolver() {
        return new IvyExcludeResolver(this)
    }

    void produceReport(String scope) {
        if(scope) {

            final desc = BuildSettings.SCOPE_TO_DESC[scope]
            if (desc) {
                reportOnScope(scope, desc)
            }
        }
        else {
            produceReport()
        }
    }

    @Override
    @CompileStatic
    void produceReport() {
        // build scope
        reportOnScope(BuildSettings.BUILD_SCOPE, BuildSettings.BUILD_SCOPE_DESC)
        // provided scope
        reportOnScope(BuildSettings.PROVIDED_SCOPE, BuildSettings.PROVIDED_SCOPE_DESC)
        // compile scope
        reportOnScope(BuildSettings.COMPILE_SCOPE, BuildSettings.COMPILE_SCOPE_DESC)
        // runtime scope
        reportOnScope(BuildSettings.RUNTIME_SCOPE, BuildSettings.RUNTIME_SCOPE_DESC)
        // test scope
        reportOnScope(BuildSettings.TEST_SCOPE, BuildSettings.TEST_SCOPE_DESC)
    }

    void reportOnScope(String scope, String desc) {
        ResolveReport resolveReport = resolveDependencies(scope)

        IvyGraphNode node = new IvyGraphNode(resolveReport)
        def renderer = new SimpleGraphRenderer(scope, "$desc (total: ${resolveReport.artifacts.size()})")
        renderer.render(node)
    }
}
