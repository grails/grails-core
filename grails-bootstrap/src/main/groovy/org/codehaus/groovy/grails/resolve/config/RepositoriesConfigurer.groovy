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
package org.codehaus.groovy.grails.resolve.config

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import org.apache.ivy.plugins.latest.LatestTimeStrategy
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.RepositoryResolver
import org.apache.ivy.util.Message
import org.codehaus.groovy.grails.resolve.GrailsPluginsDirectoryResolver
import org.codehaus.groovy.grails.resolve.GrailsRepoResolver
import org.codehaus.groovy.grails.resolve.SnapshotAwareM2Resolver

class RepositoriesConfigurer extends AbstractDependencyManagementConfigurer {

    RepositoriesConfigurer(DependencyConfigurationContext context) {
        super(context)
    }

    @CompileStatic
    void inherit(boolean b) {
        dependencyManager.inheritRepositories = b
    }

    @CompileStatic
    void inherits(boolean b) {
        dependencyManager.inheritRepositories = b
    }

    @CompileStatic
    void flatDir(Map args) {
        def name = args.get('name')?.toString()
        def dirsO = args.get('dirs')
        if (!name || !dirsO) {
            return
        }

        def fileSystemResolver = new FileSystemResolver()
        fileSystemResolver.local = true
        fileSystemResolver.name = name

        Collection dirs = (Collection)(args.get('dirs') instanceof Collection ? args.get('dirs'): [args.get('dirs')])

        dependencyManager.repositoryData << [type: 'flatDir', name:name, dirs:dirs.join(',')]
        dirs.each { dir ->
            def path = new File(dir?.toString()).absolutePath
            fileSystemResolver.addIvyPattern("${path}/[module]-[revision](-[classifier]).xml")
            fileSystemResolver.addArtifactPattern "${path}/[module]-[revision](-[classifier]).[ext]"
        }
        fileSystemResolver.settings = dependencyManager.ivySettings

        addToChainResolver(fileSystemResolver)
    }

    @CompileStatic
    void grailsPlugins() {
        if (context.offline || !isResolverNotAlreadyDefined('grailsPlugins')) {
            return
        }

        dependencyManager.repositoryData << [type: 'grailsPlugins', name:"grailsPlugins"]
        if (dependencyManager.buildSettings != null) {
            addToChainResolver new GrailsPluginsDirectoryResolver(dependencyManager.buildSettings, dependencyManager.ivySettings)
        }
    }

    @CompileStatic
    void grailsHome() {
        if (!isResolverNotAlreadyDefined('grailsHome')) {
            return
        }

        def grailsHome = dependencyManager.buildSettings?.grailsHome?.absolutePath ?: System.getenv("GRAILS_HOME")
        if (!grailsHome) {
            return
        }

        grailsHome = new File(grailsHome).absolutePath
        def fileSystemResolver = new FileSystemResolver()
        fileSystemResolver.local = true
        fileSystemResolver.name = "grailsHome"
        fileSystemResolver.addIvyPattern("${grailsHome}/lib/[organisation]/[module]/ivy-[revision](-[classifier]).xml")
        fileSystemResolver.addArtifactPattern "${grailsHome}/lib/[organisation]/[module]/jars/[module]-[revision](-[classifier]).[ext]"
        fileSystemResolver.addArtifactPattern "${grailsHome}/lib/[organisation]/[module]/bundles/[module]-[revision](-[classifier]).[ext]"
        fileSystemResolver.settings = dependencyManager.ivySettings

        addToChainResolver(fileSystemResolver)

        def grailsHomeDistResolver = new FileSystemResolver()
        grailsHomeDistResolver.local = true
        grailsHomeDistResolver.name = "grailsHome"
        def grailsHomeDistPattern = "${grailsHome}/dist/[module]-[revision](-[classifier])"
        grailsHomeDistResolver.addIvyPattern("${grailsHomeDistPattern}.pom")
        grailsHomeDistResolver.addArtifactPattern "${grailsHomeDistPattern}.[ext]"
        grailsHomeDistResolver.settings = dependencyManager.ivySettings

        addToChainResolver(grailsHomeDistResolver)

        final workDir = dependencyManager.buildSettings?.grailsWorkDir
        if (workDir) {
            flatDir(name:"grailsHome", dirs:"${workDir}/cached-installed-plugins")
        }
        if (grailsHome != '.') {
            def resolver = createLocalPluginResolver("grailsHome", grailsHome)
            addToChainResolver(resolver)
        }
    }

    @CompileStatic
    void mavenRepo(String url) {
        if (context.offline || !isResolverNotAlreadyDefined(url)) {
            return
        }

        dependencyManager.repositoryData << [type: 'mavenRepo', root: url, name: url, m2compatbile: true]
        def resolver = new SnapshotAwareM2Resolver()
        resolver.name = url
        resolver.root = url
        resolver.m2compatible = true
        resolver.settings = dependencyManager.ivySettings
        resolver.changingPattern = ".*SNAPSHOT"
        addToChainResolver(resolver)
    }

    @CompileStatic
    void mavenRepo(Map args) {
        def name = args?.get('name')
        if (args && name) {
            if (!context.offline && isResolverNotAlreadyDefined(name?.toString())) {
                dependencyManager.repositoryData << ([type: 'mavenRepo'] + args)
                args.settings = dependencyManager.ivySettings
                def resolver = createSnapshotResolver(args)

                addToChainResolver(resolver)
            }
        }
        else {
            Message.warn("A mavenRepo specified doesn't have a name argument. Please specify one!")
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private SnapshotAwareM2Resolver createSnapshotResolver(Map args) {
        new SnapshotAwareM2Resolver(args)
    }

    @CompileStatic
    void resolver(DependencyResolver resolver) {
        if (resolver) {
            resolver.setSettings(dependencyManager.ivySettings)
            addToChainResolver(resolver)
        }
    }

    @CompileStatic
    void ebr() {
        if (!context.offline && isResolverNotAlreadyDefined('ebr')) {
            dependencyManager.repositoryData << [type: 'ebr']
            IBiblioResolver ebrReleaseResolver = createSnapshotResolver(
                name:"ebrRelease",
                root:"http://repository.springsource.com/maven/bundles/release",
                m2compatible:true,
                settings:dependencyManager.ivySettings)
            addToChainResolver(ebrReleaseResolver)

            IBiblioResolver ebrExternalResolver = createSnapshotResolver(
                name:"ebrExternal",
                root:"http://repository.springsource.com/maven/bundles/external",
                m2compatible:true,
                settings:dependencyManager.ivySettings)

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
    @CompileStatic
    void grailsRepo(String url, String name=null) {
        if (!context.offline && isResolverNotAlreadyDefined(name ?: url)) {
            dependencyManager.repositoryData << [type: 'grailsRepo', url:url]
            def urlResolver = new GrailsRepoResolver(name ?: url, new URL(url))
            urlResolver.addArtifactPattern("${url}/grails-[module]/tags/RELEASE_*/grails-[module]-[revision].[ext]")
            urlResolver.addIvyPattern("${url}/grails-[module]/tags/RELEASE_*/[module]-[revision].pom")
            urlResolver.settings = dependencyManager.ivySettings
            urlResolver.latestStrategy = new LatestTimeStrategy()
            urlResolver.changingPattern = ".*SNAPSHOT"
            urlResolver.setCheckmodified(true)
            addToChainResolver(urlResolver)
        }
    }

    @CompileStatic
    void grailsCentral() {
        if (!context.offline && isResolverNotAlreadyDefined('grailsCentral')) {
            grailsRepo("http://grails.org/plugins", "grailsCentral")
        }
    }

    @CompileStatic
    void mavenCentral() {
        if (!context.offline && isResolverNotAlreadyDefined('mavenCentral')) {
            dependencyManager.repositoryData << [type: 'mavenCentral']
            IBiblioResolver mavenResolver = createSnapshotResolver(name:"mavenCentral")
            mavenResolver.m2compatible = true
            mavenResolver.settings = dependencyManager.ivySettings
            mavenResolver.changingPattern = ".*SNAPSHOT"
            addToChainResolver(mavenResolver)
        }
    }

    @CompileStatic
    void mavenLocal(String repoPath = null) {
        if (isResolverNotAlreadyDefined('mavenLocal')) {
            dependencyManager.repositoryData << [type: 'mavenLocal']
            FileSystemResolver localMavenResolver = new FileSystemResolver()
            localMavenResolver.name = 'localMavenResolver'
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
                    String localRepository = getLocalRespository(settingsXml)

                    if (localRepository.trim()) {
                        repositoryPath = localRepository
                    }
                }
            }

            localMavenResolver.addIvyPattern(
                "${repositoryPath}/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).pom")

            localMavenResolver.addArtifactPattern(
                "${repositoryPath}/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]")

            localMavenResolver.settings = dependencyManager.ivySettings
            addToChainResolver(localMavenResolver)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private String getLocalRespository(GPathResult settingsXml) {
        return settingsXml.localRepository.text()
    }

    @CompileStatic
    private DependencyResolver createLocalPluginResolver(String name, String location) {
        def pluginResolver = new FileSystemResolver()
        pluginResolver.name = name
        pluginResolver.addArtifactPattern("${location}/plugins/[artifact]-[revision].[ext]")
        pluginResolver.addIvyPattern("${location}/plugins/[module]-[revision].pom")
        pluginResolver.settings = dependencyManager.ivySettings
        pluginResolver.latestStrategy = new LatestTimeStrategy()
        pluginResolver.changingPattern = ".*SNAPSHOT"
        pluginResolver.setCheckmodified(true)
        return pluginResolver
    }

    @CompileStatic
    private addToChainResolver(DependencyResolver resolver) {
        if (context.pluginName && !dependencyManager.inheritRepositories) return

        if (dependencyManager.transferListener !=null && (resolver instanceof RepositoryResolver)) {
            ((RepositoryResolver)resolver).repository.addTransferListener dependencyManager.transferListener
        }

        // Fix for GRAILS-5805
        synchronized(dependencyManager.chainResolver.resolvers) {
            dependencyManager.chainResolver.add resolver
        }
    }

    @CompileStatic
    private boolean isResolverNotAlreadyDefined(String name) {
        def resolver
        // Fix for GRAILS-5805
        synchronized(dependencyManager.chainResolver.resolvers) {
            resolver = dependencyManager.chainResolver.resolvers.any { DependencyResolver it -> it.name == name }
        }
        if (resolver) {
            Message.debug("Dependency resolver $name already defined. Ignoring...")
            return false
        }
        return true
    }
}
