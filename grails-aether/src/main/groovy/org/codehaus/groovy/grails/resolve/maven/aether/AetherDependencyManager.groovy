/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.maven.aether

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingResult
import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest
import org.apache.maven.settings.building.SettingsBuilder
import org.apache.maven.settings.building.SettingsBuildingResult
import org.codehaus.groovy.grails.resolve.DependencyManager
import org.codehaus.groovy.grails.resolve.DependencyManagerUtils
import org.codehaus.groovy.grails.resolve.DependencyReport
import org.codehaus.groovy.grails.resolve.ExcludeResolver
import org.codehaus.groovy.grails.resolve.maven.aether.config.AetherDsl
import org.codehaus.groovy.grails.resolve.maven.aether.config.DependencyConfiguration
import org.codehaus.groovy.grails.resolve.maven.aether.support.GrailsConsoleLoggerManager
import org.codehaus.groovy.grails.resolve.reporting.SimpleGraphRenderer
import org.codehaus.plexus.DefaultPlexusContainer
import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.collection.CollectRequest
import org.sonatype.aether.collection.DependencyCollectionException
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.DependencyNode
import org.sonatype.aether.graph.Exclusion
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.repository.LocalRepository
import org.sonatype.aether.repository.Proxy
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.repository.RepositoryPolicy
import org.sonatype.aether.resolution.ArtifactRequest
import org.sonatype.aether.resolution.ArtifactResolutionException
import org.sonatype.aether.resolution.ArtifactResult
import org.sonatype.aether.resolution.DependencyRequest
import org.sonatype.aether.resolution.DependencyResolutionException
import org.sonatype.aether.resolution.DependencyResult
import org.sonatype.aether.transfer.AbstractTransferListener
import org.sonatype.aether.transfer.ArtifactTransferException
import org.sonatype.aether.transfer.TransferCancelledException
import org.sonatype.aether.transfer.TransferEvent
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.filter.ScopeDependencyFilter
import org.sonatype.aether.util.graph.DefaultDependencyNode
import org.sonatype.aether.util.graph.PreorderNodeListGenerator
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector
import org.sonatype.aether.util.repository.DefaultProxySelector

/**
 * An implementation of the {@link DependencyManager} interface that uses Aether, the dependency resolution
 * engine used by Maven.
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherDependencyManager implements DependencyManager {

    static final String DEFAULT_CACHE = "${System.getProperty('user.home')}/.m2/repository"
    static final Map<String, List<String>> SCOPE_MAPPINGS = [compile:['compile'],
                                                             optional:['optional'],
                                                             runtime:['compile', 'optional','runtime'],
                                                             test:['compile','provided', 'runtime', 'optional','test'],
                                                             provided:['provided']]


    protected Dependency jvmAgent
    protected DependencyReport jvmAgentReport
    protected List<Dependency> dependencies = []
    protected Set <org.codehaus.groovy.grails.resolve.Dependency> grailsPluginDependencies = []
    protected List<Dependency> buildDependencies = []
    protected Map<String, List<org.codehaus.groovy.grails.resolve.Dependency>> grailsDependenciesByScope = [:].withDefault { [] }
    protected Map<String, List<org.codehaus.groovy.grails.resolve.Dependency>> grailsPluginDependenciesByScope = [:].withDefault { [] }
    protected List<org.codehaus.groovy.grails.resolve.Dependency> grailsDependencies = []
    protected List<RemoteRepository> repositories = []
    String cacheDir
    String basedir = new File('.')
    Settings settings
    String checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_IGNORE
    boolean readPom
    boolean defaultDependenciesProvided
    boolean java5compatible

    Map<String, Closure> inheritedDependencies = [:]

    private MavenRepositorySystemSession session  = new MavenRepositorySystemSession()

    private RepositorySystem repositorySystem

    private SettingsBuilder settingsBuilder

    private ModelBuilder modelBuilder

    GrailsConsoleLoggerManager loggerManager

    /**
     * Whether to include the javadoc
     */
    boolean includeJavadoc
    /**
     * Whether to include the source
     */
    boolean includeSource

    AetherDependencyManager() {

        final currentThread = Thread.currentThread()
        final contextLoader = currentThread.getContextClassLoader()

        try {
            currentThread.setContextClassLoader(getClass().getClassLoader())
            final container = new DefaultPlexusContainer()
            loggerManager = new GrailsConsoleLoggerManager()
            container.setLoggerManager(loggerManager)

            repositorySystem = container.lookup(RepositorySystem.class)
            settingsBuilder = container.lookup(SettingsBuilder.class)
            modelBuilder = container.lookup(ModelBuilder.class)
        }
        finally {
            currentThread.setContextClassLoader(contextLoader)
        }
    }

    MavenRepositorySystemSession getSession() {
        return session
    }

    void setJvmAgent(Dependency jvmAgent) {
        this.jvmAgent = jvmAgent
    }

    void produceReport(String scope) {
        final desc = BuildSettings.SCOPE_TO_DESC[scope]
        if (desc) {
            reportOnScope(scope, desc)
        }
        else {
            produceReport()
        }
    }

    /**
     * Produces a report printed to System.out of the dependency graph
     */
    GPathResult downloadPluginList(File localFile) {
        return DependencyManagerUtils.downloadPluginList(localFile)
    }

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

    protected void reportOnScope(String scope, String desc) {
        DependencyNode root = collectDependencies(scope)
        AetherGraphNode node = resolveToGraphNode(root, scope)

        def nlg = new PreorderNodeListGenerator()
        root.accept nlg

        def renderer = new SimpleGraphRenderer(scope, "$desc (total: ${nlg.files.size()})")
        renderer.render(node)
    }

    AetherGraphNode resolveToGraphNode(String scope) {
        DependencyNode root = collectDependencies(scope)
        return resolveToGraphNode(root, scope)
    }

    protected AetherGraphNode resolveToGraphNode(DependencyNode root, String scope) {
        DependencyResult result

        List<Artifact> unresolved = []
        try {
            result = resolveToResult(root, scope)
        } catch (DependencyResolutionException e) {
            result = e.result
            final cause = e.cause
            if (cause instanceof ArtifactResolutionException) {
                List<ArtifactResult> results = cause.getResults()
                for (ArtifactResult r in results) {
                    if (!r.isResolved()) {
                        if (r.artifact) {
                            unresolved << r.artifact
                        } else {
                            if (r.exceptions) {
                                def ex = r.exceptions[0]
                                if (ex instanceof ArtifactTransferException) {
                                    if (ex.getArtifact()) {
                                        unresolved << ex.getArtifact()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            GrailsConsole.instance.error("${e.message} (scope: $scope)", e)
        }
        AetherGraphNode node = new AetherGraphNode(result, unresolved)
        node
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getPluginDependencies() {
        return grailsPluginDependencies
    }

    ExclusionDependencySelector exclusionDependencySelector
    /**
     * Parse the dependency definition DSL
     *
     * @param callable The DSL definition
     */
    void parseDependencies(Closure callable) {
        AetherDsl dsl = new AetherDsl(this)
        dsl.session = session
        callable.delegate = dsl
        callable.call()
    }

    List<RemoteRepository> getRepositories() {
        return repositories
    }

    void setRepositories(List<RemoteRepository> repositories) {
        this.repositories = repositories
    }

    void setSettings(Settings settings) {
        this.settings = settings
    }

    @Override
    DependencyReport resolveAgent() {
        if(jvmAgent && !jvmAgentReport) {
            jvmAgentReport = resolve('agent')
        }
        return jvmAgentReport
    }

    /**
     * Resolve dependencies for the given scope
     * @param scope The scope (defaults to 'runtime')
     * @return A DependencyReport instance
     */
    DependencyReport resolve(String scope = "runtime") {
        DependencyNode root
        try {
            root = collectDependencies(scope)
            DependencyResult results = resolveToResult(root, scope)

            if (includeSource || includeJavadoc) {

                def attachmentRequests = new ArrayList<ArtifactRequest>()
                for(ArtifactResult ar in results.artifactResults) {

                    final artifact = ar.artifact
                    attachmentRequests << new ArtifactRequest(artifact, repositories, null)
                    if (includeJavadoc) {
                        attachmentRequests << new ArtifactRequest(new DefaultArtifact(
                            artifact.groupId, artifact.artifactId, "javadoc", artifact.extension, artifact.version), repositories, null)
                    }
                    if (includeJavadoc) {
                        attachmentRequests << new ArtifactRequest(new DefaultArtifact(
                            artifact.groupId, artifact.artifactId, "sources", artifact.extension, artifact.version), repositories, null)
                    }
                }

                try {
                    final allArtifacts = repositorySystem.resolveArtifacts(session, attachmentRequests)

                    return new AetherArtifactResultReport(scope, allArtifacts)
                } catch (ArtifactResolutionException are) {
                    return new AetherArtifactResultReport(scope, are.results)
                }
            }
        } catch (DependencyResolutionException e) {
            boolean failWithException = true
            if (e.cause instanceof ArtifactResolutionException) {
                ArtifactResolutionException are = (ArtifactResolutionException) e.cause
                final unresolved = are.results.findAll { ArtifactResult ar -> !ar.resolved }
                failWithException = !unresolved.every { ArtifactResult ar ->
                    ar.request.artifact.classifier == 'javadoc' || ar.request.artifact.classifier == 'sources'
                }
            }
            if (root) {
                def nlg = new PreorderNodeListGenerator()
                root.accept nlg

                if (failWithException) {
                    return new AetherDependencyReport(nlg, scope, e)
                }
                else {
                    return new AetherDependencyReport(nlg, scope)
                }
            }
            else {
                root = e.result.root
                def nlg = new PreorderNodeListGenerator()
                root.accept nlg

                return new AetherDependencyReport(nlg, scope, e)
            }
        } catch (DependencyCollectionException e) {
            root = e.result.root
            def nlg = new PreorderNodeListGenerator()
            root.accept nlg

            return new AetherDependencyReport(nlg, scope, e)
        }

        def nlg = new PreorderNodeListGenerator()
        root.accept nlg

        return new AetherDependencyReport(nlg, scope)
    }

    protected void addAttachments(DependencyNode root, String classifier) {
        def children = new ArrayList<DependencyNode>(root.children)

        for (DependencyNode child in children) {
            final artifact = child.dependency.artifact
            def sourceArtifact = new DefaultArtifact(artifact.groupId, artifact.artifactId, classifier, artifact.extension, artifact.version)
            root.children << new DefaultDependencyNode(child.dependency.setArtifact(sourceArtifact))
            addAttachments(child, classifier)
        }
    }

    protected DependencyResult resolveToResult(DependencyNode node, String scope) {
        def dependencyRequest = new DependencyRequest(node, null)

        if (scope && scope != 'build' && scope != 'agent') {
            final includedScopes = SCOPE_MAPPINGS[scope]
            if (includedScopes) {
                final filter = new ScopeDependencyFilter(includedScopes, [])
                dependencyRequest.setFilter(filter)
            }
        }
        DependencyResult resolveResult = repositorySystem.resolveDependencies(session, dependencyRequest)
        resolveResult
    }

    protected DependencyNode collectDependencies(String scope) {
        SettingsBuildingResult result = settingsBuilder.build(new DefaultSettingsBuildingRequest())
        settings = result.getEffectiveSettings()
        final proxyHost = System.getProperty("http.proxyHost")
        final proxyPort = System.getProperty("http.proxyPort")
        if (proxyHost && proxyPort) {
            final proxyUser = System.getProperty("http.proxyUserName")
            final proxyPass = System.getProperty("http.proxyPassword")
            addProxy(proxyHost, proxyPort, proxyUser, proxyPass, System.getProperty('http.nonProxyHosts'))
        }

        session.setOffline(settings.offline)
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            void transferStarted(TransferEvent event) throws TransferCancelledException {
                GrailsConsole.instance.updateStatus("Downloading: $event.resource.resourceName")
            }
        })
        session.setChecksumPolicy(checksumPolicy)

        LocalRepository localRepo = new LocalRepository(cacheDir ?: settings.localRepository ?: DEFAULT_CACHE)
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo))

        if (readPom) {
            def pomFile = new File(basedir, "pom.xml")

            final modelRequest = new DefaultModelBuildingRequest()
            modelRequest.setPomFile(pomFile)
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelRequest)
            final mavenDependencies = modelBuildingResult.getEffectiveModel().getDependencies()
            for (org.apache.maven.model.Dependency md in mavenDependencies) {
                final dependency = new Dependency(new DefaultArtifact(md.groupId, md.artifactId, md.classifier, md.type, md.version), md.scope)
                addDependency(dependency)
            }
        }

        def collectRequest = new CollectRequest()
        if (scope == 'build') {
            collectRequest.setDependencies(buildDependencies)
        }
        else if(scope == 'agent') {
            collectRequest.setDependencies([jvmAgent])
        }
        else {
            collectRequest.setDependencies(dependencies)
        }

        collectRequest.setRepositories(repositories)

        return repositorySystem.collectDependencies(session, collectRequest).getRoot()
    }

    Proxy addProxy(String proxyHost, String proxyPort, String proxyUser, String proxyPass, String nonProxyHosts) {
        Proxy proxy
        if (proxyHost && proxyPort ) {
            if (proxyUser && proxyPass) {
                proxy = new Proxy("http", proxyHost, proxyPort.toInteger(), new Authentication(proxyUser, proxyPass))
            } else {

                proxy = new Proxy("http", proxyHost, proxyPort.toInteger(), null)
            }
        }
        if (proxy) {
            final selector = session.getProxySelector()
            if (selector instanceof DefaultProxySelector) {
                selector.add(proxy, nonProxyHosts)
            }
        }

        return proxy
    }

    void addDependency(Dependency dependency, DependencyConfiguration configuration = null) {
        org.codehaus.groovy.grails.resolve.Dependency grailsDependency = createGrailsDependency(dependency, configuration)
        grailsDependencies << grailsDependency
        grailsDependenciesByScope[dependency.scope] << grailsDependency
        final aetherDependencies = dependencies
        aetherDependencies << dependency
        if (dependency.artifact.groupId == 'org.grails.plugins' || dependency.artifact.properties.extension == 'zip') {
            grailsPluginDependencies << grailsDependency
            grailsPluginDependenciesByScope[dependency.scope] << grailsDependency
        }
    }

    protected org.codehaus.groovy.grails.resolve.Dependency createGrailsDependency(Dependency dependency, DependencyConfiguration configuration = null) {
        Artifact artifact = dependency.artifact
        final grailsDependency = new org.codehaus.groovy.grails.resolve.Dependency(artifact.groupId, artifact.artifactId, artifact.version)
        if (configuration) {
            grailsDependency.transitive = configuration.transitive
            grailsDependency.exported = configuration.exported
        }
        for(Exclusion e in dependency.exclusions) {
            grailsDependency.exclude(e.groupId, e.artifactId)
        }
        return grailsDependency
    }

    protected void includeJavadocAndSourceIfNecessary(List<Dependency> aetherDependencies, Dependency dependency) {
        final artifact = dependency.artifact
        if (includeJavadoc) {
            def javadocArtifact = new DefaultArtifact(artifact.groupId, artifact.artifactId, "javadoc", artifact.extension, artifact.version)
            aetherDependencies << dependency.setArtifact(javadocArtifact)
        }
        if (includeSource) {
            def javadocArtifact = new DefaultArtifact(artifact.groupId, artifact.artifactId, "sources", artifact.extension, artifact.version)
            aetherDependencies << dependency.setArtifact(javadocArtifact)
        }
    }

    void addBuildDependency(org.codehaus.groovy.grails.resolve.Dependency dependency) {
        Collection<Exclusion> exclusions = new ArrayList<>()
        for( exc in dependency.excludes) {
            exclusions << new Exclusion(exc.group, exc.name, "*", "*")
        }
        final mavenDependency = new Dependency(new DefaultArtifact(dependency.pattern), "compile", false, exclusions)
        grailsDependencies << dependency
        grailsDependenciesByScope["build"] << dependency
        buildDependencies << mavenDependency
        if (dependency.group == 'org.grails.plugins' || dependency.properties.extension == 'zip') {
            grailsPluginDependencies << dependency
            grailsPluginDependenciesByScope["build"] << dependency
        }
    }

    void addBuildDependency(Dependency dependency, DependencyConfiguration configuration = null) {
        final grailsDependency = createGrailsDependency(dependency,configuration
        )
        grailsDependencies << grailsDependency
        grailsDependenciesByScope["build"] << grailsDependency
        buildDependencies << dependency
        if (dependency.artifact.groupId == 'org.grails.plugins' || dependency.artifact.properties.extension == 'zip') {
            grailsPluginDependencies << grailsDependency
            grailsPluginDependenciesByScope["build"] << grailsDependency
        }
    }

    void addDependency(org.codehaus.groovy.grails.resolve.Dependency dependency, String scope, ExclusionDependencySelector exclusionDependencySelector = null) {
        Collection<Exclusion> exclusions = new ArrayList<>()
        for( exc in dependency.excludes) {
            exclusions << new Exclusion(exc.group, exc.name, "*", "*")
        }
        final mavenDependency = new Dependency(new DefaultArtifact(dependency.pattern), scope, false, exclusions)

        if (exclusionDependencySelector == null || exclusionDependencySelector.selectDependency(mavenDependency)) {
            grailsDependencies << dependency
            grailsDependenciesByScope[scope] << dependency
            dependencies << mavenDependency
            if (isGrailsPlugin(dependency)) {
                grailsPluginDependencies.add dependency
                grailsPluginDependenciesByScope[scope] << dependency
            }
        }
    }

    protected boolean isGrailsPlugin(org.codehaus.groovy.grails.resolve.Dependency dependency) {
        dependency.group == 'org.grails.plugins' || dependency.properties.extension == 'zip'
    }

    @Override
    DependencyManager createCopy(BuildSettings buildSettings = null) {
        AetherDependencyManager dependencyManager = new AetherDependencyManager()
        dependencyManager.repositories = this.repositories
        dependencyManager.settings = this.settings
        return dependencyManager
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getApplicationDependencies() {
        return grailsDependencies.findAll{ org.codehaus.groovy.grails.resolve.Dependency d -> !d.inherited && !isGrailsPlugin(d)}.asImmutable()
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getAllDependencies() {
        return grailsDependencies.asImmutable()
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getApplicationDependencies(String scope) {
        return grailsDependenciesByScope[scope].findAll{ org.codehaus.groovy.grails.resolve.Dependency d -> !d.inherited && !isGrailsPlugin(d) }.asImmutable()
    }

    Collection<org.codehaus.groovy.grails.resolve.Dependency> getPluginDependencies(String scope) {
        return grailsPluginDependenciesByScope[scope].findAll{ org.codehaus.groovy.grails.resolve.Dependency d -> !d.inherited }.asImmutable()
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getAllDependencies(String scope) {
        return grailsDependencies[scope].asImmutable()
    }

    @Override
    ExcludeResolver getExcludeResolver() {
        return new AetherExcludeResolver(this)
    }
}
