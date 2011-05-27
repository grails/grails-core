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
package org.codehaus.groovy.grails.resolve.config

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

    void inherit(boolean b) {
        dependencyManager.inheritRepositories = b
    }

    void flatDir(Map args) {
        def name = args.name?.toString()
        if (name && args.dirs) {
            def fileSystemResolver = new FileSystemResolver()
            fileSystemResolver.local = true
            fileSystemResolver.name = name

            def dirs = args.dirs instanceof Collection ? args.dirs : [args.dirs]

            dependencyManager.repositoryData << ['type':'flatDir', name:name, dirs:dirs.join(',')]
            dirs.each { dir ->
                def path = new File(dir?.toString()).absolutePath
                fileSystemResolver.addIvyPattern("${path}/[module]-[revision](-[classifier]).xml")
                fileSystemResolver.addArtifactPattern "${path}/[module]-[revision](-[classifier]).[ext]"
            }
            fileSystemResolver.settings = dependencyManager.ivySettings

            addToChainResolver(fileSystemResolver)
        }
    }

    void grailsPlugins() {
        if (isResolverNotAlreadyDefined('grailsPlugins')) {
            dependencyManager.repositoryData << [type: 'grailsPlugins', name:"grailsPlugins"]
            if (dependencyManager.buildSettings != null) {
                def pluginResolver = new GrailsPluginsDirectoryResolver(dependencyManager.buildSettings, dependencyManager.ivySettings)
                addToChainResolver(pluginResolver)
            }
        }
    }

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

        flatDir(name:"grailsHome", dirs:"${grailsHome}/src/libs")
        flatDir(name:"grailsHome", dirs:"${grailsHome}/dist")
        if (grailsHome!='.') {
            def resolver = createLocalPluginResolver("grailsHome", grailsHome)
            addToChainResolver(resolver)
        }
    }

    void mavenRepo(String url) {
        if (isResolverNotAlreadyDefined(url)) {
            dependencyManager.repositoryData << ['type':'mavenRepo', root:url, name:url, m2compatbile:true]
            def resolver = new SnapshotAwareM2Resolver(name: url, root: url, m2compatible: true, settings: dependencyManager.ivySettings, changingPattern: ".*SNAPSHOT")
            addToChainResolver(resolver)
        }
    }

    void mavenRepo(Map args) {
        if (args && args.name) {
            if (isResolverNotAlreadyDefined(args.name)) {
                dependencyManager.repositoryData << (['type':'mavenRepo'] + args)
                args.settings = dependencyManager.ivySettings
                def resolver = new SnapshotAwareM2Resolver(args)
                addToChainResolver(resolver)
            }
        }
        else {
            Message.warn("A mavenRepo specified doesn't have a name argument. Please specify one!")
        }
    }

    void resolver(DependencyResolver resolver) {
        if (resolver) {
            resolver.setSettings(dependencyManager.ivySettings)
            addToChainResolver(resolver)
        }
    }

    void ebr() {
        if (isResolverNotAlreadyDefined('ebr')) {
            dependencyManager.repositoryData << ['type':'ebr']
            IBiblioResolver ebrReleaseResolver = new SnapshotAwareM2Resolver(name:"ebrRelease",
                                                                     root:"http://repository.springsource.com/maven/bundles/release",
                                                                     m2compatible:true,
                                                                     settings:dependencyManager.ivySettings)
            addToChainResolver(ebrReleaseResolver)

            IBiblioResolver ebrExternalResolver = new SnapshotAwareM2Resolver(name:"ebrExternal",
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
    void grailsRepo(String url, String name=null) {
        if (isResolverNotAlreadyDefined(name ?: url)) {
            dependencyManager.repositoryData << ['type':'grailsRepo', url:url]
            def urlResolver = new GrailsRepoResolver(name ?: url, new URL(url))
            urlResolver.addArtifactPattern("${url}/grails-[artifact]/tags/RELEASE_*/grails-[artifact]-[revision].[ext]")
            urlResolver.addIvyPattern("${url}/grails-[artifact]/tags/RELEASE_*/[artifact]-[revision].pom")
            urlResolver.settings = dependencyManager.ivySettings
            urlResolver.latestStrategy = new LatestTimeStrategy()
            urlResolver.changingPattern = ".*SNAPSHOT"
            urlResolver.setCheckmodified(true)
            addToChainResolver(urlResolver)
        }
    }

    void grailsCentral() {
        if (isResolverNotAlreadyDefined('grailsCentral')) {
            mavenRepo("http://repo.grails.org/grails/plugins")
            grailsRepo("http://svn.codehaus.org/grails-plugins", "grailsCentral")
            grailsRepo("http://svn.codehaus.org/grails/trunk/grails-plugins", "grailsCore")
        }
    }

    void mavenCentral() {
        if (isResolverNotAlreadyDefined('mavenCentral')) {
            dependencyManager.repositoryData << ['type':'mavenCentral']
            IBiblioResolver mavenResolver = new SnapshotAwareM2Resolver(name:"mavenCentral")
            mavenResolver.m2compatible = true
            mavenResolver.settings = dependencyManager.ivySettings
            mavenResolver.changingPattern = ".*SNAPSHOT"
            addToChainResolver(mavenResolver)
        }
    }

    void mavenLocal(String repoPath) {
        if (isResolverNotAlreadyDefined('mavenLocal')) {
            dependencyManager.repositoryData << ['type':'mavenLocal']
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

            localMavenResolver.settings = dependencyManager.ivySettings
            addToChainResolver(localMavenResolver)
        }
    }

    private createLocalPluginResolver(String name, String location) {
        def pluginResolver = new FileSystemResolver(name: name)
        pluginResolver.addArtifactPattern("${location}/plugins/grails-[artifact]-[revision].[ext]")
        pluginResolver.settings = dependencyManager.ivySettings
        pluginResolver.latestStrategy = new LatestTimeStrategy()
        pluginResolver.changingPattern = ".*SNAPSHOT"
        pluginResolver.setCheckmodified(true)
        return pluginResolver
    }

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

    private boolean isResolverNotAlreadyDefined(String name) {
        def resolver
        // Fix for GRAILS-5805
        synchronized(dependencyManager.chainResolver.resolvers) {
            resolver = dependencyManager.chainResolver.resolvers.any { it.name == name }
        }
        if (resolver) {
            Message.debug("Dependency resolver $name already defined. Ignoring...")
            return false
        }
        return true
    }
}
