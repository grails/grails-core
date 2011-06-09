/*
 * Copyright 2008 the original author or authors.
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
package grails.util

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import org.apache.ivy.plugins.repository.TransferEvent
import org.apache.ivy.plugins.repository.TransferListener
import org.apache.ivy.util.ChecksumHelper
import org.apache.ivy.util.DefaultMessageLogger
import org.apache.ivy.util.Message
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.codehaus.groovy.grails.resolve.GrailsCoreDependencies
import org.codehaus.groovy.runtime.StackTraceUtils
import grails.build.logging.GrailsConsole

/**
 * <p>Represents the project paths and other build settings
 * that the user can change when running the Grails commands. Defaults
 * are provided for all settings, but the user can override those by
 * setting the appropriate system property or specifying a value for
 * it in the BuildConfig.groovy file.</p>
 * <p><b>Warning</b> The behaviour is poorly defined if you explicitly
 * set some of the project paths (such as {@link BuildSettings#projectWorkDir }),
 * but not others. If you set one of them explicitly, set all of them
 * to ensure consistent behaviour.</p>
 */
class BuildSettings extends AbstractBuildSettings {
    static final Pattern JAR_PATTERN = ~/^\S+\.jar$/
    /**
     * The base directory of the application
     */
    public static final String APP_BASE_DIR = "base.dir"
    /**
     * The name of the system property for {@link #grailsWorkDir}.
     */
    public static final String WORK_DIR = "grails.work.dir"

    /**
     * The name of the system property for {@link #projectWorkDir}.
     */
    public static final String PROJECT_WORK_DIR = "grails.project.work.dir"

    /**
     * The name of the system property for {@link #projectWarExplodedDir}.
     */
    public static final String PROJECT_WAR_EXPLODED_DIR = "grails.project.war.exploded.dir"

    /**
     * The name of the system property for {@link #projectPluginsDir}.
     */
    public static final String PLUGINS_DIR = "grails.project.plugins.dir"

    /**
     * The name of the system property for {@link #globalPluginsDir}.
     */
    public static final String GLOBAL_PLUGINS_DIR = "grails.global.plugins.dir"

    /**
     * The name of the system property for {@link #
         }.
     */
    public static final String PROJECT_RESOURCES_DIR = "grails.project.resource.dir"

    /**
     * The name of the system property for {@link #sourceDir}.
     */
    public static final String PROJECT_SOURCE_DIR = "grails.project.source.dir"

    /**
     * The name of the system property for {@link BuildSettings#webXmlLocation}.
     */
    public static final String PROJECT_WEB_XML_FILE = "grails.project.web.xml"
    /**
     * The name of the system property for {@link #classesDir}.
     */
    public static final String PROJECT_CLASSES_DIR = "grails.project.class.dir"
    /**
     * The name of the system property for {@link #pluginClassesDir}.
     */
    public static final String PROJECT_PLUGIN_CLASSES_DIR = "grails.project.plugin.class.dir"

    /**
     * The name of the system property for {@link #testClassesDir}.
     */
    public static final String PROJECT_TEST_CLASSES_DIR = "grails.project.test.class.dir"

    /**
     * The name of the system property for {@link #testReportsDir}.
     */
    public static final String PROJECT_TEST_REPORTS_DIR = "grails.project.test.reports.dir"

    /**
     * The name of the system property for {@link #testReportsDir}.
     */
    public static final String PROJECT_DOCS_OUTPUT_DIR = "grails.project.docs.output.dir"

    /**
     * The name of the system property for {@link #testSourceDir}.
     */
    public static final String PROJECT_TEST_SOURCE_DIR = "grails.project.test.source.dir"

    /**
     * The name of the system property for {@link #projectTargetDir}.
     */
    public static final String PROJECT_TARGET_DIR = "grails.project.target.dir"

    /**
     * The name of the WAR file of the project
     */
    public static final String PROJECT_WAR_FILE = "grails.project.war.file"

    /**
     * The name of the system property for enabling osgi headers in the WAR Manifest
     */
    public static final String PROJECT_WAR_OSGI_HEADERS = "grails.project.war.osgi.headers"

    /**
     * The name of the system property for multiple {@link #buildListeners}.
     */
    public static final String BUILD_LISTENERS = "grails.build.listeners"

    /**
     * The name of the system property for enabling verbose compilation {@link #verboseCompile}.
     */
    public static final String VERBOSE_COMPILE = "grails.project.compile.verbose"

    /**
     * A system property with this name is populated in the preparation phase of functional testing
     * with the base URL that tests should be run against.
     */
    public static final String FUNCTIONAL_BASE_URL_PROPERTY = 'grails.testing.functional.baseUrl'

    /**
     * The name of the working directory for commands that don't belong to a project (like create-app)
     */
    public static final String CORE_WORKING_DIR_NAME = '.core'

    /**
     *  A property name to enable/disable AST conversion of closures actions&tags to methods
     */

    public static final String CONVERT_CLOSURES_KEY = "grails.compile.artefacts.closures.convert"


    /**
     * The base directory for the build, which is normally the root
     * directory of the current project. If a command is run outside
     * of a project, then this will be the current working directory
     * that the command was launched from.
     */
    File baseDir

    /** Location of the current user's home directory - equivalent to "user.home" system property. */
    File userHome

    /**
     * Location of the Grails distribution as usually identified by
     * the GRAILS_HOME environment variable. This value may be
     * <code>null</code> if GRAILS_HOME is not set, for example if a
     * project uses the Grails JAR files directly.
     */
    File grailsHome

    /** The version of Grails being used for the current script. */
    String grailsVersion

    /** The environment for the current script. */
    String grailsEnv

    /** <code>true</code> if the default environment for a script should be used. */
    boolean defaultEnv

    /**
     * Whether the project required build dependencies are externally configured (by Maven for example) or not
     */
    boolean dependenciesExternallyConfigured = false

    /** The location of the Grails working directory where non-project-specific temporary files are stored. */
    File grailsWorkDir

    /** The location of the project working directory for project-specific temporary files. */
    File projectWorkDir

    /** The location of the project target directory where reports, artifacts and so on are output. */
    File projectTargetDir

    /** The location of the Grails WAR directory where exploded WAR is built. */
    File projectWarExplodedDir

    /**
     * The WAR file of the project
     */
    File projectWarFile

    /**
     * Setting for whether or not to enable OSGI headers in the WAR Manifest, can be overridden via -verboseCompile(=[true|false])?
     */
    boolean projectWarOsgiHeaders = false

    /** The location to which Grails compiles a project's classes. */
    File classesDir

    /** The location to which Grails compiles a project's test classes. */
    File testClassesDir

    /** The location to which Grails compiles a project's plugin classes. */
    File pluginClassesDir

    /** The location where Grails keeps temporary copies of a project's resources. */
    File resourcesDir

    /** The location of the plain source. */
    File sourceDir

    /** The location of the test reports. */
    File testReportsDir

    /** The location of the documentation output. */
    File docsOutputDir

    /** The location of the test source. */
    File testSourceDir

    /** The root loader for the build. This has the required libraries on the classpath. */
    URLClassLoader rootLoader

    /**
     * The settings used to establish the HTTP proxy to use for dependency resolution etc.
     */
    ConfigObject proxySettings = new ConfigObject()

    /**
     * The file containing the proxy settings
     */
    File proxySettingsFile;

    /** Implementation of the "grailsScript()" method used in Grails scripts. */
    Closure grailsScriptClosure

    /**
     * A Set of plugin names that represent the default set of plugins installed when creating Grails applications
     */
    Set defaultPluginSet

    /**
     * A Set of plugin names and versions that represent the default set of plugins installed when creating Grails applications
     */
    Map defaultPluginMap

    /**
     * Location of the generated web.xml file
     */
    File webXmlLocation

    /**
     * List of jars provided in the applications 'lib' directory
     */
    List applicationJars = []

    List buildListeners = []

    boolean convertClosuresArtefacts = false

    /**
     * Setting for whether or not to enable verbose compilation, can be overridden via -verboseCompile(=[true|false])?
     */
    boolean verboseCompile = false

    /**
     * Return whether the BuildConfig has been modified
     */
    boolean modified = false

    final GrailsCoreDependencies coreDependencies

    private List<File> compileDependencies = []
    private boolean defaultCompileDepsAdded = false

    /** List containing the compile-time dependencies of the app as File instances. */
    List<File> getCompileDependencies() {
        if (!defaultCompileDepsAdded) {
            compileDependencies += defaultCompileDependencies
            defaultCompileDepsAdded = true
        }
        return compileDependencies
    }

    /**
     * Sets the compile time dependencies for the project
     */
    void setCompileDependencies(List<File> deps) {
        compileDependencies = deps
    }

    /** List containing the default (resolved via the dependencyManager) compile-time dependencies of the app as File instances. */
    @Lazy List<File> defaultCompileDependencies = {
        Message.info "Resolving [compile] dependencies..."
        def jarFiles = dependencyManager
                            .resolveDependencies(IvyDependencyManager.COMPILE_CONFIGURATION)
                            .getArtifactsReports(null, false)
                            .localFile + applicationJars
        Message.debug("Resolved jars for [compile]: ${{->jarFiles.join('\n')}}")
        resolveCache['compile'] = jarFiles
        storeCache()
        return jarFiles
    }()

    private List<File> testDependencies = []
    private boolean defaultTestDepsAdded = false

    /** List containing the test-time dependencies of the app as File instances. */
    List<File> getTestDependencies() {
        if (!defaultTestDepsAdded) {
            testDependencies += defaultTestDependencies
            defaultTestDepsAdded = true
        }
        return testDependencies
    }

    /**
     * Sets the test time dependencies for the project
     */
    void setTestDependencies(List<File> deps) {
        testDependencies = deps
    }

    /** List containing the default test-time dependencies of the app as File instances. */
    @Lazy List<File> defaultTestDependencies = {
        Message.info "Resolving [test] dependencies..."
        def jarFiles = dependencyManager
                            .resolveDependencies(IvyDependencyManager.TEST_CONFIGURATION)
                            .getArtifactsReports(null, false)
                            .localFile + applicationJars
        Message.debug("Resolved jars for [test]: ${{->jarFiles.join('\n')}}")
        resolveCache['test'] = jarFiles
        storeCache()
        return jarFiles
    }()

    private List<File> runtimeDependencies = []
    private boolean defaultRuntimeDepsAdded = false

    /** List containing the runtime dependencies of the app as File instances. */
    List<File> getRuntimeDependencies() {
        if (!defaultRuntimeDepsAdded) {
            runtimeDependencies += defaultRuntimeDependencies
            defaultRuntimeDepsAdded = true
        }
        return runtimeDependencies
    }

    /**
     * Sets the runtime dependencies for the project
     */
    void setRuntimeDependencies(List<File> deps) {
        runtimeDependencies = deps
    }

    /** List containing the default runtime-time dependencies of the app as File instances. */
    @Lazy List<File> defaultRuntimeDependencies = {
        Message.info "Resolving [runtime] dependencies..."
        def jarFiles = dependencyManager
                   .resolveDependencies(IvyDependencyManager.RUNTIME_CONFIGURATION)
                   .getArtifactsReports(null, false)
                   .localFile + applicationJars
        Message.debug("Resolved jars for [runtime]: ${{->jarFiles.join('\n')}}")
        resolveCache['runtime'] = jarFiles
        storeCache()
        return jarFiles
    }()

    private List<File> providedDependencies = []
    private boolean defaultProvidedDepsAdded = false

    /** List containing the runtime dependencies of the app as File instances. */
    List<File> getProvidedDependencies() {
        if (!defaultProvidedDepsAdded) {
            providedDependencies += defaultProvidedDependencies
            defaultProvidedDepsAdded = true
        }
        return providedDependencies
    }

    /**
     * Sets the runtime dependencies for the project
     */
    void setProvidedDependencies(List<File> deps) {
        providedDependencies = deps
    }

    /** List containing the dependencies needed at development time, but provided by the container at runtime **/
    @Lazy List<File> defaultProvidedDependencies = {
        if (dependenciesExternallyConfigured) {
            return []
        }
        Message.info "Resolving [provided] dependencies..."
        def jarFiles = dependencyManager
                       .resolveDependencies(IvyDependencyManager.PROVIDED_CONFIGURATION)
                       .getArtifactsReports(null, false)
                       .localFile

        Message.debug("Resolved jars for [provided]: ${{->jarFiles.join('\n')}}")
        resolveCache['provided'] = jarFiles
        storeCache()

        return jarFiles
    }()

    private List<File> buildDependencies = []
    private boolean defaultBuildDepsAdded = false

    /** List containing the runtime dependencies of the app as File instances. */
    List<File> getBuildDependencies() {
        if (!defaultBuildDepsAdded) {
            buildDependencies += defaultDuildDependencies
            defaultBuildDepsAdded = true
        }
        return buildDependencies
    }

    /**
     * Sets the runtime dependencies for the project
     */
    void setBuildDependencies(List<File> deps) {
        buildDependencies = deps
    }
    /**
     * List containing the dependencies required for the build system only
     */
    @Lazy List<File> defaultDuildDependencies = {
        if (dependenciesExternallyConfigured) {
            return []
        }

        Message.info "Resolving [build] dependencies..."
        def jarFiles = dependencyManager
                           .resolveDependencies(IvyDependencyManager.BUILD_CONFIGURATION)
                           .getArtifactsReports(null, false)
                           .localFile + applicationJars

        Message.debug("Resolved jars for [build]: ${{->jarFiles.join('\n')}}")
        resolveCache['build'] = jarFiles
        storeCache()

        return jarFiles
    }()

    /**
     * Manages dependencies and dependency resolution in a Grails application
     */
    IvyDependencyManager dependencyManager

    /*
     * This is an unclever solution for handling "sticky" values in the
     * project paths, but trying to be clever so far has failed. So, if
     * the values of properties such as "grailsWorkDir" are set explicitly
     * (from outside the class), then they are not overridden by system
     * properties/build config.
     *
     * TODO Sort out this mess. Must decide on what can set this properties,
     * when, and how. Also when and how values can be overridden. This
     * is critically important for the Maven and Ant support.
     */
    private boolean grailsWorkDirSet
    private boolean projectWorkDirSet
    private boolean projectTargetDirSet
    private boolean projectWarExplodedDirSet
    private boolean classesDirSet
    private boolean testClassesDirSet
    private boolean pluginClassesDirSet
    private boolean resourcesDirSet
    private boolean sourceDirSet
    private boolean webXmlFileSet
    private boolean testReportsDirSet
    private boolean docsOutputDirSet
    private boolean testSourceDirSet
    private boolean projectWarFileSet
    private boolean projectWarOsgiHeadersSet
    private boolean buildListenersSet
    private boolean verboseCompileSet
    private boolean convertClosuresArtefactsSet
    private String resolveChecksum
    private Map resolveCache = new ConcurrentHashMap()

    BuildSettings() {
        this(null)
    }

    BuildSettings(File grailsHome) {
        this(grailsHome, null)
    }

    BuildSettings(File grailsHome, File baseDir) {
        userHome = new File(System.getProperty("user.home"))

        if (grailsHome) this.grailsHome = grailsHome

        // Load the 'build.properties' file from the classpath and
        // retrieve the Grails version from it.
        Properties buildProps = new Properties()
        try {
            loadBuildPropertiesFromClasspath(buildProps)
            grailsVersion = buildProps.'grails.version'
        }
        catch (IOException ex) {
            throw new IOException("Unable to find 'build.properties' - make " +
                    "that sure the 'grails-core-*.jar' file is on the classpath.")
        }

        coreDependencies = new GrailsCoreDependencies(grailsVersion)

        // If 'grailsHome' is set, add the JAR file dependencies.
        defaultPluginMap = [hibernate:grailsVersion, tomcat:grailsVersion]
        defaultPluginSet = defaultPluginMap.keySet()

        // Update the base directory. This triggers some extra config.
        setBaseDir(baseDir)

        // The "grailsScript" closure definition. Returns the location
        // of the corresponding script file if GRAILS_HOME is set,
        // otherwise it loads the script class using the Gant classloader.
        grailsScriptClosure = {String name ->
            def potentialScript = new File("${grailsHome}/scripts/${name}.groovy")
            potentialScript = potentialScript.exists() ? potentialScript : new File("${grailsHome}/scripts/${name}_.groovy")
            if (potentialScript.exists()) {
                return potentialScript
            }
            else {
                try {
                    return classLoader.loadClass("${name}_")
                }
                catch (e) {
                    return classLoader.loadClass(name)
                }
            }
        }
    }

    private storeCache() {
        projectWorkDir.mkdirs()
        if (resolveChecksum) {
            try {
                def cachedResolve = new File(projectWorkDir, "${resolveChecksum}.resolve")
                cachedResolve.withOutputStream { output ->
                    def oos = new ObjectOutputStream(output)
                    oos.writeObject(resolveCache)

                }
            }
            catch(e) {
                // failed to cache for some reason, probably I/O related. Ignore.
            }

        }
    }

    protected def loadBuildPropertiesFromClasspath(Properties buildProps) {
        InputStream stream = getClass().classLoader.getResourceAsStream("grails.build.properties")
        if (stream == null) {
            stream = getClass().classLoader.getResourceAsStream("build.properties")
        }
        if (stream) {
            buildProps.load(stream)
        }
    }

    /**
     * Returns the current base directory of this project.
     */
    File getBaseDir() { baseDir }

    /**
     * <p>Changes the base directory, making sure that everything that
     * depends on it gets refreshed too. If you have have previously
     * loaded a configuration file, you should load it again after
     * calling this method.</p>
     * <p><b>Warning</b> This method resets the project paths, so if
     * they have been set manually by the caller, then that information
     * will be lost!</p>
     */
    void setBaseDir(File newBaseDir) {
        baseDir = newBaseDir ?: establishBaseDir()
        // Initialize Metadata
        Metadata.getInstance(new File(baseDir, "application.properties"))

        // Set up the project paths, using an empty config for now. The
        // paths will be updated if and when a BuildConfig configuration
        // file is loaded.
        config = new ConfigObject()
        establishProjectStructure()

        if (baseDir) {
            // Add the application's libraries.
            def appLibDir = new File(baseDir, "lib")
            if (appLibDir.exists()) {
                appLibDir.eachFileMatch(JAR_PATTERN) {
                    applicationJars << it
                }
            }
        }
    }

    File getGrailsWorkDir() { grailsWorkDir }

    void setGrailsWorkDir(File dir) {
        grailsWorkDir = dir
        grailsWorkDirSet = true
    }

    File getProjectWorkDir() { projectWorkDir }

    void setProjectWorkDir(File dir) {
        projectWorkDir = dir
        projectWorkDirSet = true
    }

    File getProjectTargetDir() { projectTargetDir }

    void setProjectTargetDir(File dir) {
        projectTargetDir = dir
        projectTargetDirSet = true
    }

    File getProjectWarFile() { projectWarFile }

    void setProjectWarFile(File file) {
        projectWarFile = file
        projectWarFileSet = true
    }

    File getProjectWarExplodedDir() { projectWarExplodedDir }

    void setProjectWarExplodedDir(File dir) {
        projectWarExplodedDir = dir
        projectWarExplodedDirSet = true
    }

    boolean getConvertClosuresArtefacts() { convertClosuresArtefacts }

    void setConvertClosuresArtefacts(boolean convert) {
        convertClosuresArtefacts = convert
        convertClosuresArtefactsSet = true
    }

    boolean getProjectWarOsgiHeaders() { projectWarOsgiHeaders }

    void setProjectWarOsgiHeaders(boolean flag) {
        projectWarOsgiHeaders = flag
        projectWarOsgiHeadersSet = true
    }

    File getClassesDir() { classesDir }

    void setClassesDir(File dir) {
        classesDir = dir
        classesDirSet = true
    }

    File getTestClassesDir() { testClassesDir }

    void setTestClassesDir(File dir) {
        testClassesDir = dir
        testClassesDirSet = true
    }

    File getPluginClassesDir() { pluginClassesDir }

    void setPluginClassesDir(File dir) {
        pluginClassesDir = dir
        pluginClassesDirSet = true
    }

    File getResourcesDir() { resourcesDir }

    void setResourcesDir(File dir) {
        resourcesDir = dir
        resourcesDirSet = true
    }

    File getSourceDir() { sourceDir }

    void setSourceDir(File dir) {
        sourceDir = dir
        sourceDirSet = true
    }

    File getTestReportsDir() { testReportsDir }

    void setTestReportsDir(File dir) {
        testReportsDir = dir
        testReportsDirSet = true
    }

    File getTestSourceDir() { testSourceDir }

    void setTestSourceDir(File dir) {
        testSourceDir = dir
        testSourceDirSet = true
    }

    void setBuildListeners(buildListeners) {
        this.buildListeners = buildListeners.toList()
        buildListenersSet = true
    }

    Object[] getBuildListeners() { buildListeners.toArray() }

    void setVerboseCompile(boolean flag) {
        verboseCompile = flag
        verboseCompileSet = true
    }

    /**
     * Loads the application's BuildSettings.groovy file if it exists
     * and returns the corresponding config object. If the file does
     * not exist, this returns an empty config.
     */
    ConfigObject loadConfig() {
        loadConfig(new File(baseDir, "grails-app/conf/BuildConfig.groovy"))
    }

    /**
     * Loads the given configuration file if it exists and returns the
     * corresponding config object. If the file does not exist, this
     * returns an empty config.
     */
    ConfigObject loadConfig(File configFile) {
        try {
            loadSettingsFile()
            if (configFile.exists()) {
                // To avoid class loader issues, we make sure that the
                // Groovy class loader used to parse the config file has
                // the root loader as its parent. Otherwise we get something
                // like NoClassDefFoundError for Script.
                GroovyClassLoader gcl = obtainGroovyClassLoader()
                ConfigSlurper slurper = createConfigSlurper()

                URL configUrl = configFile.toURI().toURL()
                Script script = gcl.parseClass(configFile)?.newInstance()

                config.setConfigFile(configUrl)
                loadConfig(slurper.parse(script))
            } else {
                postLoadConfig()
            }
        }
        catch(e) {
            StackTraceUtils.deepSanitize e
            throw e
        }

    }

    ConfigObject loadConfig(ConfigObject config) {
        try {
            this.config.merge(config)
            return config
        }
        finally {
            postLoadConfig()
        }
    }

    protected void postLoadConfig() {
        establishProjectStructure()
        parseGrailsBuildListeners()
        if (config.grails.default.plugin.set instanceof List) {
            defaultPluginSet = config.grails.default.plugin.set
        }
        flatConfig = config.flatten()

        def configURL = config.getConfigFile()
        def configFile  = configURL ? new File(configURL.getFile()) : null

        def metadataFile = Metadata.current.getMetadataFile()

        if (configFile?.exists() && metadataFile?.exists()) {
            this.resolveChecksum = ChecksumHelper.computeAsString(configFile, "md5") +
            ChecksumHelper.computeAsString(metadataFile, "md5")
        }

        def cachedResolve = new File("${projectWorkDir}/${resolveChecksum}.resolve")
        if (cachedResolve.exists()) {

            cachedResolve.withInputStream { input ->
                def ois = new ObjectInputStream(input)
                Map dependencyMap = ois.readObject()

                if (dependencyMap?.values()*.any { !it.exists() }) {
                    modified = true
                }
                else {
                    def compileDeps = dependencyMap.compile
                    def runtimeDeps = dependencyMap.runtime
                    def testDeps = dependencyMap.test
                    def buildDeps = dependencyMap.build
                    def providedDeps = dependencyMap.provided

                    if (compileDeps) {
                        this.@compileDependencies.addAll(compileDeps)
                        defaultCompileDepsAdded = true
                    }

                    if (runtimeDeps) {
                        this.@runtimeDependencies.addAll(runtimeDeps)
                        defaultRuntimeDepsAdded = true
                    }

                    if (testDeps) {
                        this.@testDependencies.addAll(testDeps)
                        defaultTestDepsAdded = true
                    }

                    if (buildDeps) {
                        this.@buildDependencies.addAll(buildDeps)
                        defaultBuildDepsAdded = true
                    }

                    if (providedDeps) {
                        this.@providedDependencies.addAll(providedDeps)
                        defaultProvidedDepsAdded = true
                    }
                }
            }
        }
        else {
            this.modified = true
        }
        configureDependencyManager(config)
    }

    protected boolean settingsFileLoaded = false
    protected ConfigObject loadSettingsFile() {
        if (!settingsFileLoaded) {
            def settingsFile = new File("$userHome/.grails/settings.groovy")
            def gcl = obtainGroovyClassLoader()
            def slurper = createConfigSlurper()
            if (settingsFile.exists()) {
                Script script = gcl.parseClass(settingsFile)?.newInstance()
                if (script) {
                    config = slurper.parse(script)
                }
            }

            this.proxySettingsFile = new File("$userHome/.grails/ProxySettings.groovy")
            if (proxySettingsFile.exists()) {
                slurper = createConfigSlurper()
                try {
                    Script script = gcl.parseClass(proxySettingsFile)?.newInstance()
                    if (script) {
                        proxySettings = slurper.parse(script)
                        def current = proxySettings.currentProxy
                        if (current) {
                            proxySettings[current]?.each { key, value ->
                                System.setProperty(key, value)
                            }
                        }
                    }
                }
                catch (e) {
                    println "WARNING: Error configuring proxy settings: ${e.message}"
                }

            }

            settingsFileLoaded = true
        }
        config
    }

    private GroovyClassLoader gcl
    GroovyClassLoader obtainGroovyClassLoader() {
        if (gcl == null) {
            gcl = rootLoader != null ? new GroovyClassLoader(rootLoader) : new GroovyClassLoader(ClassLoader.getSystemClassLoader())
        }
        return gcl
    }

    def configureDependencyManager(ConfigObject config) {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_WARN)

        Metadata metadata = Metadata.current
        def appName = metadata.getApplicationName() ?: "grails"
        def appVersion = metadata.getApplicationVersion() ?: grailsVersion

        dependencyManager = new IvyDependencyManager(appName,
                appVersion, this, metadata)

        def console = GrailsConsole.instance
        dependencyManager.transferListener = { TransferEvent e ->
            switch(e.eventType) {
                case TransferEvent.TRANSFER_STARTED:
                    def resourceName = e.resource.name
                    resourceName = resourceName[resourceName.lastIndexOf('/')+1..-1]
                    console.updateStatus "Downloading: ${resourceName}"
                break
            }
        } as TransferListener


        def grailsConfig = config.grails

        if (!dependenciesExternallyConfigured) {
            grailsConfig.global.dependency.resolution = coreDependencies.createDeclaration()
            def credentials = grailsConfig.project.ivy.authentication
            if (credentials instanceof Closure) {
                dependencyManager.parseDependencies credentials
            }
        }
        else {
            // Even if the dependencies are handled externally, we still
            // to handle plugin dependencies.
            grailsConfig.global.dependency.resolution = {
                repositories {
                    grailsPlugins()
                }
            }
        }

        def dependencyConfig = grailsConfig.project.dependency.resolution
        if (!dependencyConfig) {
            dependencyConfig = grailsConfig.global.dependency.resolution
            dependencyManager.inheritsAll = true
        }
        if (dependencyConfig) {
            if (resolveCache) {
                dependencyManager.pluginsOnly = true
            }

            dependencyManager.parseDependencies dependencyConfig
        }

        // All projects need the plugins to be resolved.
        def handlePluginDirectory = pluginDependencyHandler()
        def pluginDirs = getPluginDirectories()
        for (dir in pluginDirs) {
            handlePluginDirectory(dir)
        }
    }

    Closure pluginDependencyHandler() {
        return pluginDependencyHandler(dependencyManager)
    }

    Closure pluginDependencyHandler(IvyDependencyManager dependencyManager) {
        def pluginSlurper = createConfigSlurper()

        def handlePluginDirectory = {File dir ->
            def pluginName = dir.name
            def matcher = pluginName =~ /(\S+?)-(\d\S+)/
            pluginName = matcher ? matcher[0][1] : pluginName
            // Try BuildConfig.groovy first, which should work
            // work for in-place plugins.
            def path = dir.absolutePath
            def pluginDependencyDescriptor = new File("${path}/grails-app/conf/BuildConfig.groovy")

            if (!pluginDependencyDescriptor.exists()) {
                // OK, that doesn't exist, so try dependencies.groovy.
                pluginDependencyDescriptor = new File("$path/dependencies.groovy")
            }

            if (pluginDependencyDescriptor.exists()) {
                def gcl = obtainGroovyClassLoader()

                try {
                    Script script = gcl.parseClass(pluginDependencyDescriptor)?.newInstance()
                    def pluginConfig = pluginSlurper.parse(script)
                    def pluginDependencyConfig = pluginConfig.grails.project.dependency.resolution
                    if (pluginDependencyConfig instanceof Closure) {
                        dependencyManager.parseDependencies(pluginName, pluginDependencyConfig)
                    }

                    def inlinePlugins = getInlinePluginsFromConfiguration(pluginConfig, dir)
                    if (inlinePlugins) {
                        for (File inlinePlugin in inlinePlugins) {
                            addPluginDirectory inlinePlugin, true
                            // recurse
                            def handleInlinePlugin = pluginDependencyHandler()
                            handleInlinePlugin(inlinePlugin)
                        }
                    }
                }
                catch (e) {
                    println "WARNING: Dependencies cannot be resolved for plugin [$pluginName] due to error: ${e.message}"
                }

            }
        }
        return handlePluginDirectory
    }

    ConfigSlurper createConfigSlurper() {
        def slurper = new ConfigSlurper()
        slurper.setBinding(
                basedir: baseDir.path,
                baseFile: baseDir,
                baseName: baseDir.name,
                grailsHome: grailsHome?.path,
                grailsVersion: grailsVersion,
                userHome: userHome,
                grailsSettings: this,
                appName:Metadata.current.getApplicationName(),
                appVersion:Metadata.current.getApplicationVersion())
        return slurper
    }

    private void establishProjectStructure() {
        // The third argument to "getPropertyValue()" is either the
        // existing value of the corresponding field, or if that's
        // null, a default value. This ensures that we don't override
        // settings provided by, for example, the Maven plugin.
        def props = config.toProperties()
        def metadata = Metadata.current
        if (!grailsWorkDirSet) {
            grailsWorkDir = new File(getPropertyValue(WORK_DIR, props, "${userHome}/.grails/${grailsVersion}"))
        }

        if (!projectWorkDirSet) {
            def workingDirName = metadata.getApplicationName() ?: CORE_WORKING_DIR_NAME
            projectWorkDir = new File(getPropertyValue(PROJECT_WORK_DIR, props, "$grailsWorkDir/projects/${workingDirName}"))
        }

        if (!projectTargetDirSet) {
            projectTargetDir = new File(getPropertyValue(PROJECT_TARGET_DIR, props, "$baseDir/target"))
        }

        if (!projectWarFileSet) {
            def version = metadata.getApplicationVersion()
            def appName = metadata.getApplicationName() ?: baseDir.name
            def warName = version ? "$baseDir/target/${appName}-${version}.war" : "$baseDir/target/${appName}.war"

            projectWarFile = new File(getPropertyValue(PROJECT_WAR_FILE, props, warName))
        }

        if (!projectWarExplodedDirSet) {
            projectWarExplodedDir = new File(getPropertyValue(PROJECT_WAR_EXPLODED_DIR, props,  "${projectWorkDir}/stage"))
        }

        if (!convertClosuresArtefactsSet) {
            convertClosuresArtefacts = getPropertyValue(CONVERT_CLOSURES_KEY, props,  'false').toBoolean()
            System.setProperty(CONVERT_CLOSURES_KEY, "$convertClosuresArtefacts")
        }

        if (!projectWarOsgiHeadersSet) {
            projectWarOsgiHeaders = getPropertyValue(PROJECT_WAR_OSGI_HEADERS, props, 'true').toBoolean()
        }

        if (!classesDirSet) {
            classesDir = new File(getPropertyValue(PROJECT_CLASSES_DIR, props, "$projectWorkDir/classes"))
        }

        if (!testClassesDirSet) {
            testClassesDir = new File(getPropertyValue(PROJECT_TEST_CLASSES_DIR, props, "$projectWorkDir/test-classes"))
        }

        if (!pluginClassesDirSet) {
            pluginClassesDir = new File(getPropertyValue(PROJECT_PLUGIN_CLASSES_DIR, props, "$projectWorkDir/plugin-classes"))
        }

        if (!resourcesDirSet) {
            resourcesDir = new File(getPropertyValue(PROJECT_RESOURCES_DIR, props, "$projectWorkDir/resources"))
        }

        if (!sourceDirSet) {
            sourceDir = new File(getPropertyValue(PROJECT_SOURCE_DIR, props, "$baseDir/src"))
        }

        if (!webXmlFileSet) {
            webXmlLocation = new File(getPropertyValue(PROJECT_WEB_XML_FILE, props, "$resourcesDir/web.xml"))
        }

        if (!projectPluginsDirSet) {
            this.@projectPluginsDir = new File(getPropertyValue(PLUGINS_DIR, props, "$projectWorkDir/plugins"))
        }

        if (!globalPluginsDirSet) {
            this.@globalPluginsDir = new File(getPropertyValue(GLOBAL_PLUGINS_DIR, props, "$grailsWorkDir/global-plugins"))
        }

        if (!testReportsDirSet) {
            testReportsDir = new File(getPropertyValue(PROJECT_TEST_REPORTS_DIR, props, "${projectTargetDir}/test-reports"))
        }

        if (!docsOutputDirSet) {
            docsOutputDir = new File(getPropertyValue(PROJECT_DOCS_OUTPUT_DIR, props, "${projectTargetDir}/docs"))
        }

        if (!testSourceDirSet) {
            testSourceDir = new File(getPropertyValue(PROJECT_TEST_SOURCE_DIR, props, "${baseDir}/test"))
        }

        if (!verboseCompileSet) {
            verboseCompile = getPropertyValue(VERBOSE_COMPILE, props, '').toBoolean()
        }
    }

    protected void parseGrailsBuildListeners() {
        if (!buildListenersSet) {
            def listenersValue = System.getProperty(BUILD_LISTENERS) ?: config.grails.build.listeners // Anyway to use the constant to do this?
            if (listenersValue) {
                def add = {
                    if (it instanceof String) {
                        it.split(',').each { this.@buildListeners << it }
                    } else if (it instanceof Class) {
                        this.@buildListeners << it
                    } else {
                        throw new IllegalArgumentException("$it is not a valid value for $BUILD_LISTENERS")
                    }
                }

                (listenersValue instanceof Collection) ? listenersValue.each(add) : add(listenersValue)
            }
            buildListenersSet = true
        }
    }

    private getPropertyValue(String propertyName, Properties props, String defaultValue) {
        // First check whether we have a system property with the given name.
        def value = getValueFromSystemOrBuild(propertyName, props)

        // Return the BuildSettings value if there is one, otherwise
        // use the default.
        return value != null ? value : defaultValue
    }

    private getValueFromSystemOrBuild(String propertyName, Properties props) {
        def value = System.getProperty(propertyName)
        if (value != null) return value

        // Now try the BuildSettings config.
        value = props[propertyName]
        return value
    }

    private File establishBaseDir() {
        def sysProp = System.getProperty(APP_BASE_DIR)
        def baseDir
        if (sysProp) {
            baseDir = sysProp == '.' ? new File("") : new File(sysProp)
        }
        else {
            baseDir = new File("")
            if (!new File(baseDir, "grails-app").exists()) {
                // be careful with this next step...
                // baseDir.parentFile will return null since baseDir is new File("")
                // baseDir.absoluteFile needs to happen before retrieving the parentFile
                def parentDir = baseDir.absoluteFile.parentFile

                // keep moving up one directory until we find
                // one that contains the grails-app dir or get
                // to the top of the filesystem...
                while (parentDir != null && !new File(parentDir, "grails-app").exists()) {
                    parentDir = parentDir.parentFile
                }

                if (parentDir != null) {
                    // if we found the project root, use it
                    baseDir = parentDir
                }
            }
        }
        return baseDir.canonicalFile
    }

    File getWebXmlLocation() { webXmlLocation }

    void setWebXmlLocation(File location) {
        webXmlLocation = location
        webXmlFileSet = true
    }

    String getFunctionalTestBaseUrl() {
        System.getProperty(FUNCTIONAL_BASE_URL_PROPERTY)
    }
}
