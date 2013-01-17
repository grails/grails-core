/* Copyright 2013 the original author or authors.
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
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingResult
import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest
import org.apache.maven.settings.building.SettingsBuilder
import org.apache.maven.settings.building.SettingsBuildingResult
import org.codehaus.groovy.grails.resolve.AbstractDependencyManager
import org.codehaus.groovy.grails.resolve.DependencyManager
import org.codehaus.groovy.grails.resolve.maven.aether.config.AetherDsl
import org.codehaus.groovy.grails.resolve.maven.aether.support.GrailsConsoleLoggerManager
import org.codehaus.groovy.grails.resolve.reporting.SimpleGraphRenderer
import org.codehaus.plexus.DefaultPlexusContainer
import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.collection.CollectRequest
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.DependencyNode
import org.sonatype.aether.graph.Exclusion
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.repository.LocalRepository
import org.sonatype.aether.repository.Proxy
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.repository.RepositoryPolicy
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
class AetherDependencyManager extends AbstractDependencyManager{

    static final String DEFAULT_CACHE = "${System.getProperty('user.home')}/.m2/repository"
    static final Map<String, List<String>> SCOPE_MAPPINGS = [compile:['compile'],
                                                             optional:['optional'],
                                                             runtime:['compile', 'optional','runtime'],
                                                             test:['compile','provided', 'runtime', 'optional','test'],
                                                             provided:['provided']];
    private List<Dependency> dependencies = []
    private Set <org.codehaus.groovy.grails.resolve.Dependency> grailsPluginDependencies = []
    private List<Dependency> buildDependencies = []
    private Map<String, List<org.codehaus.groovy.grails.resolve.Dependency>> grailsDependenciesByScope = [:].withDefault { [] }
    private List<org.codehaus.groovy.grails.resolve.Dependency> grailsDependencies = []
    private List<RemoteRepository> repositories = []
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

    AetherDependencyManager() {

        final currentThread = Thread.currentThread()
        final contextLoader = currentThread.getContextClassLoader()

        try {
            currentThread.setContextClassLoader(getClass().getClassLoader());
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



    void produceReport(String scope) {
        final desc = BuildSettings.SCOPE_TO_DESC[scope]
        if (desc)
            reportOnScope(scope, desc)
        else {
            produceReport()
        }
    }
    /**
     * Produces a report printed to System.out of the dependency graph
     */
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

    private void reportOnScope(String scope, String desc) {
        DependencyNode root = collectDependencies(scope)
        DependencyResult result

        List<Artifact> unresolved = []
        try {
            result = resolveToResult(root, scope)
        } catch (DependencyResolutionException e) {
            result = e.result
            final cause = e.cause
            if (cause instanceof ArtifactResolutionException) {
                List<ArtifactResult> results = cause.getResults()
                for(ArtifactResult r in results) {
                    if (!r.isResolved()) {
                        if (r.artifact)
                            unresolved << r.artifact
                        else {
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

        def nlg = new PreorderNodeListGenerator()
        root.accept nlg
        AetherGraphNode node = new AetherGraphNode(result, unresolved)

        def renderer = new SimpleGraphRenderer(scope, "$desc (total: ${nlg.files.size()})")
        renderer.render(node)
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
/**
     * Resolve dependencies for the given scope
     * @param scope The scope (defaults to 'runtime')
     * @return A DependencyReport instance
     */
    AetherDependencyReport resolve(String scope = "runtime") {


        DependencyNode node = collectDependencies(scope)

        try {
            resolveToResult(node, scope)
        } catch (org.sonatype.aether.resolution.DependencyResolutionException e) {
            def nlg = new PreorderNodeListGenerator()
            node.accept nlg

            return new AetherDependencyReport(nlg, scope, e)
        }


        def nlg = new PreorderNodeListGenerator()
        node.accept nlg


        return new AetherDependencyReport(nlg, scope);
    }

    private DependencyResult resolveToResult(DependencyNode node, String scope) {
        def dependencyRequest = new DependencyRequest(node, null)

        if (scope && scope != 'build') {
            final includedScopes = SCOPE_MAPPINGS[scope]
            if (includedScopes) {
                final filter = new ScopeDependencyFilter(includedScopes, [])
                dependencyRequest.setFilter(filter)
            }

        }
        DependencyResult resolveResult = repositorySystem.resolveDependencies(session, dependencyRequest)
        resolveResult
    }

    private DependencyNode collectDependencies(String scope) {
        SettingsBuildingResult result = settingsBuilder.build(new DefaultSettingsBuildingRequest())
        settings = result.getEffectiveSettings()
        final proxyHost = System.getProperty("http.proxyHost")
        final proxyPort = System.getProperty("http.proxyPort")
        if(proxyHost && proxyPort) {
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
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo));

        if (readPom) {
            def pomFile = new File(basedir, "pom.xml")

            final modelRequest = new DefaultModelBuildingRequest()
            modelRequest.setPomFile(pomFile)
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelRequest)
            final mavenDependencies = modelBuildingResult.getRawModel().getDependencies()
            for (org.apache.maven.model.Dependency md in mavenDependencies) {
                final dependency = new Dependency(new DefaultArtifact(md.groupId, md.artifactId, md.classifier, md.type, md.version), md.scope)
                addDependency(dependency)
            }
        }


        def collectRequest = new CollectRequest();
        if (scope == 'build')
            collectRequest.setDependencies(buildDependencies)
        else
            collectRequest.setDependencies(dependencies)

        collectRequest.setRepositories(repositories)

        DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot()
        node
    }

    public org.sonatype.aether.repository.Proxy addProxy(String proxyHost, String proxyPort, String proxyUser, String proxyPass, String nonProxyHosts) {
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

    public void addDependency(Dependency dependency) {
        Artifact artifact = dependency.artifact
        final grailsDependency = new org.codehaus.groovy.grails.resolve.Dependency(artifact.groupId, artifact.artifactId, artifact.version)
        grailsDependencies << grailsDependency
        grailsDependenciesByScope[dependency.scope] << grailsDependency
        dependencies << dependency
        if (dependency.artifact.groupId == 'org.grails.plugins' || dependency.artifact.properties.extension == 'zip') {
            grailsPluginDependencies << grailsDependency
        }
    }

    public void addBuildDependency(org.codehaus.groovy.grails.resolve.Dependency dependency) {
        Collection<Exclusion> exclusions = new ArrayList<>()
        for( exc in dependency.excludes) {
            exclusions << new Exclusion(exc.group, exc.name, "*", "*")
        }
        final mavenDependency = new org.sonatype.aether.graph.Dependency(new DefaultArtifact(dependency.pattern), "compile", false, exclusions)
        grailsDependencies << dependency
        grailsDependenciesByScope["build"] << dependency
        buildDependencies << mavenDependency
        if (dependency.group == 'org.grails.plugins' || dependency.properties.extension == 'zip') {
            grailsPluginDependencies << dependency
        }
    }

    public void addBuildDependency(Dependency dependency) {
        Artifact artifact = dependency.artifact
        final grailsDependency = new org.codehaus.groovy.grails.resolve.Dependency(artifact.groupId, artifact.artifactId, artifact.version)
        grailsDependencies << grailsDependency
        grailsDependenciesByScope["build"] << grailsDependency
        buildDependencies << dependency
        if (dependency.artifact.groupId == 'org.grails.plugins' || dependency.artifact.properties.extension == 'zip') {
            grailsPluginDependencies << grailsDependency
        }
    }

    public void addDependency(org.codehaus.groovy.grails.resolve.Dependency dependency, String scope, ExclusionDependencySelector exclusionDependencySelector = null) {
        Collection<Exclusion> exclusions = new ArrayList<>()
        for( exc in dependency.excludes) {
            exclusions << new Exclusion(exc.group, exc.name, "*", "*")
        }
        final mavenDependency = new org.sonatype.aether.graph.Dependency(new DefaultArtifact(dependency.pattern), scope, false, exclusions)

        if (exclusionDependencySelector == null || exclusionDependencySelector.selectDependency(mavenDependency)) {
            grailsDependencies << dependency
            grailsDependenciesByScope[scope] << dependency
            dependencies << mavenDependency
            if (dependency.group == 'org.grails.plugins' || dependency.properties.extension == 'zip') {
                grailsPluginDependencies.add dependency
            }
        }
    }

    @Override
    DependencyManager createCopy(BuildSettings buildSettings) {
        AetherDependencyManager dependencyManager = new AetherDependencyManager()
        dependencyManager.repositories = this.repositories
        dependencyManager.settings = this.settings
        return dependencyManager
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getApplicationDependencies() {
        return grailsDependencies.findAll{ org.codehaus.groovy.grails.resolve.Dependency d -> !d.inherited }.asImmutable()
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getAllDependencies() {
        return grailsDependencies.asImmutable()
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getApplicationDependencies(String scope) {
        return grailsDependenciesByScope[scope].findAll{ org.codehaus.groovy.grails.resolve.Dependency d -> !d.inherited }.asImmutable()
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getAllDependencies(String scope) {
        return grailsDependencies[scope].asImmutable()
    }
}

