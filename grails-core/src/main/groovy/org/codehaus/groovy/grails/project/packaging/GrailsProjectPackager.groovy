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
package org.codehaus.groovy.grails.project.packaging

import grails.build.logging.GrailsConsole
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import grails.util.Holders
import grails.util.Metadata
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleAntBuilder
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper
import org.codehaus.groovy.grails.compiler.GrailsProjectCompiler
import org.codehaus.groovy.grails.compiler.PackagingException
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.publishing.PluginDescriptorGenerator
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.springframework.core.io.FileSystemResource
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

    protected String serverContextPath
    protected ConfigObject config
    protected AntBuilder ant
    protected File basedir
    protected resourcesDirPath
    protected boolean doCompile
    protected File webXmlFile
    protected boolean packaged
    protected boolean webXmlGenerated

    GrailsProjectPackager(GrailsProjectCompiler compiler, boolean doCompile = true) {
        this(compiler, compiler.buildSettings.configFile, doCompile)
    }

    GrailsProjectPackager(GrailsProjectCompiler compiler, File configFile, boolean doCompile = true) {
        this(compiler, null, configFile, doCompile)
    }

    GrailsProjectPackager(GrailsProjectCompiler compiler, GrailsBuildEventListener buildEventListener,boolean doCompile = true) {
        this(compiler, buildEventListener, compiler.buildSettings.configFile, doCompile)

    }
    GrailsProjectPackager(GrailsProjectCompiler compiler, GrailsBuildEventListener buildEventListener,File configFile, boolean doCompile = true) {
        super(compiler.buildSettings, buildEventListener, false)
        projectCompiler = compiler
        servletVersion = buildSettings.servletVersion
        Metadata.current[Metadata.SERVLET_VERSION] = servletVersion
        classLoader = compiler.classLoader
        pluginSettings = compiler.pluginSettings
        resourcesDirPath = buildSettings.resourcesDir.path
        basedir = buildSettings.baseDir
        webXmlFile = buildSettings.webXmlLocation
        ant = compiler.ant
        this.configFile = configFile
        this.doCompile = doCompile
        this.buildEventListener = buildEventListener
    }

    @CompileStatic
    String configureServerContextPath() {
        createConfig()
        if (serverContextPath == null) {
            // Get the application context path by looking for a property named 'app.context' in the following order of precedence:
            //    System properties
            //    application.properties
            //    config
            //    default to grailsAppName if not specified

            serverContextPath = System.getProperty("app.context")
            serverContextPath = serverContextPath ?: metadata.get('app.context')
            serverContextPath = serverContextPath ?: getServerContextPathFromConfig()
            serverContextPath = serverContextPath ?: grailsAppName

            if (!serverContextPath.startsWith('/')) {
                serverContextPath = "/${serverContextPath}"
            }
        }
        return serverContextPath
    }

    @CompileStatic
    AntBuilder getAnt() {
       if (ant == null) {
           ant = new GrailsConsoleAntBuilder()
       }
       ant
    }

    /**
     * Generates the web.xml file used by Grails to startup
     */
    void generateWebXml(GrailsPluginManager pluginManager) {
        // don't duplicate work
        if (webXmlFile.exists() && webXmlGenerated) return

        File projectWorkDir = buildSettings.projectWorkDir
        ConfigObject buildConfig = buildSettings.config
        def webXml = new FileSystemResource("$basedir/src/templates/war/web.xml")
        String tmpWebXml = "$projectWorkDir/web.xml.tmp"

        final baseWebXml = getBaseWebXml(buildConfig)
        if (baseWebXml) {
            def customWebXml = resolveResources(baseWebXml.toString())
            def customWebXmlFile = customWebXml[0].file
            if (customWebXmlFile.exists()) {
                ant.copy(file:customWebXmlFile, tofile:tmpWebXml, overwrite:true)
            }
            else {
                grailsConsole.error("Custom web.xml defined in config [${baseWebXml}] could not be found." )
                exit(1)
            }
        } else {
            if (webXml.exists()) {
                ant.copy(file:webXml.file, tofile:tmpWebXml, overwrite:true)
            }
            else {
                copyGrailsResource(tmpWebXml, grailsResource("src/war/WEB-INF/web${servletVersion}.template.xml"))
            }
        }
        webXml = new FileSystemResource(tmpWebXml)
        ant.replace(file:tmpWebXml, token:"@grails.project.key@",
                    value:"${grailsAppName}-${buildSettings.grailsEnv}-${grailsAppVersion}")

        def sw = new StringWriter()

        try {
            profile("generating web.xml from $webXml") {
                buildEventListener.triggerEvent("WebXmlStart", webXml.filename)
                pluginManager.doWebDescriptor(webXml, sw)
                webXmlFile.withWriter { it << sw.toString() }
                webXmlGenerated = true
                buildEventListener.triggerEvent("WebXmlEnd", webXml.filename)
            }
        }
        catch (Exception e) {
            grailsConsole.error("Error generating web.xml file", e)
            exit(1)
        }
    }

    private getBaseWebXml(ConfigObject buildConfig) {
        buildConfig.grails.config.base.webXml
    }

    /**
     * Generates a plugin.xml file for the given plugin descriptor
     *
     * @param descriptor The descriptor
     * @param compilePlugin Whether the compile the plugin
     * @return The plugin properties
     */
//    @CompileStatic
    def generatePluginXml(File descriptor, boolean compilePlugin = true ) {
        def pluginBaseDir = descriptor.parentFile
        def pluginProps = pluginSettings.getPluginInfo(pluginBaseDir.absolutePath)
        def plugin
        def pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"

        if (compilePlugin) {
            try {
                // Rather than compiling the descriptor via Ant, we just load
                // the Groovy file into a GroovyClassLoader. We add the classes
                // directory to the class loader in case it didn't exist before
                // the associated plugin's sources were compiled.
                def gcl = new GroovyClassLoader(classLoader)
                gcl.addURL(buildSettings.classesDir.toURI().toURL())

                def pluginClass = gcl.parseClass(descriptor)
                plugin = pluginClass.newInstance()
                pluginProps = DefaultGroovyMethods.getProperties(plugin)
            }
            catch (Throwable t) {
                grailsConsole.error("Failed to compile plugin: ${t.message}", t)
                exit(1)
            }
        }

        if (pluginProps != null && pluginProps["grailsVersion"]) {
            pluginGrailsVersion = pluginProps["grailsVersion"]
        }

        def resourceList = pluginSettings.getArtefactResourcesForOne(descriptor.parentFile.absolutePath)
        // Work out what the name of the plugin is from the name of the descriptor file.
        String pluginName = GrailsNameUtils.getPluginName(descriptor.name)

        // Remove the existing 'plugin.xml' if there is one.
        def pluginXml = new File(pluginBaseDir, "plugin.xml")
        pluginXml.delete()

        // Use MarkupBuilder with indenting to generate the file.
        pluginXml.withWriter { Writer writer ->
            def generator = new PluginDescriptorGenerator(buildSettings, pluginName, resourceList.toList())

            pluginProps["type"] = descriptor.name - '.groovy'
            generator.generatePluginXml(pluginProps, writer)

            return compilePlugin ? plugin : pluginProps
        }
    }

    /**
     * Packages an application.
     *
     * @return true if the packaging was successful, false otherwise
     */
    @CompileStatic
    ConfigObject packageApplication() {
        // don't duplicate work
        if (config != null && packaged == true) {
            return config
        }

        try {
            packageConfigFiles(basedir)
        } catch (Throwable e) {
            throw new PackagingException("Error occurred packaging configuration files: ${e.message}", e)
        }

        if (doCompile) {
            projectCompiler.compilePlugins()
            projectCompiler.compile()
        }

        try {
            packageTlds()
        } catch (Throwable e) {
            throw new PackagingException("Error occurred packaging TLD files: ${e.message}", e)
        }

        def config = createConfig()

        try {
            packagePlugins()
        } catch (Throwable e) {
            throw new PackagingException("Error occurred packaging plugin resources: ${e.message}", e)
        }

        try {
            packageJspFiles()
        } catch (Throwable e) {
            throw new PackagingException("Error occurred packaging JSP files: ${e.message}", e)
        }
        try {
            processMessageBundles()
        } catch (Throwable e) {
            throw new PackagingException("Error occurred processing message bundles: ${e.message}", e)
        }



        packageMetadataFile()

        startLogging(config)
        packaged = true
        return config
    }

    private void packageMetadataFile() {
        ant.copy(todir: buildSettings.getClassesDir(), failonerror: false) {
            fileset(dir: basedir, includes: metadataFile.name)
        }
    }

    /**
     * Starts the logging infrastructure
     *
     * @param config The config object
     */
    void startLogging(ConfigObject config) {
        if (!ClassUtils.isPresent(LOGGING_INITIALIZER_CLASS, classLoader)) {
            return
        }

        try {
            classLoader.loadClass(LOGGING_INITIALIZER_CLASS).newInstance().initialize(config)
        } catch (e) {
            throw new PackagingException("Error initializing logging: $e.message",e)
        }
    }

    /**
     * Creates and loads the application Config
     *
     * @return The application config
     */
    @CompileStatic
    ConfigObject createConfig() {
        if (config == null) {
            config = new ConfigObject()
            if (configFile.exists()) {
                Class configClass = null
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

            if (new File(basedir, "grails-app/conf/DataSource.groovy").exists()) {
                try {
                    config.merge configSlurper.parse(classLoader.loadClass("DataSource"))
                }
                catch(ClassNotFoundException e) {
                    grailsConsole.error "Warning", "DataSource.groovy not found, assuming dataSource bean is configured by Spring"
                }
                catch(Exception e) {
                    throw new PackagingException("Error loading DataSource.groovy: $e.message",e)
                }
            }
            ConfigurationHelper.initConfig(config, null, classLoader)
        }
        Holders.config = config
        return config
    }

    /**
     * Processes application message bundles converting them from native to ascii if required
     */
    void processMessageBundles() {
        String i18nDir = "${resourcesDirPath}/grails-app/i18n"

        if (!native2ascii) {
            ant.copy(todir:i18nDir) {
                fileset(dir:"$basedir/grails-app/i18n", includes:"**/*.properties")
            }
            return
        }

        def ant = new GrailsConsoleAntBuilder(ant.project)
        File grailsAppI18n = new File(basedir, 'grails-app/i18n')
        if (grailsAppI18n.exists()) {
            ant.native2ascii(src: grailsAppI18n.path, dest: i18nDir,
                             includes: "**/*.properties", encoding: "UTF-8")
        }

        PluginBuildSettings settings = pluginSettings
        def i18nPluginDirs = settings.pluginI18nDirectories
        if (!i18nPluginDirs) {
            return
        }

        ExecutorService pool = Executors.newFixedThreadPool(5)
        try {
            for (Resource r in i18nPluginDirs) {
                pool.execute({ Resource srcDir ->
                    if (!srcDir.exists()) {
                        return
                    }

                    def file = srcDir.file
                    def pluginDir = file.parentFile.parentFile
                    def info = settings.getPluginInfo(pluginDir.absolutePath)
                    if (!info) {
                        return
                    }

                    def destDir = "$resourcesDirPath/plugins/${info.name}-${info.version}/grails-app/i18n"
                    try {
                        def localAnt = new GrailsConsoleAntBuilder(ant.project)
                        localAnt.project.defaultInputStream = System.in
                        localAnt.mkdir(dir: destDir)
                        localAnt.native2ascii(src: file, dest: destDir,
                                              includes: "**/*.properties", encoding: "UTF-8")
                    }
                    catch (e) {
                        grailsConsole.error "native2ascii error converting i18n bundles for plugin [$pluginDir.name] $e.message"
                    }
                }.curry(r))
            }
        } finally {
            pool.shutdown()
        }
    }

    protected void packageJspFiles() {
        def logic = {
            def ant = new GrailsConsoleAntBuilder(ant.project)
            File viewsDir = new File(basedir, 'grails-app/views')
            if (!viewsDir.exists()) {
                return
            }

            def files = ant.fileScanner {
                fileset(dir: viewsDir, includes:"**/*.jsp")
            }

            if (files.iterator().hasNext()) {
                ant.mkdir(dir:"$basedir/web-app/WEB-INF/grails-app/views")
                ant.copy(todir:"$basedir/web-app/WEB-INF/grails-app/views") {
                    fileset(dir: viewsDir, includes:"**/*.jsp")
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
        ExecutorService pool = Executors.newFixedThreadPool(5)
        try {
            def pluginInfos = pluginSettings.getSupportedPluginInfos()
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

            futures.each {  Future it -> it.get() }
        } finally {
            pool.shutdown()
        }
    }

    @CompileStatic
    void packageTlds() {
        // We don't know until runtime what servlet version to use, so install the relevant TLDs now
        copyGrailsResources("$basedir/web-app/WEB-INF/tld", "src/war/WEB-INF/tld/${servletVersion}/*", false)
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

    private String getServerContextPathFromConfig() {
        final v = config.grails.app.context
        if (v instanceof CharSequence) return v.toString()
    }
}
