/* Copyright 2012 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.maven

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingResult
import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest
import org.apache.maven.settings.building.SettingsBuilder
import org.apache.maven.settings.building.SettingsBuildingResult
import org.codehaus.plexus.DefaultPlexusContainer
import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.collection.CollectRequest
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.graph.DependencyNode
import org.sonatype.aether.repository.ArtifactRepository
import org.sonatype.aether.repository.LocalRepository
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.DependencyRequest
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.graph.PreorderNodeListGenerator

import java.util.regex.Pattern

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AetherDependencyManager {

    static final String DEFAULT_CACHE = "${System.getProperty('user.home')}/.m2/repository"
    List<Dependency> dependencies = []
    List<RemoteRepository> repositories = []
    String cacheDir
    String basedir = new File('.')
    Settings settings
    boolean readPom

    void parseDependencies(Closure callable) {
        AetherDsl dsl = new AetherDsl(this)
        callable.delegate = dsl
        callable.call()
    }

    DependencyReport resolveDependencies() {

        final container = new DefaultPlexusContainer()
        final system = container.lookup( RepositorySystem.class );
        final settingsBuilder = container.lookup( SettingsBuilder.class )

        SettingsBuildingResult result = settingsBuilder.build(new DefaultSettingsBuildingRequest())
        settings = result.getEffectiveSettings()




        MavenRepositorySystemSession session = new MavenRepositorySystemSession()
        session.setOffline(settings.offline)
        //session.setTransferListener(...) TODO: add transfer listener to log output

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

        system.resolveDependencies session, dependencyRequest

        def nlg = new PreorderNodeListGenerator()
        node.accept nlg


        return new DependencyReport(nlg);
    }

}
@CompileStatic
class AetherDsl {
    AetherDependencyManager dependencyManager

    AetherDsl(AetherDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    void pom(boolean b) {
        dependencyManager.readPom = b
    }
    void cacheDir(File f) {
        dependencyManager.cacheDir = f.canonicalPath
    }

    void cacheDir(String f) {
        dependencyManager.cacheDir = f
    }

    void useOrigin(boolean b) {
        GrailsConsole.getInstance().warn("BuildConifg: Method [useOrigin] not supported by Aether dependency manager")
    }
    void checksums(boolean enable) {
        GrailsConsole.getInstance().warn("BuildConifg: Method [checksums] not supported by Aether dependency manager")
    }
    void checksums(String checksumConfig) {
        GrailsConsole.getInstance().warn("BuildConifg: Method [checksums] not supported by Aether dependency manager")
    }

    void log(String level) {
        // TODO: Handle logging activation
//        switch(level) {
//            case "warn":
//            case "error":
//            case "info":
//            case "debug":
//            case "verbose":
//            default:
//        }
    }

    void repositories(Closure callable) {
        def rc = new RepositoryConfiguration()
        callable.delegate = rc
        callable.call()

        this.dependencyManager.repositories = rc.repositories
    }

    void dependencies(Closure callable) {
        def dc = new DependencyConfiguration(dependencyManager)
        callable.delegate = dc
        callable.call()
    }

    void plugins(Closure callable) {
        def dc = new PluginConfiguration(dependencyManager)
        callable.delegate = dc
        callable.call()
    }
}
@CompileStatic
class PluginConfiguration extends DependencyConfiguration {

    PluginConfiguration(AetherDependencyManager dependencyManager) {
        super(dependencyManager)
    }

    @Override
    void compile(String pattern) {
        super.compile(extractDependencyProperties(pattern))
    }

    @Override
    void runtime(String pattern) {
        super.compile(extractDependencyProperties(pattern))
    }

    @Override
    void provided(String pattern) {
        super.compile(extractDependencyProperties(pattern))
    }

    @Override
    void optional(String pattern) {
        super.compile(extractDependencyProperties(pattern))
    }

    @Override
    void test(String pattern) {
        super.compile(extractDependencyProperties(pattern))
    }

    @Override
    protected String getDefaultExtension() {
        'zip'
    }

    protected String getDefaultGroup() {
        'org.grails.plugins'
    }


}
@CompileStatic
class DependencyConfiguration {
    static final Pattern DEPENDENCY_PATTERN = Pattern.compile("([a-zA-Z0-9\\-/\\._+=]*?):([a-zA-Z0-9\\-/\\._+=]+?):([a-zA-Z0-9\\-/\\.,\\]\\[\\(\\)_+=]+)");
    public static final String SCOPE_COMPILE = "compile"
    public static final String SCOPE_RUNTIME = "runtime"
    public static final String SCOPE_PROVIDED = "provided"
    public static final String SCOPE_OPTIONAL = "optional"
    public static final String SCOPE_TEST = "test"

    AetherDependencyManager dependencyManager

    DependencyConfiguration(AetherDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    void addDependency(Dependency dependency) {
        dependencyManager.dependencies << dependency
    }

    void compile(String pattern) {
        dependencyManager.dependencies << new Dependency(new DefaultArtifact(pattern), SCOPE_COMPILE)
    }

    void compile(Map<String, String> properties) {
        addDependency(properties, SCOPE_COMPILE)
    }

    void runtime(String pattern) {
        dependencyManager.dependencies << new Dependency(new DefaultArtifact(pattern), SCOPE_RUNTIME)
    }

    void runtime(Map<String, String> properties) {
        addDependency(properties, SCOPE_RUNTIME)
    }

    void provided(String pattern) {
        dependencyManager.dependencies << new Dependency(new DefaultArtifact(pattern), SCOPE_PROVIDED)
    }

    void provided(Map<String, String> properties) {
        addDependency(properties, SCOPE_PROVIDED)
    }

    void optional(String pattern) {
        dependencyManager.dependencies << new Dependency(new DefaultArtifact(pattern), SCOPE_OPTIONAL)
    }

    void optional(Map<String, String> properties) {
        addDependency(properties, SCOPE_OPTIONAL)
    }

    void test(String pattern) {
        dependencyManager.dependencies << new Dependency(new DefaultArtifact(pattern), SCOPE_TEST)
    }

    void test(Map<String, String> properties) {
        addDependency(properties, SCOPE_TEST)
    }

    protected Map extractDependencyProperties(String pattern) {
        def matcher = DEPENDENCY_PATTERN.matcher(pattern)
        if (matcher.matches()) {

            def properties = [:]
            properties.artifactId = matcher.group(2)
            properties.groupId = matcher.group(1) ?: getDefaultGroup()
            properties.version = matcher.group(3)
            properties
        }
        else {
            throw new IllegalArgumentException( "Bad artifact coordinates " + pattern
                + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>" );

        }
    }

    protected String getDefaultGroup() { "" }
    protected String getDefaultExtension() { null }

    protected void addDependency(Map<String, String> properties, String scope) {
        if (!properties.group) {
            properties.group = defaultGroup
        }
        if (!properties.extension) {
            properties.extension = defaultExtension
        }
        dependencyManager.dependencies << new Dependency(new DefaultArtifact(properties.groupId, properties.artifactId, properties.classifier, properties.extension, properties.version), scope)
    }
}
@CompileStatic
class RepositoryConfiguration {
    List<RemoteRepository> repositories = []

    void inherits(boolean b) {
        // TODO
    }
    void mavenCentral() {
        if (! repositories.find{ ArtifactRepository ar -> ar.id == "mavenCentral"} )
            repositories << new RemoteRepository( "mavenCentral", "default", "http://repo1.maven.org/maven2/" );
    }

    void grailsCentral() {
        if (! repositories.find{ ArtifactRepository ar -> ar.id == "grailsCentral"} )
            repositories << new RemoteRepository( "grailsCentral", "default", "http://repo.grails.org/grails/plugins" );
    }

    void mavenRepo(String url) {
        if (! repositories.find{ ArtifactRepository ar -> ar.id == url} )
            repositories << new RemoteRepository( url, "default", url);
    }
}
