/*
 * Copyright 2011 SpringSource
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

package org.codehaus.groovy.grails.compiler

import grails.build.logging.GrailsConsole
import grails.util.Metadata
import grails.util.PluginBuildSettings
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleAntBuilder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.springframework.core.io.Resource
import org.springframework.util.ClassUtils

/**
 * Encapsulates the logic to package a project ready for execution.
 *
 * @since 2.0
 * @author Graeme Rocher
 */
class GrailsProjectPackager extends BaseSettingsApi {

    public static final String LOGGING_INITIALIZER_CLASS = "org.codehaus.groovy.grails.plugins.logging.LoggingInitializer"
    GrailsProjectCompiler projectCompiler
    GrailsConsole grailsConsole = GrailsConsole.getInstance()
    boolean warMode = false
    boolean async = true
    boolean native2ascii = true
    File configFile
    String servletVersion = "2.5"
    ClassLoader classLoader

    private AntBuilder ant
    private basedir
    private resourcesDirPath
    private boolean doCompile

    GrailsProjectPackager(GrailsProjectCompiler compiler, File configFile, boolean doCompile = true) {
        super(compiler.buildSettings, false)
        projectCompiler = compiler
        servletVersion = buildSettings.servletVersion
        Metadata.current[Metadata.SERVLET_VERSION] = servletVersion
        classLoader = compiler.classLoader
        pluginSettings = compiler.pluginSettings
        resourcesDirPath = buildSettings.resourcesDir.path
        basedir = buildSettings.baseDir
        ant = compiler.ant
        this.configFile = configFile
        this.doCompile = doCompile
    }

    AntBuilder getAnt() {
       if (this.ant == null) {
           this.ant = new GrailsConsoleAntBuilder()
       }
       return ant
    }

    /**
     * Packages an application
     *
     * @return True if the packaging was successful, false otherwise
     */
    ConfigObject packageApplication() {

        if (doCompile) {
            projectCompiler.compilePlugins()
            projectCompiler.compile()
        }

        try {
            packageTlds()
        } catch (e) {
            throw new PackagingException("Error occurred packaging TLD files: ${e.message}", e)
        }

        def config = createConfig()

        try {
            packagePlugins()
        } catch (e) {
            throw new PackagingException("Error occurred packaging plugin resources: ${e.message}", e)
        }

        try {
            packageJspFiles()
        } catch (e) {
            throw new PackagingException("Error occurred packaging JSP files: ${e.message}", e)
        }
        try {
            processMessageBundles()
        } catch (e) {
            throw new PackagingException("Error occurred processing message bundles: ${e.message}", e)
        }

        try {
            packageConfigFiles(basedir)
        } catch (e) {
            throw new PackagingException("Error occurred packaging configuration files: ${e.message}", e)
        }

        ant.copy(todir:buildSettings.getClassesDir(), failonerror:false) {
            fileset(dir: basedir, includes:metadataFile.name)
        }

        startLogging(config)
        return config
    }

    /**
     * Starts the logging infrastructure
     *
     * @param config The config object
     */
    void startLogging(ConfigObject config) {
        if (ClassUtils.isPresent(LOGGING_INITIALIZER_CLASS, classLoader)) {
            try {
                classLoader
                    .loadClass(LOGGING_INITIALIZER_CLASS)
                    .newInstance()
                    .initialize(config)
            } catch (e) {
                throw new PackagingException("Error initializing logging: $e.message",e)
            }
        }
    }

    /**
     * Creates and loads the application Config
     *
     * @return The application config
     */
    ConfigObject createConfig() {
        def config = new ConfigObject()
        if (configFile.exists()) {
            def configClass
            try {
                configClass = classLoader.loadClass("Config")
            }
            catch (ClassNotFoundException cnfe) {
                grailsConsole.error "Warning", "No config found for the application."
            }
            if (configClass) {
                try {
                    config = configSlurper.parse(configClass)
                    config.setConfigFile(configFile.toURI().toURL())
                }
                catch (Exception e) {
                    throw new PackagingException("Error loading Config.groovy: $e.message", e)
                }
            }
        }

        def dataSourceFile = new File(basedir, "grails-app/conf/DataSource.groovy")
        if (dataSourceFile.exists()) {
            try {
                def dataSourceConfig = configSlurper.parse(classLoader.loadClass("DataSource"))
                config.merge(dataSourceConfig)
            }
            catch(ClassNotFoundException e) {
                grailsConsole.error "Warning", "DataSource.groovy not found, assuming dataSource bean is configured by Spring"
            }
            catch(Exception e) {
                throw new PackagingException("Error loading DataSource.groovy: $e.message",e)
            }
        }
        ConfigurationHelper.initConfig(config, null, classLoader)
        ConfigurationHolder.config = config
        return config
    }

    /**
     * Processes application message bundles converting them from native to ascii if required
     */
    void processMessageBundles() {
        String i18nDir = "${resourcesDirPath}/grails-app/i18n"
        if (native2ascii) {

            def ant = new GrailsConsoleAntBuilder(ant.project)
            ant.native2ascii(src: "${basedir}/grails-app/i18n",
                    dest: i18nDir,
                    includes: "**/*.properties",
                    encoding: "UTF-8")

            PluginBuildSettings settings = pluginSettings
            def i18nPluginDirs = settings.pluginI18nDirectories
            if (i18nPluginDirs) {
                ExecutorService pool = Executors.newFixedThreadPool(5)
                for (Resource r in i18nPluginDirs) {
                    pool.execute({ Resource srcDir ->
                        if (srcDir.exists()) {
                            def file = srcDir.file
                            def pluginDir = file.parentFile.parentFile
                            def info = settings.getPluginInfo(pluginDir.absolutePath)

                            if (info) {
                                def pluginDirName = pluginDir.name
                                def destDir = "$resourcesDirPath/plugins/${info.name}-${info.version}/grails-app/i18n"
                                try {
                                    def localAnt = new GrailsConsoleAntBuilder(ant.project)
                                    localAnt.project.defaultInputStream = System.in
                                    localAnt.mkdir(dir: destDir)
                                    localAnt.native2ascii(src: file,
                                            dest: destDir,
                                            includes: "**/*.properties",
                                            encoding: "UTF-8")
                                }
                                catch (e) {
                                    grailsConsole.error "native2ascii error converting i18n bundles for plugin [${pluginDirName}] ${e.message}"
                                }
                            }
                        }
                    }.curry(r))
                }
            }
        }
        else {
            ant.copy(todir:i18nDir) {
                fileset(dir:"${basedir}/grails-app/i18n", includes:"**/*.properties")
            }
        }
    }

    protected void packageJspFiles() {
        def logic = {
            def ant = new GrailsConsoleAntBuilder(ant.project)
            def files = ant.fileScanner {
                fileset(dir:"${basedir}/grails-app/views", includes:"**/*.jsp")
            }

            if (files.iterator().hasNext()) {
                ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/views")
                ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/views") {
                    fileset(dir:"${basedir}/grails-app/views", includes:"**/*.jsp")
                }
            }
        }

        if (async) {
            Thread.start logic
        }
        else {
            logic.call()
        }
    }
    /**
     * Packages application plugins to the target directory in WAR mode
     *
     * @param targetDir The target dir
     */
    void packagePluginsForWar(targetDir) {
        def pluginInfos = pluginSettings.getSupportedPluginInfos()
        for (GrailsPluginInfo info in pluginInfos) {
            try {
                def pluginBase = info.pluginDir.file
                def pluginPath = pluginBase.absolutePath
                def pluginName = "${info.name}-${info.version}"

                packageConfigFiles(pluginBase.path)
                if (new File(pluginPath, "web-app").exists()) {
                    ant.mkdir(dir:"${targetDir}/plugins/${pluginName}")
                    ant.copy(todir: "${targetDir}/plugins/${pluginName}") {
                        fileset(dir: "${pluginBase}/web-app", includes: "**",
                                excludes: "**/WEB-INF/**, **/META-INF/**")
                    }
                }
            }
            catch (Exception e) {
                throw new PackagingException("Error packaging plugin [${info.name}] : ${e.message}", e)
            }
        }
    }

    /**
     * Packages plugins for development mode
     */
    void packagePlugins() {
        def pluginInfos = pluginSettings.getSupportedPluginInfos()
        ExecutorService pool = Executors.newFixedThreadPool(5)
        def futures = []
        for (GrailsPluginInfo gpi in pluginInfos) {
            futures << pool.submit({ GrailsPluginInfo info ->
                try {
                    def pluginDir = info.pluginDir
                    if (pluginDir) {
                        def pluginBase = pluginDir.file
                        packageConfigFiles(pluginBase.path)
                    }
                }
                catch (Exception e) {
                    grailsConsole.error "Error packaging plugin [${info.name}] : ${e.message}"
                }
            }.curry(gpi) as Runnable)
        }

        futures.each { it.get() }
    }

    void packageTlds() {
        // We don't know until runtime what servlet version to use, so
        // install the relevant TLDs now.
        copyGrailsResources("${basedir}/web-app/WEB-INF/tld", "src/war/WEB-INF/tld/${servletVersion}/*", false)
    }

    void packageTemplates(scaffoldDir) {
        ant.mkdir(dir:scaffoldDir)
        if (new File(basedir, "src/templates/scaffolding").exists()) {
            ant.copy(todir:scaffoldDir, overwrite:true) {
                fileset(dir:"${basedir}/src/templates/scaffolding", includes:"**")
            }
        }
        else {
            copyGrailsResources(scaffoldDir, "src/grails/templates/scaffolding/*")
        }
    }

    /**
     * Packages any config files such as Hibernate config, XML files etc.
     * to the projects resources directory
     *
     * @param from Where to package from
     */
    void packageConfigFiles(from) {
        def ant = new GrailsConsoleAntBuilder()
        def targetPath = buildSettings.resourcesDir.path
        def dir = new File(from , "grails-app/conf")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path) {
                    exclude(name:"**/*.groovy")
                    exclude(name:"**/log4j*")
                    exclude(name:"hibernate/**/*")
                    exclude(name:"spring/**/*")
                }
            }
        }

        dir = new File(dir, "hibernate")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path, includes:"**/*")
            }
        }

        dir = new File(from, "src/groovy")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path) {
                    exclude(name:"**/*.groovy")
                    exclude(name:"**/*.java")
                }
            }
        }

        dir = new File(from, "src/java")
        if (dir.exists()) {
            ant.copy(todir:targetPath, failonerror:false) {
                fileset(dir:dir.path) {
                    exclude(name:"**/*.java")
                }
            }
        }
    }
}
