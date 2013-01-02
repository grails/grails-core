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
import groovy.transform.CompileStatic
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingResult
import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest
import org.apache.maven.settings.building.SettingsBuilder
import org.apache.maven.settings.building.SettingsBuildingResult
import org.codehaus.groovy.grails.resolve.maven.aether.config.AetherDsl
import org.codehaus.plexus.DefaultPlexusContainer
import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.collection.CollectRequest
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.DependencyNode
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

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherDependencyManager {

    static final String DEFAULT_CACHE = "${System.getProperty('user.home')}/.m2/repository"

    static final Map<String, List<String>> SCOPE_MAPPINGS = [runtime:['compile', 'optional','runtime'],
                                                             test:['compile', 'runtime', 'optional','test'],
                                                             provided:['provided']];
    List<Dependency> dependencies = []
    List<RemoteRepository> repositories = []
    String cacheDir
    String basedir = new File('.')
    Settings settings
    boolean readPom
    boolean defaultDependenciesProvided
    boolean java5compatible

    Map<String, Closure> inheritedDependencies = [:]

    private MavenRepositorySystemSession session  = new MavenRepositorySystemSession()


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

    /**
     * Resolve dependencies for the given scope
     * @param scope The scope (defaults to 'runtime')
     * @return A DependencyReport instance
     */
    DependencyReport resolveDependencies(String scope = "runtime") {

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
                dependencies << new Dependency(new DefaultArtifact(md.groupId, md.artifactId, md.classifier, md.type, md.version), md.scope)
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


        return new DependencyReport(nlg);
    }

}

