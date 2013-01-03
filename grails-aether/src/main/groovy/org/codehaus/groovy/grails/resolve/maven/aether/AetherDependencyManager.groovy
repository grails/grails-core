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
import org.codehaus.groovy.grails.resolve.DependencyManager
import org.codehaus.groovy.grails.resolve.maven.aether.config.AetherDsl
import org.codehaus.plexus.DefaultPlexusContainer
import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.collection.CollectRequest
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.DependencyNode
import org.sonatype.aether.graph.Exclusion
import org.sonatype.aether.repository.LocalRepository
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.DependencyRequest
import org.sonatype.aether.resolution.DependencyResult
import org.sonatype.aether.transfer.AbstractTransferListener
import org.sonatype.aether.transfer.TransferCancelledException
import org.sonatype.aether.transfer.TransferEvent
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.filter.ScopeDependencyFilter
import org.sonatype.aether.util.graph.PreorderNodeListGenerator
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector

/**
 * An implementation of the {@link DependencyManager} interface that uses Aether, the dependency resolution
 * engine used by Maven.
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherDependencyManager implements DependencyManager{

    static final String DEFAULT_CACHE = "${System.getProperty('user.home')}/.m2/repository"

    static final Map<String, List<String>> SCOPE_MAPPINGS = [runtime:['compile', 'optional','runtime'],
                                                             test:['compile', 'runtime', 'optional','test'],
                                                             provided:['provided']];
    private List<Dependency> dependencies = []
    private Map<String, List<org.codehaus.groovy.grails.resolve.Dependency>> grailsDependenciesByScope = [:].withDefault { [] }
    private List<org.codehaus.groovy.grails.resolve.Dependency> grailsDependencies = []
    private List<RemoteRepository> repositories = []
    String cacheDir
    String basedir = new File('.')
    Settings settings
    boolean readPom
    boolean defaultDependenciesProvided
    boolean java5compatible

    Map<String, Closure> inheritedDependencies = [:]

    private MavenRepositorySystemSession session  = new MavenRepositorySystemSession()


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

        final container = new DefaultPlexusContainer()
        final system = container.lookup( RepositorySystem.class );
        final settingsBuilder = container.lookup( SettingsBuilder.class )

        SettingsBuildingResult result = settingsBuilder.build(new DefaultSettingsBuildingRequest())
        settings = result.getEffectiveSettings()




        session.setOffline(settings.offline)
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            void transferStarted(TransferEvent event) throws TransferCancelledException {
                GrailsConsole.instance.updateStatus("Downloading: $event.resource.resourceName")
            }
        })

        LocalRepository localRepo = new LocalRepository(cacheDir ?: settings.localRepository ?: DEFAULT_CACHE)
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( localRepo ) );

        if (readPom) {
            def pomFile = new File(basedir, "pom.xml")
            final modelBuilder = container.lookup( ModelBuilder.class )
            final modelRequest = new DefaultModelBuildingRequest()
            modelRequest.setPomFile(pomFile)
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelRequest)
            final mavenDependencies = modelBuildingResult.getRawModel().getDependencies()
            for(org.apache.maven.model.Dependency md in mavenDependencies) {
                final dependency = new Dependency(new DefaultArtifact(md.groupId, md.artifactId, md.classifier, md.type, md.version), md.scope)
                addDependency(dependency)
            }
        }


        def collectRequest = new CollectRequest();
        collectRequest.setDependencies(dependencies)
        collectRequest.setRepositories(repositories)

        DependencyNode node = system.collectDependencies( session, collectRequest ).getRoot()

        def dependencyRequest = new DependencyRequest( node, null )

        if (scope) {
            final includedScopes = SCOPE_MAPPINGS[scope]
            if (includedScopes) {
                final filter = new ScopeDependencyFilter(includedScopes, [])
                dependencyRequest.setFilter(filter)
            }

        }
        DependencyResult resolveResult = system.resolveDependencies(session, dependencyRequest)

        def nlg = new PreorderNodeListGenerator()
        node.accept nlg


        return new AetherDependencyReport(nlg, scope);
    }

    public void addDependency(Dependency dependency) {
        Artifact artifact = dependency.artifact
        final grailsDependency = new org.codehaus.groovy.grails.resolve.Dependency(artifact.groupId, artifact.artifactId, artifact.version)
        grailsDependencies << grailsDependency
        grailsDependenciesByScope[dependency.scope] << grailsDependency
        dependencies << dependency
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
        return grailsDependencies.findAll{ org.codehaus.groovy.grails.resolve.Dependency d -> !d.inherited }
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getAllDependencies() {
        return grailsDependencies
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getApplicationDependencies(String scope) {
        return grailsDependenciesByScope[scope].findAll{ org.codehaus.groovy.grails.resolve.Dependency d -> !d.inherited }
    }

    @Override
    Collection<org.codehaus.groovy.grails.resolve.Dependency> getAllDependencies(String scope) {
        return grailsDependencies[scope]
    }
}

