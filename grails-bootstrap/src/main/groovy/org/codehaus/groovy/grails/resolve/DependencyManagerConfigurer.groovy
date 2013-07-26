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
package org.codehaus.groovy.grails.resolve

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.apache.ivy.plugins.repository.TransferEvent
import org.apache.ivy.plugins.repository.TransferListener
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.codehaus.groovy.tools.LoaderConfiguration

/**
 * Configures the Ivy dependency manager for usage within Grails
 *
 * @author Graeme Rocher
 * @since 2.3
 */
// @CompileStatic TODO: Report Groovy issue, uncommenting causes VerifierError
class DependencyManagerConfigurer {

    @CompileStatic
    DependencyManager configureAether(BuildSettings buildSettings) {
        final grailsHome = buildSettings.grailsHome
        final grailsVersion = buildSettings.grailsVersion
        GroovyClassLoader classLoader = configureAetherClassLoader(grailsHome)
        if (Environment.isFork()) {
            BuildSettings.initialiseDefaultLog4j(classLoader)
        }
        DependencyManager aetherDependencyManager = loadAetherDependencyManager(classLoader)

        final coreDeps = classLoader.loadClass("org.codehaus.groovy.grails.resolve.maven.aether.config.GrailsAetherCoreDependencies")
            .newInstance(grailsVersion, buildSettings.servletVersion, !org.codehaus.groovy.grails.plugins.GrailsVersionUtils.isVersionGreaterThan("1.5", buildSettings.compilerTargetLevel), buildSettings.isGrailsProject())
        prepareAetherDependencies(aetherDependencyManager, buildSettings, coreDeps)

        if (buildSettings.proxySettings) {
            setProxy(aetherDependencyManager, buildSettings.proxySettings)
        }
        return aetherDependencyManager
    }


//    @CompileStatic
    static DependencyManager createAetherDependencyManager(BuildSettings buildSettings) {
        loadAetherDependencyManager( configureAetherClassLoader(buildSettings.grailsHome) )
    }

    @CompileStatic
    private static DependencyManager loadAetherDependencyManager(GroovyClassLoader classLoader) {
        (DependencyManager) classLoader.loadClass("org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager").newInstance()
    }

    @CompileStatic
    private static GroovyClassLoader configureAetherClassLoader(File grailsHome) {
        def lc = new LoaderConfiguration()
        lc.setRequireMain(false)
        System.setProperty("grails.home", grailsHome.canonicalPath)
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
        new File(grailsHome, "conf/aether-starter.conf").withInputStream { InputStream it ->
            lc.configure(it)
        }

        GroovyClassLoader classLoader = new GroovyClassLoader()
        final jarFiles = lc.getClassPathUrls()
        for (jar in jarFiles) {
            classLoader.addURL(jar)
        }
        classLoader
    }

    void setProxy(DependencyManager dependencyManager, ConfigObject config) {
        final host = config.proxyHost ?: null
        final port = config.proxyPort ?: null
        if (host && port) {
            dependencyManager.addProxy(host, port, config.proxyUser ?: null, config.proxyPassword ?: null, config.nonProxyHosts ?: null)
        }
    }

    private static void prepareAetherDependencies(aetherDependencyManager, BuildSettings buildSettings, coreDeps) {
        aetherDependencyManager.includeJavadoc = buildSettings.includeJavadoc
        aetherDependencyManager.includeSource = buildSettings.includeSource

        aetherDependencyManager.inheritedDependencies.global = coreDeps.createDeclaration()

        def dependencyConfig = buildSettings.config.grails.project.dependency.resolution

        if (dependencyConfig instanceof Closure) {
            aetherDependencyManager.parseDependencies(dependencyConfig)
        }
    }

    DependencyManager configureIvy(BuildSettings buildSettings) {
        ConfigObject config = buildSettings.config
        final grailsVersion = buildSettings.grailsVersion
        IvyDependencyManager dependencyManager = createIvyDependencyManager( buildSettings)

        dependencyManager.offline = buildSettings.isOffline()
        dependencyManager.includeJavadoc = buildSettings.isIncludeJavadoc()
        dependencyManager.includeSource = buildSettings.isIncludeSource()

        def console = GrailsConsole.instance
        dependencyManager.transferListener = { TransferEvent e ->
            switch (e.eventType) {
                case TransferEvent.TRANSFER_STARTED:
                    def resourceName = e.resource.name
                    if (!resourceName?.endsWith('plugins-list.xml')) {
                        resourceName = resourceName[resourceName.lastIndexOf('/') + 1..-1]
                        console.updateStatus "Downloading: ${resourceName}"
                    }
                    break
            }
        } as TransferListener

        def grailsConfig = config.grails

        setCacheDir(grailsConfig, dependencyManager)

        if (!buildSettings.dependenciesExternallyConfigured) {
            def coreDependencies = new GrailsIvyDependencies(grailsVersion, buildSettings.servletVersion, !org.codehaus.groovy.grails.plugins.GrailsVersionUtils.isVersionGreaterThan("1.5", buildSettings.compilerTargetLevel), buildSettings.isGrailsProject())
            buildSettings.coreDependencies = coreDependencies
            configureGlobalFrameworkDependencies(coreDependencies, grailsConfig)
            configureIvyAuthentication(grailsConfig, dependencyManager)
        }
        else {

            configureDefaultPluginResolver(grailsConfig)
        }

        Closure dependencyConfig = getDependencyConfig(grailsConfig, dependencyManager)
        if (dependencyConfig instanceof Closure) {
            dependencyManager.parseDependencies dependencyConfig
        }

        return dependencyManager
    }

//    @CompileStatic
    static IvyDependencyManager createIvyDependencyManager(BuildSettings buildSettings) {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_WARN)

        Metadata metadata = Metadata.getCurrent()
        def appName = metadata.getApplicationName() ?: "grails"
        def appVersion = metadata.getApplicationVersion() ?: buildSettings.grailsVersion

        def dependencyManager = new IvyDependencyManager(appName,
            appVersion, buildSettings, metadata)
        dependencyManager
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Closure getDependencyConfig(grailsConfig, IvyDependencyManager dependencyManager) {
        def dependencyConfig = grailsConfig.project.dependency.resolution
        if (!dependencyConfig) {
            dependencyConfig = grailsConfig.global.dependency.resolution
            dependencyManager.inheritsAll = true
        }
        if (dependencyConfig instanceof Closure) {
            return dependencyConfig
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void configureGlobalFrameworkDependencies(GrailsIvyDependencies coreDependencies, grailsConfig) {
        grailsConfig.global.dependency.resolution = coreDependencies.createDeclaration()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void configureIvyAuthentication(grailsConfig, IvyDependencyManager dependencyManager) {
        def credentials = grailsConfig.project.ivy.authentication
        if (credentials instanceof Closure) {
            dependencyManager.parseDependencies credentials
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void configureDefaultPluginResolver(grailsConfig) {
        // Even if the dependencies are handled externally, we still
        // to handle plugin dependencies.
        grailsConfig.global.dependency.resolution = {
            repositories {
                grailsPlugins()
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void setCacheDir(grailsConfig, IvyDependencyManager dependencyManager) {
        // If grails.dependency.cache.dir is set, use it for Ivy.
        if (grailsConfig.dependency.cache.dir) {
            dependencyManager.ivySettings.defaultCache = grailsConfig.dependency.cache.dir as File
        }
    }
}
