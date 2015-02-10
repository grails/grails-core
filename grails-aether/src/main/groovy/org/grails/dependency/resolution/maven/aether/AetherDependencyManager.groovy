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
package org.grails.dependency.resolution.maven.aether

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingResult
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest
import org.apache.maven.settings.building.SettingsBuilder
import org.apache.maven.settings.building.SettingsBuildingResult
import org.grails.io.support.SpringIOUtils
import org.grails.dependency.resolution.DependencyManager
import org.grails.dependency.resolution.DependencyManagerUtils
import org.grails.dependency.resolution.DependencyReport
import org.grails.dependency.resolution.ExcludeResolver
import org.grails.dependency.resolution.GrailsCoreDependencies
import org.grails.dependency.resolution.maven.aether.config.AetherDsl
import org.grails.dependency.resolution.maven.aether.config.DependencyConfiguration
import org.grails.dependency.resolution.maven.aether.support.GrailsConsoleLoggerManager
import org.grails.dependency.resolution.maven.aether.support.GrailsHomeWorkspaceReader
import org.grails.dependency.resolution.maven.aether.support.GrailsModelResolver
import org.grails.dependency.resolution.maven.aether.support.MultipleTopLevelJavaScopeSelector
import org.grails.dependency.resolution.maven.aether.support.ScopeAwareNearestVersionSelector
import org.grails.dependency.resolution.reporting.SimpleGraphRenderer
import org.codehaus.plexus.DefaultPlexusContainer
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.DependencyCollectionException
import org.eclipse.aether.collection.DependencyGraphTransformer
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.DefaultDependencyNode
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.Proxy
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.ArtifactTransferException
import org.eclipse.aether.transfer.TransferCancelledException
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.filter.ScopeDependencyFilter
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector
import org.eclipse.aether.util.repository.DefaultMirrorSelector
import org.eclipse.aether.util.repository.DefaultProxySelector

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
    static final Map<String, List<String>> SCOPE_MAPPINGS = [compile: ['compile'],
        optional: ['optional'],
        runtime: [
            'compile',
            'optional',
            'runtime'
        ],
        test: [
            'compile',
            'provided',
            'runtime',
            'optional',
            'test'
        ],
        provided: ['provided']]


    protected Dependency jvmAgent
    protected DependencyReport jvmAgentReport
    protected List<Dependency> dependencies = []
    protected List<Dependency> managedDependencies = []
    protected Set<org.grails.dependency.resolution.Dependency> grailsPluginDependencies = []
    protected List<Dependency> buildDependencies = []
    protected Map<String, List<org.grails.dependency.resolution.Dependency>> grailsDependenciesByScope = [:].withDefault { [] }
    protected Map<String, List<org.grails.dependency.resolution.Dependency>> grailsPluginDependenciesByScope = [:].withDefault { [] }
    protected List<org.grails.dependency.resolution.Dependency> grailsDependencies = []
    protected List<RemoteRepository> repositories = []
    String cacheDir
    String basedir = new File('.')
    Settings settings
    String checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_IGNORE
    boolean readPom
    boolean defaultDependenciesProvided
    boolean offline
    boolean java5compatible

    Map<String, Closure> inheritedDependencies = [:]

    private DefaultRepositorySystemSession session = (DefaultRepositorySystemSession) MavenRepositorySystemUtils.newSession()

    private RepositorySystem repositorySystem

    private SettingsBuilder settingsBuilder

    private ModelBuilder modelBuilder

    GrailsConsoleLoggerManager loggerManager

    GrailsCoreDependencies coreDependencies

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

            settingsBuilder = container.lookup(SettingsBuilder)
            modelBuilder = container.lookup(ModelBuilder)
            DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator()
            locator.addService(RepositoryConnectorFactory, BasicRepositoryConnectorFactory)
            locator.addService(TransporterFactory, FileTransporterFactory)
            locator.addService(TransporterFactory, HttpTransporterFactory)

            repositorySystem = locator.getService(RepositorySystem)

            session.setAuthenticationSelector(new DefaultAuthenticationSelector())
            DependencyGraphTransformer transformer =
                    new ConflictResolver(new ScopeAwareNearestVersionSelector(), new MultipleTopLevelJavaScopeSelector(),
                    new SimpleOptionalitySelector(), new JavaScopeDeriver())

            session.setDependencyGraphTransformer(new ChainedDependencyGraphTransformer(transformer, new JavaDependencyContextRefiner()))

            session.setProxySelector(new DefaultProxySelector())
            session.setMirrorSelector(new DefaultMirrorSelector())
            session.setWorkspaceReader(new GrailsHomeWorkspaceReader())
        }
        finally {
            currentThread.setContextClassLoader(contextLoader)
        }
    }

    RepositorySystemSession getSession() {
        return session
    }

    void setJvmAgent(Dependency jvmAgent) {
        this.jvmAgent = jvmAgent
    }

    void produceReport(String scope) {
        final desc = SCOPE_TO_DESC[scope]
        if (desc) {
            reportOnScope(scope, desc)
        } else {
            produceReport()
        }
    }

    /**
     * Produces a report printed to System.out of the dependency graph
     */
    GPathResult downloadPluginList(File localFile) {
        return DependencyManagerUtils.downloadPluginList(localFile)
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    GPathResult downloadPluginInfo(String pluginName, String pluginVersion) {
        AetherDependencyManager newDependencyManager = (AetherDependencyManager) createCopy()

        newDependencyManager.parseDependencies {
            dependencies {
                compile group: "org.grails.plugins",
                name: pluginName,
                version: pluginVersion ?: 'RELEASE',
                classifier: "plugin",
                extension: 'xml'
            }
        }

        final report = newDependencyManager.resolve()
        if (report.allArtifacts) {
            File pluginXml = report.allArtifacts.find { File f -> f.name.endsWith('-plugin.xml') }

            return SpringIOUtils.createXmlSlurper().parse(pluginXml)
        }

        return null
    }

    void produceReport() {
        // build scope
        reportOnScope(BUILD_SCOPE, BUILD_SCOPE_DESC)
        // provided scope
        reportOnScope(PROVIDED_SCOPE, PROVIDED_SCOPE_DESC)
        // compile scope
        reportOnScope(COMPILE_SCOPE, COMPILE_SCOPE_DESC)
        // runtime scope
        reportOnScope(RUNTIME_SCOPE, RUNTIME_SCOPE_DESC)
        // test scope
        reportOnScope(TEST_SCOPE, TEST_SCOPE_DESC)
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
    Collection<org.grails.dependency.resolution.Dependency> getPluginDependencies() {
        return grailsPluginDependencies
    }

    ExclusionDependencySelector exclusionDependencySelector
    /**
     * Parse the dependency definition DSL
     *
     * @param callable The DSL definition
     */
    void parseDependencies(@DelegatesTo(AetherDsl) Closure callable) {
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
        if (jvmAgent && !jvmAgentReport) {
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
                for (ArtifactResult ar in results.artifactResults) {

                    final artifact = ar.artifact
                    attachmentRequests << new ArtifactRequest(artifact, repositories, null)
                    if (includeJavadoc) {
                        attachmentRequests << new ArtifactRequest(new DefaultArtifact(
                                artifact.groupId, artifact.artifactId, "javadoc", artifact.extension, artifact.version), repositories, null)
                    }
                    if (includeSource) {
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
                } else {
                    return new AetherDependencyReport(nlg, scope)
                }
            } else {
                root = e.result.root
                def nlg = new PreorderNodeListGenerator()
                root.accept nlg

                return new AetherDependencyReport(nlg, scope, e)
            }
        } catch (DependencyCollectionException e) {
            root = e.result.root
            def nlg = new PreorderNodeListGenerator()
            root.accept nlg

            return new AetherDependencyReport(nlg, scope, e.cause ?: e)
        }

        def nlg = new PreorderNodeListGenerator()
        root.accept nlg

        return new AetherDependencyReport(nlg, scope)
    }

    @Override
    public DependencyReport resolveDependency(org.grails.dependency.resolution.Dependency dependency) {
        DefaultDependencyNode node = new DefaultDependencyNode(new DefaultArtifact(dependency.group, dependency.name, dependency.classifier, dependency.extension, dependency.version))

        DependencyResult resolveResult = resolveToResult(node, null)

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator()
        resolveResult.getRoot()?.accept(nlg)

        return new AetherDependencyReport(nlg, null)
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
        settings.offline = offline
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
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo))

        if (readPom) {
            def pomFile = new File(basedir, "pom.xml")

            final modelRequest = new DefaultModelBuildingRequest()
            modelRequest.setPomFile(pomFile)
            modelRequest.setSystemProperties(System.properties)
            modelRequest.setModelResolver(new GrailsModelResolver(repositorySystem, session, repositories))
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelRequest)
            final mavenDependencies = modelBuildingResult.getEffectiveModel().getDependencies()
            for (org.apache.maven.model.Dependency md in mavenDependencies) {
                final artifact = new DefaultArtifact(md.groupId, md.artifactId, md.classifier, md.type, md.version)
                final mavenExclusions = md.getExclusions()
                Set<Exclusion> exclusions = []
                for (me in mavenExclusions) {
                    exclusions << new Exclusion(me.groupId, me.artifactId, DependencyConfiguration.WILD_CARD, DependencyConfiguration.WILD_CARD)
                }
                final dependency = new Dependency(artifact, md.scope, md.isOptional(), exclusions)
                addDependency(dependency)
            }
        }

        def collectRequest = new CollectRequest()

        manageDependencies(collectRequest)

        if (scope == 'build') {
            collectRequest.setDependencies(buildDependencies)
        } else if (scope == 'agent') {
            collectRequest.setDependencies([jvmAgent])
        } else {
            collectRequest.setDependencies(dependencies)
        }

        collectRequest.setRepositories(repositories)

        return repositorySystem.collectDependencies(session, collectRequest).getRoot()
    }

    protected void manageDependencies(CollectRequest collectRequest) {
        def alreadyManagedArtefacts = managedDependencies.collect { Dependency d -> d.artifact }
        if(managedDependencies) {
            collectRequest.setManagedDependencies(managedDependencies)
        }
        if(coreDependencies) {

            // ensure correct version of Spring is used
            for (springDep in [
                'spring-orm',
                'spring-beans',
                'spring-core',
                'spring-tx',
                'spring-context',
                'spring-context-support',
                'spring-bean',
                'spring-web',
                'spring-webmvc',
                'spring-jms',
                'spring-aop',
                'spring-jdbc',
                'spring-expression',
                'spring-jdbc',
                'spring-test'
            ]) {
                String id = "org.springframework:${springDep}:${coreDependencies.springVersion}"
                registerManagedDependency(collectRequest, id, alreadyManagedArtefacts)
            }
            // ensure correct version of Groovy is used
            registerManagedDependency(collectRequest, "org.codehaus.groovy:groovy-all:${coreDependencies.groovyVersion}", alreadyManagedArtefacts)
            registerManagedDependency(collectRequest, "org.codehaus.groovy:groovy:${coreDependencies.groovyVersion}", alreadyManagedArtefacts)

            // ensure the correct versions of Grails jars are used
            for (grailsDep in [
                'grails-core',
                'grails-bootstrap',
                'grails-web',
                'grails-async',
                'grails-test'
            ]) {
                String id = "org.grails:${grailsDep}:${coreDependencies.grailsVersion}"
                registerManagedDependency(collectRequest, id,alreadyManagedArtefacts)
            }
            for(org.grails.dependency.resolution.Dependency d in coreDependencies.compileDependencies) {
                if(d.group == 'org.grails') {
                    registerManagedDependency(collectRequest, d.pattern,alreadyManagedArtefacts)
                }
            }
        }

    }
    Proxy addProxy(String proxyHost, String proxyPort, String proxyUser, String proxyPass, String nonProxyHosts) {
        Proxy proxy
        if (proxyHost && proxyPort) {
            if (proxyUser && proxyPass) {
                proxy = new Proxy("http", proxyHost, proxyPort.toInteger(), new AuthenticationBuilder().addUsername(proxyUser).addPassword(proxyPass).build())
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

    /**
     * Adds a dependency to be used for dependency management. See  http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management
     *
     * @param dependency The dependency to add
     * @param configuration The configuration
     */
    void addManagedDependency(Dependency dependency) {
        managedDependencies << dependency
    }

    /**
     * Adds a dependency
     *
     * @param dependency The dependency to add
     * @param configuration The depdency configuration
     */
    void addDependency(Dependency dependency, DependencyConfiguration configuration = null) {
        org.grails.dependency.resolution.Dependency grailsDependency = createGrailsDependency(dependency, configuration)
        grailsDependencies << grailsDependency
        grailsDependenciesByScope[dependency.scope] << grailsDependency
        final aetherDependencies = dependencies
        aetherDependencies << dependency
        if (dependency.artifact.groupId == 'org.grails.plugins' || dependency.artifact.extension == 'zip') {
            grailsPluginDependencies << grailsDependency
            grailsPluginDependenciesByScope[dependency.scope] << grailsDependency
        }
    }

    protected org.grails.dependency.resolution.Dependency createGrailsDependency(Dependency dependency, DependencyConfiguration configuration = null) {
        Artifact artifact = dependency.artifact
        final grailsDependency = new org.grails.dependency.resolution.Dependency(artifact.groupId, artifact.artifactId, artifact.version)
        grailsDependency.extension = artifact.extension
        grailsDependency.classifier=artifact.classifier
        if (configuration) {
            grailsDependency.transitive = configuration.transitive
            grailsDependency.exported = configuration.exported
        }
        for (Exclusion e in dependency.exclusions) {
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

    void addBuildDependency(org.grails.dependency.resolution.Dependency dependency, ExclusionDependencySelector exclusionDependencySelector = null) {
        Collection<Exclusion> exclusions = new ArrayList<>()
        for (exc in dependency.excludes) {
            exclusions << new Exclusion(exc.group, exc.name, "*", "*")
        }

        final mavenDependency = new Dependency(new DefaultArtifact(dependency.pattern), "compile", false, exclusions)
        if (exclusionDependencySelector == null || exclusionDependencySelector.selectDependency(mavenDependency)) {
            grailsDependencies << dependency
            grailsDependenciesByScope["build"] << dependency
            buildDependencies << mavenDependency
            if (dependency.group == 'org.grails.plugins' || dependency.extension == 'zip') {
                grailsPluginDependencies << dependency
                grailsPluginDependenciesByScope["build"] << dependency
            }
        }
    }

    void addBuildDependency(Dependency dependency, DependencyConfiguration configuration = null) {

        final grailsDependency = createGrailsDependency(dependency, configuration)
        grailsDependencies << grailsDependency
        grailsDependenciesByScope["build"] << grailsDependency
        buildDependencies << dependency
        if (dependency.artifact.groupId == 'org.grails.plugins' || dependency.artifact.properties.extension == 'zip') {
            grailsPluginDependencies << grailsDependency
            grailsPluginDependenciesByScope["build"] << grailsDependency
        }
    }

    void addDependency(org.grails.dependency.resolution.Dependency dependency, String scope, ExclusionDependencySelector exclusionDependencySelector = null) {
        Collection<Exclusion> exclusions = new ArrayList<>()
        for (exc in dependency.excludes) {
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

    protected boolean isGrailsPlugin(org.grails.dependency.resolution.Dependency dependency) {
        dependency.group == 'org.grails.plugins' || dependency.extension == 'zip'
    }

    DependencyManager createCopy() {
        AetherDependencyManager dependencyManager = new AetherDependencyManager()
        dependencyManager.repositories = this.repositories
        dependencyManager.settings = this.settings
        return dependencyManager
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<org.grails.dependency.resolution.Dependency> getApplicationDependencies() {
        return grailsDependencies.findAll { org.grails.dependency.resolution.Dependency d -> !d.inherited && !isGrailsPlugin(d) }.asImmutable()
    }

    @Override
    Collection<org.grails.dependency.resolution.Dependency> getAllDependencies() {
        return grailsDependencies.asImmutable()
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Collection<org.grails.dependency.resolution.Dependency> getApplicationDependencies(String scope) {
        return grailsDependenciesByScope[scope].findAll { org.grails.dependency.resolution.Dependency d -> !d.inherited && !isGrailsPlugin(d) }.asImmutable()
    }

    Collection<org.grails.dependency.resolution.Dependency> getPluginDependencies(String scope) {
        return grailsPluginDependenciesByScope[scope].findAll { org.grails.dependency.resolution.Dependency d -> !d.inherited }.asImmutable()
    }

    @Override
    Collection<org.grails.dependency.resolution.Dependency> getAllDependencies(String scope) {
        return grailsDependencies[scope].asImmutable()
    }

    @Override
    ExcludeResolver getExcludeResolver() {
        return new AetherExcludeResolver(this)
    }

    private CollectRequest registerManagedDependency(CollectRequest collectRequest, String id, Collection<Artifact> alreadyManagedArtefacts) {
        def artifact = new DefaultArtifact(id)
        if(!alreadyManagedArtefacts.contains(artifact)) {
            collectRequest.addManagedDependency(new Dependency(artifact, null))
        }
    }
}
