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

import org.codehaus.groovy.grails.io.support.IOUtils

import grails.build.logging.GrailsConsole
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProcess

import static grails.build.logging.GrailsConsole.instance as CONSOLE
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

import org.codehaus.groovy.grails.cli.support.ClasspathConfigurer
import org.codehaus.groovy.grails.cli.support.OwnerlessClosure
import org.codehaus.groovy.grails.io.support.IOUtils
import org.codehaus.groovy.grails.resolve.DependencyManager
import org.codehaus.groovy.grails.resolve.DependencyManagerConfigurer
import org.codehaus.groovy.grails.resolve.DependencyReport
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor
import org.codehaus.groovy.grails.resolve.GrailsCoreDependencies
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.codehaus.groovy.runtime.StackTraceUtils

/**
 * <p>Represents the project paths and other build settings
 * that the user can change when running the Grails commands. Defaults
 * are provided for all settings, but the user can override those by
 * setting the appropriate system property or specifying a value for
 * it in the BuildConfig.groovy file.</p>
 * <p><b>Warning</b> The behaviour is poorly defined if you explicitly
 * set some of the project paths (such as {@link BuildSettings#projectWorkDir}),
 * but not others. If you set one of them explicitly, set all of them
 * to ensure consistent behaviour.</p>
 */
class BuildSettings extends AbstractBuildSettings {

    static final String BUILD_SCOPE = "build"
    static final String COMPILE_SCOPE = "compile"
    static final String RUNTIME_SCOPE = "runtime"
    static final String TEST_SCOPE = "test"
    static final String PROVIDED_SCOPE = "provided"

    static final String BUILD_SCOPE_DESC = "Dependencies for the build system only"
    static final String COMPILE_SCOPE_DESC = "Dependencies placed on the classpath for compilation"
    static final String RUNTIME_SCOPE_DESC = "Dependencies needed at runtime but not for compilation"
    static final String TEST_SCOPE_DESC = "Dependencies needed for test compilation and execution but not at runtime"
    static final String PROVIDED_SCOPE_DESC = "Dependencies needed at development time, but not during deployment"

    static final Map<String, String> SCOPE_TO_DESC = [
        (BUILD_SCOPE): BUILD_SCOPE_DESC,
        (PROVIDED_SCOPE): PROVIDED_SCOPE_DESC,
        (COMPILE_SCOPE): COMPILE_SCOPE_DESC,
        (RUNTIME_SCOPE): RUNTIME_SCOPE_DESC,
        (TEST_SCOPE): TEST_SCOPE_DESC
    ]

    static final Pattern JAR_PATTERN = ~/^\S+\.jar$/

    /**
     * The compiler source level to use
     */
    public static final String COMPILER_SOURCE_LEVEL = "grails.project.source.level"

    /**
     * The dependency resolver to use
     */
    public static final String DEPENDENCY_RESOLVER = "grails.project.dependency.resolver"

    /**
     * The compiler source level to use
     */
    public static final String COMPILER_TARGET_LEVEL = "grails.project.target.level"
    /**
     * The version of the servlet API
     */
    public static final String SERVLET_VERSION = "grails.servlet.version"
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

    public static final String OFFLINE_MODE= "grails.offline.mode"

    /**
     * The name of the system property for {@link #projectWarExplodedDir}.
     */
    public static final String PROJECT_WAR_EXPLODED_DIR = "grails.project.war.exploded.dir"

    /**
     * The name of the system property for {@link #pluginStagingDir}.
     */
    public static final String PLUGIN_STAGING_DIR = "grails.project.plugin.staging.dir"

    /**
     * The name of the system property for {@link #pluginIncludeSource}.
     */
    public static final String PLUGIN_INCLUDE_SOURCE = "grails.project.plugin.includeSource"

    /**
     * The name of the system property for {@link #projectPluginsDir}.
     */
    public static final String PLUGINS_DIR = "grails.project.plugins.dir"

    /**
     * The name of the system property for {@link #globalPluginsDir}.
     */
    public static final String GLOBAL_PLUGINS_DIR = "grails.global.plugins.dir"

    /**
     * The name of the system property for {@link #}.
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
     * The name of the system property for {@link #pluginBuildClassesDir}.
     */
    public static final String PROJECT_PLUGIN_BUILD_CLASSES_DIR = "grails.project.plugin.build.class.dir"

    /**
     * The name of the system property for {@link #pluginProvidedClassesDir}.
     */
    public static final String PROJECT_PLUGIN_PROVIDED_CLASSES_DIR = "grails.project.plugin.provided.class.dir"

    /**
     * The name of the system property for {@link #testClassesDir}.
     */
    public static final String PROJECT_TEST_CLASSES_DIR = "grails.project.test.class.dir"

    /**
     * The name of the system property for {@link #testReportsDir}.
     */
    public static final String PROJECT_TEST_REPORTS_DIR = "grails.project.test.reports.dir"

    /**
     * The name of the system property for {@link #docsOutputDir}.
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
     * The name of the WAR file of the project
     */
    public static final String PROJECT_AUTODEPLOY_DIR = "grails.project.autodeploy.dir"

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
     *  Property name to enable/disable script event time logging.
     */
    public static final String LOG_SCRIPT_TIMING_KEY = 'grails.script.logTiming'

    /**
     * The base directory for the build, which is normally the root
     * directory of the current project. If a command is run outside
     * of a project, then this will be the current working directory
     * that the command was launched from.
     */
    File baseDir

    /** Location of the current user's home directory - equivalent to "user.home" system property.  */
    File userHome

    /**
     * Location of the Grails distribution as usually identified by
     * the GRAILS_HOME environment variable. This value may be
     * <code>null</code> if GRAILS_HOME is not set, for example if a
     * project uses the Grails JAR files directly.
     */
    File grailsHome

    /** The version of Grails being used for the current script.  */
    String grailsVersion

    /** The environment for the current script.  */
    String grailsEnv

    /**
     * The compiler source level to use
     */
    String compilerSourceLevel

    /**
     * The compiler target level to use
     */
    String compilerTargetLevel = "1.6"

    /**
     * The dependency resolution engine to use (aether or ivy)
     */
    String dependencyResolver = "ivy"

    /** <code>true</code> if the default environment for a script should be used.  */
    boolean defaultEnv

    /**
     * whether to include source attachments in a resolve
     */
    boolean includeSource
    /**
    * whether to include javadoc attachments in a resolve
     */
    boolean includeJavadoc

    /**
     * Whether the project required build dependencies are externally configured (by Maven for example) or not
     */
    boolean dependenciesExternallyConfigured = false

    /**
     * Whether to enable resolving dependencies
     */
    boolean enableResolve = true

    /** The location of the Grails working directory where non-project-specific temporary files are stored.  */
    File grailsWorkDir

    /** The location of the project working directory for project-specific temporary files.  */
    File projectWorkDir

    /** The location of the project target directory where reports, artifacts and so on are output.  */
    File projectTargetDir

    /** The location of the Grails WAR directory where exploded WAR is built.  */
    File projectWarExplodedDir

    /** The location of the temporary directory where plugins are staged before being bundled into a ZIP or JAR file.  */
    File pluginStagingDir

    /** Whether to include the source in binary plugin JAR files.  */
    Boolean pluginIncludeSource

    /**
     * The WAR file of the project
     */
    File projectWarFile

    /**
     * Directory where additional war files to be autodeployed are located
     */
    File autodeployDir

    /**
     * Setting for whether or not to enable OSGI headers in the WAR Manifest, can be overridden via -verboseCompile(=[true|false])?
     */
    boolean projectWarOsgiHeaders = false

    /** The location to which Grails compiles a project's classes.  */
    File classesDir

    /** The location to which Grails compiles a project's test classes and any test scoped plugin classes.  */
    File testClassesDir

    /** The location to which Grails compiles a project's plugin classes. Plugin classes that are compile or runtime scoped will be compiled to this directory  */
    File pluginClassesDir

    /**
     * The location to which Grails compiles build scoped plugins classes.
     */
    File pluginBuildClassesDir

    /**
     * The location to which Grails compiles provided scoped plugins classes.
     */
    File pluginProvidedClassesDir

    /** The location where Grails keeps temporary copies of a project's resources.  */
    File resourcesDir

    /** The location of the plain source.  */
    File sourceDir

    /** The location of the test reports.  */
    File testReportsDir

    /** The location of the documentation output.  */
    File docsOutputDir

    /** The location of the test source.  */
    File testSourceDir

    /**
     * The version of the servlet API used
     */
    String servletVersion = "2.5"

    /** The root loader for the build. This has the required libraries on the classpath.  */
    URLClassLoader rootLoader

    /**
     * The settings used to establish the HTTP proxy to use for dependency resolution etc.
     */
    ConfigObject proxySettings = new ConfigObject()

    /**
     * The file containing the proxy settings
     */
    File proxySettingsFile

    /**
     * Fork Settings. These are the default settings used to control forked mode, and what
     */
    Map<String, Object> forkSettings = [run:false, test:false, console:false, shell:false, compile:false]

    /** Implementation of the "grailsScript()" method used in Grails scripts.  */
    private boolean useMavenDependencyResolver

    Closure getGrailsScriptClosure() {
        return new OwnerlessClosure() {
            Object doCall(String name) {
                if(grailsHome != null) {

                    def potentialScript = new File(grailsHome, "grails-scripts/src/main/scripts/${name}.groovy")
                    potentialScript = potentialScript.exists() ? potentialScript : new File(grailsHome, "grails-scripts/src/main/scripts/${name}_.groovy")

                    if (potentialScript.exists()) {
                        return potentialScript
                    }
                }
                try {
                    return classLoader.loadClass("${name}_")
                }
                catch (e) {
                    return classLoader.loadClass(name)
                }
            }
        }
    }

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
    List<File> applicationJars = []

    List buildListeners = []

    List<File> pluginDependencies = []

    boolean convertClosuresArtefacts = false

    /**
     * Setting for whether or not to enable verbose compilation, can be overridden via -verboseCompile(=[true|false])?
     */
    boolean verboseCompile = false

    /**
     * Return whether the BuildConfig has been modified
     */
    boolean modified = false

    /**
     * Whether the build is allowed to connect to remote servers to resolve dependencies
     */
    boolean offline = false

    boolean logScriptTiming = false

    GrailsCoreDependencies coreDependencies

    private List<File> compileDependencies = []
    private boolean defaultCompileDepsAdded = false

    private List<File> internalPluginCompileDependencies = []
    private List<File> internalPluginTestDependencies = []
    private List<File> internalPluginBuildDependencies = []
    private List<File> internalPluginRuntimeDependencies = []
    private List<File> internalPluginProvidedDependencies = []

    /** List containing the compile-time dependencies of the app as File instances.  */
    @CompileStatic
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
    @CompileStatic
    void setCompileDependencies(List<File> deps) {
        def jarFiles = findAndRemovePluginDependencies("compile", deps, internalPluginCompileDependencies)
        compileDependencies = jarFiles
    }

    DependencyReport buildResolveReport
    DependencyReport compileResolveReport
    DependencyReport testResolveReport
    DependencyReport runtimeResolveReport
    DependencyReport providedResolveReport

    /** List containing the default (resolved via the dependencyManager) compile-time dependencies of the app as File instances.  */
    private List<File> internalCompileDependencies
    @Lazy List<File> defaultCompileDependencies = {
        if (dependenciesExternallyConfigured) {
            return []
        }

        if (internalCompileDependencies) return internalCompileDependencies
        List<File> jarFiles
        if (shouldResolve()) {
            jarFiles = doResolve("compile", internalPluginCompileDependencies)
        }
        else {
            jarFiles = []
        }
        return jarFiles
    }()

    @CompileStatic
    List<File> doResolve(String scope, List<File> pluginZips, boolean includeAppJars = true) {
        final resolveReport = dependencyManager.resolve(scope)
        if (resolveReport.hasError()) {
            final console = GrailsConsole.getInstance()
            final error = resolveReport.getResolveError()
            console.error("Resolve error obtaining dependencies: ${error.message}", error)
        }
        ((GroovyObject)this).setProperty("${scope}ResolveReport".toString(), resolveReport )
        List<File> jarFiles
        if (includeAppJars) {
            jarFiles = []
            jarFiles.addAll(resolveReport.jarFiles)
            jarFiles.addAll(applicationJars)
        } else {
            jarFiles = resolveReport.jarFiles
        }
        pluginZips.addAll(resolveReport.pluginZips)
        this.pluginDependencies.addAll(resolveReport.pluginZips)
        pluginDependencies.unique(true)
        resolveCache[scope] = resolveReport.allArtifacts
        jarFiles
    }

    @CompileStatic
    boolean isGrailsProject() {
        return new File(getBaseDir(), "grails-app").exists()
    }

    @CompileStatic
    private List<File> findAndRemovePluginDependencies(String scope, Collection<File> jarFiles, List<File> scopePluginDependencies) {
        jarFiles = jarFiles?.findAll { File it -> it != null} ?: new ArrayList<File>()
        def pluginZips = jarFiles.findAll { File it -> it.name.endsWith(".zip") }
        for (z in pluginZips) {
            if (!pluginDependencies.contains(z)) {
                pluginDependencies.add(z)
            }
        }
        scopePluginDependencies.addAll(pluginZips)
        resolveCache[scope] = jarFiles
        jarFiles = jarFiles.findAll { File it -> !it.name.endsWith(".zip") }
        return jarFiles as List
    }

    private List<File> testDependencies = []
    private boolean defaultTestDepsAdded = false

    /** List containing the test-time dependencies of the app as File instances.  */
    @CompileStatic
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
    @CompileStatic
    void setTestDependencies(List<File> deps) {
        def jarFiles = findAndRemovePluginDependencies("test", deps, internalPluginTestDependencies)

        testDependencies = jarFiles
    }

    /** List containing the default test-time dependencies of the app as File instances.  */
    private List<File> internalTestDependencies
    @Lazy List<File> defaultTestDependencies = {
        if (dependenciesExternallyConfigured) {
            return []
        }

        if (internalTestDependencies) return internalTestDependencies
        if (shouldResolve()) {
            return doResolve("test", internalPluginTestDependencies)
        }
        else {
            return []
        }
    }()

    private List<File> runtimeDependencies = []
    private boolean defaultRuntimeDepsAdded = false

    /** List containing the runtime dependencies of the app as File instances.  */
    @CompileStatic
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
    @CompileStatic
    void setRuntimeDependencies(List<File> deps) {
        def jarFiles = findAndRemovePluginDependencies("runtime", deps, internalPluginRuntimeDependencies)
        runtimeDependencies = jarFiles
    }

    /** List containing the default runtime-time dependencies of the app as File instances.  */
    private List<File> internalRuntimeDependencies
    @Lazy List<File> defaultRuntimeDependencies = {
        if (dependenciesExternallyConfigured) {
            return []
        }

        if (internalRuntimeDependencies) return internalRuntimeDependencies
        if (shouldResolve()) {
            return doResolve("runtime", internalPluginRuntimeDependencies)
        }
        return []
    }()

    private List<File> providedDependencies = []
    private boolean defaultProvidedDepsAdded = false

    /** List containing the runtime dependencies of the app as File instances.  */
    @CompileStatic
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
    @CompileStatic
    void setProvidedDependencies(List<File> deps) {
        def jarFiles = findAndRemovePluginDependencies("provided", deps, internalPluginProvidedDependencies)
        providedDependencies = jarFiles
    }

    /** List containing the dependencies needed at development time, but provided by the container at runtime  **/
    private List<File> internalProvidedDependencies
    @Lazy List<File> defaultProvidedDependencies = {
        if (dependenciesExternallyConfigured) {
            return []
        }
        if (internalProvidedDependencies) return internalProvidedDependencies

        if (shouldResolve()) {
            return doResolve("provided", internalPluginProvidedDependencies, false)
        }
        return []
    }()

    private List<File> buildDependencies = []
    private boolean defaultBuildDepsAdded = false

    /** List containing the runtime dependencies of the app as File instances.  */
    @CompileStatic
    List<File> getBuildDependencies() {
        if (!defaultBuildDepsAdded) {
            buildDependencies += defaultBuildDependencies
            defaultBuildDepsAdded = true
        }
        return buildDependencies
    }

    /**
     * Obtains a list of source plugins that are provided time dependencies
     *
     * @return A list of the source zips
     */
    @CompileStatic
    List<File> getPluginCompileDependencies() {
        // ensure initialization
        if (!internalPluginCompileDependencies) {
            getCompileDependencies()
        }

        return internalPluginCompileDependencies
    }

    /**
     * Obtains a list of source plugins that are provided time dependencies
     *
     * @return A list of the source zips
     */
    @CompileStatic
    List<File> getPluginProvidedDependencies() {
        // ensure initialization
        if (!internalPluginProvidedDependencies) {
            getProvidedDependencies()
        }

        return internalPluginProvidedDependencies
    }

    /**
     * Obtains a list of source plugins that are runtime time dependencies
     *
     * @return A list of the source zips
     */
    @CompileStatic
    List<File> getPluginRuntimeDependencies() {
        // ensure initialization
        if (!internalPluginRuntimeDependencies) {
            getRuntimeDependencies()
        }

        return internalPluginRuntimeDependencies
    }

    /**
     * Obtains a list of source plugins that are test time dependencies
     *
     * @return A list of the source zips
     */
    @CompileStatic
    List<File> getPluginTestDependencies() {
        // ensure initialization
        if (!internalPluginTestDependencies) {
            getTestDependencies()
        }

        return internalPluginTestDependencies
    }

    /**
     * Obtains a list of source plugins that are build time dependencies
     *
     * @return A list of the source zips
     */
    @CompileStatic
    List<File> getPluginBuildDependencies() {
        // ensure initialization
        if (!internalPluginBuildDependencies) {
            getBuildDependencies()
        }

        return internalPluginBuildDependencies
    }

    /**
     * Sets the runtime dependencies for the project
     */
    @CompileStatic
    void setBuildDependencies(List<File> deps) {
        def jarFiles = findAndRemovePluginDependencies("build", deps, internalPluginBuildDependencies)
        buildDependencies = jarFiles
    }

    /**
     * List containing the dependencies required for the build system only
     */
    private List<File> internalBuildDependencies
    @Lazy List<File> defaultBuildDependencies = {
        if (dependenciesExternallyConfigured) {
            return []
        }
        if (internalBuildDependencies) return internalBuildDependencies

        if (shouldResolve()) {
            return doResolve("build", internalPluginBuildDependencies)
        }
        return []
    }()

    protected boolean shouldResolve() {
        return dependencyManager != null && enableResolve
    }

    /**
     * Manages dependencies and dependency resolution in a Grails application
     */
    DependencyManager dependencyManager

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
    private boolean pluginStagingDirSet
    private boolean pluginIncludeSourceSet
    private boolean classesDirSet
    private boolean testClassesDirSet
    private boolean pluginClassesDirSet
    private boolean pluginBuildClassesDirSet
    private boolean pluginProvidedClassesDirSet
    private boolean resourcesDirSet
    private boolean sourceDirSet
    private boolean webXmlFileSet
    private boolean testReportsDirSet
    private boolean docsOutputDirSet
    private boolean testSourceDirSet
    private boolean projectWarFileSet
    private boolean autodeployDirSet
    private boolean projectWarOsgiHeadersSet
    private boolean buildListenersSet
    private boolean verboseCompileSet
    private boolean convertClosuresArtefactsSet
    private boolean logScriptTimingSet
    private String resolveChecksum
    private Map resolveCache = new ConcurrentHashMap()
    private boolean readFromCache = false

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

        // If 'grailsHome' is set, add the JAR file dependencies.
        defaultPluginMap = [:]
        defaultPluginSet = defaultPluginMap.keySet()

        // Update the base directory. This triggers some extra config.
        setBaseDir(baseDir)

        if (![Environment.DEVELOPMENT, Environment.TEST].contains(Environment.current)) {
            modified = true
        }

        // The "grailsScript" closure definition. Returns the location
        // of the corresponding script file if GRAILS_HOME is set,
        // otherwise it loads the script class using the Gant classloader.
    }

    boolean isDependenciesExternallyConfigured() {
        return dependenciesExternallyConfigured
    }

    void setDependenciesExternallyConfigured(boolean dependenciesExternallyConfigured) {
        if(dependenciesExternallyConfigured) {
            setForkSettings([run:false, test:false, console:false, shell:false, compile:false])
        }
        this.dependenciesExternallyConfigured = dependenciesExternallyConfigured
    }

    @CompileStatic
    void storeDependencyCache() {
        projectWorkDir.mkdirs()
        if (resolveChecksum) {
            try {
                if (resolveCache.size() == 5 && !readFromCache) {
                    def cachedResolve = new File(projectWorkDir, "${resolveChecksum}.resolve")
                    cachedResolve.withOutputStream { OutputStream output ->
                        new ObjectOutputStream(output).writeObject(resolveCache)
                    }
                }
            }
            catch (e) {
                ClasspathConfigurer.cleanResolveCache(this)
            }
        }
    }

    @CompileStatic
    protected void loadBuildPropertiesFromClasspath(Properties buildProps) {
        InputStream stream = getClass().classLoader.getResourceAsStream("grails.build.properties")
        if (stream == null) {
            stream = getClass().classLoader.getResourceAsStream("build.properties")
        }
        if (stream) {
            buildProps.load(stream)
        }
    }

    /**
     * <p>Changes the base directory, making sure that everything that
     * depends on it gets refreshed too. If you have have previously
     * loaded a configuration file, you should load it again after
     * calling this method.</p>
     * <p><b>Warning</b> This method resets the project paths, so if
     * they have been set manually by the caller, then that information
     * will be lost!</p>
     */
    @CompileStatic
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
                appLibDir.eachFileMatch(JAR_PATTERN) { File it ->
                    applicationJars << it
                }
            }
        }
    }

    void setGrailsWorkDir(File dir) {
        grailsWorkDir = dir
        grailsWorkDirSet = true
    }

    void setProjectWorkDir(File dir) {
        projectWorkDir = dir
        projectWorkDirSet = true
    }

    void setProjectTargetDir(File dir) {
        projectTargetDir = dir
        projectTargetDirSet = true
    }

    void setProjectWarFile(File file) {
        projectWarFile = file
        projectWarFileSet = true
    }

    void setProjectWarExplodedDir(File dir) {
        projectWarExplodedDir = dir
        projectWarExplodedDirSet = true
    }

    void setPluginStagingDir(File dir) {
        pluginStagingDir = dir
        pluginStagingDirSet = true
    }

    void setPluginIncludeSource(boolean include) {
        pluginIncludeSource = include
        pluginIncludeSourceSet = true
    }

    void setConvertClosuresArtefacts(boolean convert) {
        convertClosuresArtefacts = convert
        convertClosuresArtefactsSet = true
    }

    void setLogScriptTiming(boolean b) {
        logScriptTiming = b
        logScriptTimingSet = true
    }

    void setProjectWarOsgiHeaders(boolean flag) {
        projectWarOsgiHeaders = flag
        projectWarOsgiHeadersSet = true
    }

    void setClassesDir(File dir) {
        classesDir = dir
        classesDirSet = true
    }

    void setTestClassesDir(File dir) {
        testClassesDir = dir
        testClassesDirSet = true
    }

    void setPluginClassesDir(File dir) {
        pluginClassesDir = dir
        pluginClassesDirSet = true
    }

    void setPluginBuildClassesDir(File dir) {
        pluginBuildClassesDir = dir
        pluginBuildClassesDirSet = true
    }

    void setPluginProvidedClassesDir(File dir) {
        pluginProvidedClassesDir = dir
        pluginProvidedClassesDirSet = true
    }

    void setResourcesDir(File dir) {
        resourcesDir = dir
        resourcesDirSet = true
    }

    void setSourceDir(File dir) {
        sourceDir = dir
        sourceDirSet = true
    }

    void setTestReportsDir(File dir) {
        testReportsDir = dir
        testReportsDirSet = true
    }

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
    ConfigObject loadConfig(String environment = null) {
        loadConfig(new File(baseDir, "grails-app/conf/BuildConfig.groovy"))
    }

    /**
     * Loads the given configuration file if it exists and returns the
     * corresponding config object. If the file does not exist, this
     * returns an empty config.
     */
    @CompileStatic
    ConfigObject loadConfig(File configFile, String environment = null) {
        loadSettingsFile()
        if (configFile.exists()) {
            // To avoid class loader issues, we make sure that the Groovy class loader used to parse the config file has
            // the root loader as its parent. Otherwise we get something like NoClassDefFoundError for Script.
            GroovyClassLoader gcl = obtainGroovyClassLoader()

            URL configUrl = configFile.toURI().toURL()
            Script script = (Script)gcl.parseClass(configFile)?.newInstance()

            config.setConfigFile(configUrl)
            return loadConfig(script)
        } else {
            postLoadConfig()
        }
    }

    /**
     * Loads the given script and returns corresponding config object. If the file does not exist, this
     * returns an empty config.
     */
    @CompileStatic
    public ConfigObject loadConfig(Script script, String environment = null) {
        ConfigSlurper slurper = createConfigSlurper()
        slurper.setEnvironment(environment)
        return loadConfig(slurper.parse(script))
    }

    @CompileStatic
    static void initialiseDefaultLog4j(ClassLoader classLoader) {
        def defaultLog4j = new Properties()
        defaultLog4j."log4j.rootLogger"="error, stdout"
        defaultLog4j."log4j.appender.stdout"="org.apache.log4j.ConsoleAppender"
        defaultLog4j."log4j.appender.stdout.layout"="org.apache.log4j.PatternLayout"

        configureDefaultLog4j(classLoader, defaultLog4j)
    }

    private static void configureDefaultLog4j(ClassLoader classLoader, Properties defaultLog4j) {
        try {
            classLoader.loadClass("org.apache.log4j.PropertyConfigurator").configure(defaultLog4j)
        } catch (e) {
            // ignore
        }
    }

    @CompileStatic
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
        useMavenDependencyResolver = dependencyResolver?.equalsIgnoreCase("aether") || dependencyResolver?.equalsIgnoreCase("maven")

        parseGrailsBuildListeners()
        if (config.grails.default.plugin.set instanceof List) {
            defaultPluginSet = config.grails.default.plugin.set
        }
        flatConfig = config.flatten()

        def configURL = config.getConfigFile()
        def configFile = configURL ? new File(configURL.getFile()) : null

        def metadataFile = Metadata.current.getMetadataFile()

        if (!modified && !useMavenDependencyResolver) {

            if (configFile?.exists() && metadataFile?.exists()) {
                resolveChecksum = IOUtils.computeChecksum(configFile, "md5") +
                    IOUtils.computeChecksum(metadataFile, "md5")
            }

            def cachedResolve = new File(projectWorkDir, "${resolveChecksum}.resolve")
            if (cachedResolve.exists()) {

                cachedResolve.withInputStream { input ->
                    Map dependencyMap = [:]
                    try {
                        def ois = new ObjectInputStream(input)
                        dependencyMap = ois.readObject()
                    } catch (e) {
                        modified = true
                        return
                    }

                    if (dependencyMap?.isEmpty()) {
                        modified = true
                    }
                    else {
                        def compileDeps = dependencyMap.compile
                        def runtimeDeps = dependencyMap.runtime
                        def testDeps = dependencyMap.test
                        def buildDeps = dependencyMap.build
                        def providedDeps = dependencyMap.provided

                        if (compileDeps) {
                            compileDeps = findAndRemovePluginDependencies("compile", compileDeps, internalPluginCompileDependencies)
                            if (compileDeps.any({ File f -> !f.exists() })) modified = true
                            internalCompileDependencies = compileDeps + applicationJars
                        } else {
                            modified = true
                        }

                        if (runtimeDeps) {
                            runtimeDeps = findAndRemovePluginDependencies("runtime", runtimeDeps, internalPluginRuntimeDependencies)
                            if (runtimeDeps.any({ File f -> !f.exists() })) modified = true
                            internalRuntimeDependencies = runtimeDeps
                        } else {
                            modified = true
                        }

                        if (testDeps) {
                            testDeps = findAndRemovePluginDependencies("test", testDeps, internalPluginTestDependencies)
                            if (testDeps.any({ File f -> !f.exists() })) modified = true
                            internalTestDependencies = testDeps
                        } else {
                            modified = true
                        }

                        if (buildDeps) {
                            buildDeps = findAndRemovePluginDependencies("build", buildDeps, internalPluginBuildDependencies)
                            if (buildDeps.any({ File f -> !f.exists() })) modified = true
                            internalBuildDependencies = buildDeps
                        } else {
                            modified = true
                        }

                        if (providedDeps) {
                            providedDeps = findAndRemovePluginDependencies("provided", providedDeps, internalPluginProvidedDependencies)
                            if (providedDeps.any({ File f -> !f.exists() })) modified = true
                            internalProvidedDependencies = providedDeps
                        } else {
                            modified = true
                        }

                        if (!modified) {
                            readFromCache = true
                        }
                    }
                }
            }
            else {
                modified = true
            }
            if (modified) {
                ClasspathConfigurer.cleanResolveCache(this)
                [internalBuildDependencies, internalCompileDependencies, internalProvidedDependencies, internalRuntimeDependencies, internalTestDependencies].each { it?.clear() }
            }
        }

        if (!Environment.isFork()) {
            dependencyManager = configureDependencyManager()
            def pluginDirs = getPluginDirectories()
            for (dir in pluginDirs) {
                handleInlinePlugin(dir.name, dir, dependencyManager)
            }
        }
    }

    @CompileStatic
    void handleInlinePlugin(String pluginName, File dir, DependencyManager dependencyManager) {

        if (!isInlinePluginLocation(dir)) {
            return
        }

        // Try BuildConfig.groovy first, which should work work for in-place plugins.
        def path = dir.absolutePath
        def pluginDependencyDescriptor = new File(path, "grails-app/conf/BuildConfig.groovy")

        if (!pluginDependencyDescriptor.exists()) {
            // OK, that doesn't exist, so try dependencies.groovy.
            pluginDependencyDescriptor = new File(path, "dependencies.groovy")
        }

        if (pluginDependencyDescriptor.exists()) {
            def gcl = obtainGroovyClassLoader()

            try {
                Script script = (Script)gcl.parseClass(pluginDependencyDescriptor)?.newInstance()
                def pluginSlurper = createConfigSlurper()
                def pluginConfig = pluginSlurper.parse(script)
                def pluginDependencyConfig = getInlinePluginDependencyConfig(pluginConfig)
                if (pluginDependencyConfig instanceof Closure) {
                    dependencyManager.parseDependencies(pluginDependencyConfig)
                }
                def inlinePlugins = getInlinePluginsFromConfiguration(pluginConfig, dir)
                if (inlinePlugins) {
                    for (File inlinePlugin in inlinePlugins) {
                        addPluginDirectory inlinePlugin, true
                        // recurse
                        handleInlinePlugin(inlinePlugin.name, inlinePlugin, dependencyManager)
                    }
                }
            }
            catch (Throwable e) {
                CONSOLE.error "WARNING: Inline plugins for [$pluginName] cannot be read due to error: ${e.message}", e
            }
        }
    }

    protected Object getInlinePluginDependencyConfig(ConfigObject pluginConfig) {
        pluginConfig.grails.project.dependency.resolution
    }

    protected boolean settingsFileLoaded = false

    @CompileStatic
    DependencyManager configureDependencyManager() {
        DependencyManagerConfigurer configurer = new DependencyManagerConfigurer()

        if (useMavenDependencyResolver && !isDependenciesExternallyConfigured()) {
            return configurer.configureAether(this)
        }
        return configurer.configureIvy(this)
    }

    @CompileStatic
    DependencyManager createNewDependencyManager() {

        if (useMavenDependencyResolver) {
            return DependencyManagerConfigurer.createAetherDependencyManager(this)
        }
        return DependencyManagerConfigurer.createIvyDependencyManager(this)
    }

    @CompileStatic
    protected ConfigObject loadSettingsFile() {
        if (!settingsFileLoaded) {
            def settingsFile = new File("$userHome/.grails/settings.groovy")
            def gcl = obtainGroovyClassLoader()
            def slurper = createConfigSlurper()
            if (settingsFile.exists()) {
                Script script = (Script)gcl.parseClass(settingsFile)?.newInstance()
                if (script) {
                    config = slurper.parse(script)
                }
            }

            proxySettingsFile = new File("$userHome/.grails/ProxySettings.groovy")
            if (proxySettingsFile.exists()) {
                slurper = createConfigSlurper()
                try {
                    Script script = (Script)gcl.parseClass(proxySettingsFile)?.newInstance()
                    if (script) {
                        proxySettings = slurper.parse(script)
                        def current = proxySettings.currentProxy
                        if (current) {
                            proxySettings[current]?.each { Map.Entry entry ->
                                System.setProperty(entry.key as String, entry.value as String)
                            }
                        }
                    }
                }
                catch (Throwable e) {
                    CONSOLE.error "WARNING: Error configuring proxy settings: ${e.message}", e
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

    boolean isRegisteredInMetadata(String pluginName) {
        if (dependencyManager instanceof IvyDependencyManager) {
            return dependencyManager.metadataRegisteredPluginNames?.contains(pluginName)
        }
        return true
    }

    @Deprecated
    boolean notDefinedInBuildConfig(String pluginName) {
        if (!(dependencyManager instanceof IvyDependencyManager)) {
            return true
        }

        def descriptors = dependencyManager.pluginDependencyDescriptors.findAll {EnhancedDefaultDependencyDescriptor desc ->
            def nonTransitive = !desc.plugin
            def exported = desc.exportedToApplication
            nonTransitive || exported
        }

        def defined = descriptors*.dependencyId*.name.contains(pluginName)
        return !defined
    }

    @CompileStatic
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
                groovyVersion: "${->coreDependencies?.groovyVersion}",
                springVersion: "${->coreDependencies?.springVersion}",
                appName: Metadata.getCurrent().getApplicationName(),
                appVersion: Metadata.getCurrent().getApplicationVersion())
        return slurper
    }

    @CompileStatic
    private void establishProjectStructure() {
        // The third argument to "getPropertyValue()" is either the existing value of the corresponding
        // field, or if that's null, a default value. This ensures that we don't override settings
        // provided by, for example, the Maven plugin.
        def props = config.toProperties()

        final forkConfig = getForkConfig()
        if ((forkConfig instanceof Map) && forkConfig && !isDependenciesExternallyConfigured()) {
            forkSettings = (Map)forkConfig
        }
        Metadata metadata = Metadata.getCurrent()

        offline = Boolean.valueOf(getPropertyValue(OFFLINE_MODE, props, String.valueOf(offline)))

        if (!grailsWorkDirSet) {
            grailsWorkDir = new File(getPropertyValue(WORK_DIR, props, "${userHome}/.grails/${grailsVersion}"))
        }

        servletVersion = getPropertyValue(SERVLET_VERSION, props, "2.5")
        metadata.setServletVersion(servletVersion)
        compilerSourceLevel = getPropertyValue(COMPILER_SOURCE_LEVEL, props, "1.6")
        compilerTargetLevel = getPropertyValue(COMPILER_TARGET_LEVEL, props, "1.6")
        dependencyResolver = getPropertyValue(DEPENDENCY_RESOLVER, props, "ivy")

        if (!projectWorkDirSet) {
            def workingDirName = metadata.getApplicationName() ?: CORE_WORKING_DIR_NAME
            projectWorkDir = new File(getPropertyValue(PROJECT_WORK_DIR, props, "$grailsWorkDir/projects/${workingDirName}"))
        }

        if (!projectTargetDirSet) {
            projectTargetDir = makeAbsolute(new File(getPropertyValue(PROJECT_TARGET_DIR, props, "$baseDir/target")))
        }

        if (!projectWarFileSet) {
            def version = metadata.getApplicationVersion()
            def appName = metadata.getApplicationName() ?: baseDir.name
            def warName = version ? "$baseDir/target/${appName}-${version}.war" : "$baseDir/target/${appName}.war"

            projectWarFile = makeAbsolute(new File(getPropertyValue(PROJECT_WAR_FILE, props, warName)))
        }

        if (!autodeployDirSet) {
            autodeployDir = makeAbsolute(new File(getPropertyValue(PROJECT_AUTODEPLOY_DIR, props, "$baseDir/src/autodeploy")))
        }

        if (!projectWarExplodedDirSet) {
            projectWarExplodedDir = new File(getPropertyValue(PROJECT_WAR_EXPLODED_DIR, props, "${projectWorkDir}/stage"))
        }

        if (!pluginStagingDirSet) {
            pluginStagingDir = new File(getPropertyValue(PLUGIN_STAGING_DIR, props, "${projectWorkDir}/plugin_stage"))
        }

        if (!pluginIncludeSourceSet) {
            pluginIncludeSource = getPropertyValue(PLUGIN_INCLUDE_SOURCE, props, 'true').toBoolean()
        }

        if (!convertClosuresArtefactsSet) {
            convertClosuresArtefacts = getPropertyValue(CONVERT_CLOSURES_KEY, props, 'false').toBoolean()
        }

        if (!logScriptTimingSet) {
            logScriptTiming = getPropertyValue(LOG_SCRIPT_TIMING_KEY, props, 'false').toBoolean()
        }

        if (!projectWarOsgiHeadersSet) {
            projectWarOsgiHeaders = getPropertyValue(PROJECT_WAR_OSGI_HEADERS, props, 'false').toBoolean()
        }

        if (!classesDirSet) {
            classesDir = makeAbsolute(new File(getPropertyValue(PROJECT_CLASSES_DIR, props, "$projectWorkDir/classes")))
        }

        if (!testClassesDirSet) {
            testClassesDir = makeAbsolute(new File(getPropertyValue(PROJECT_TEST_CLASSES_DIR, props, "$projectWorkDir/test-classes")))
        }

        if (!pluginClassesDirSet) {
            pluginClassesDir = makeAbsolute(new File(getPropertyValue(PROJECT_PLUGIN_CLASSES_DIR, props, "$projectWorkDir/plugin-classes")))
        }

        if (!pluginBuildClassesDirSet) {
            pluginBuildClassesDir = makeAbsolute(new File(getPropertyValue(PROJECT_PLUGIN_BUILD_CLASSES_DIR, props, "$projectWorkDir/plugin-build-classes")))
        }

        if (!pluginProvidedClassesDirSet) {
            pluginProvidedClassesDir = makeAbsolute(new File(getPropertyValue(PROJECT_PLUGIN_PROVIDED_CLASSES_DIR, props, "$projectWorkDir/plugin-provided-classes")))
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
            testReportsDir = makeAbsolute(new File(getPropertyValue(PROJECT_TEST_REPORTS_DIR, props, "${projectTargetDir}/test-reports")))
        }

        if (!docsOutputDirSet) {
            docsOutputDir = makeAbsolute(new File(getPropertyValue(PROJECT_DOCS_OUTPUT_DIR, props, "${projectTargetDir}/docs")))
        }

        if (!testSourceDirSet) {
            testSourceDir = new File(getPropertyValue(PROJECT_TEST_SOURCE_DIR, props, "${baseDir}/test"))
        }

        if (!verboseCompileSet) {
            verboseCompile = getPropertyValue(VERBOSE_COMPILE, props, '').toBoolean()
        }
    }

    private getForkConfig() {
        def result = config.grails.project.fork
        String syspropDebugArgs = System.getProperty("grails.project.fork.run.debugArgs")
        boolean debugFork = Boolean.getBoolean(ForkedGrailsProcess.DEBUG_FORK)
        if (syspropDebugArgs || debugFork) {
            if (!(result.run instanceof Map)) {
                result.run = [:]
            }
            result.run.debug = true
            if (syspropDebugArgs != "true") {
                result.run.debugArgs = syspropDebugArgs
            }
        }
        syspropDebugArgs = System.getProperty("grails.project.fork.test.debugArgs")
        if (syspropDebugArgs || debugFork) {
            if (!(result.test instanceof Map)) {
                result.test = [:]
            }
            result.test.debug = true
            if (syspropDebugArgs != "true") {
                result.test.debugArgs = syspropDebugArgs
            }
        }
        return result
    }

    protected File makeAbsolute(File dir) {
        if (!dir.absolute) {
            dir = new File(baseDir, dir.path)
        }
        dir
    }

    protected void parseGrailsBuildListeners() {
        if (buildListenersSet) {
            return
        }

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

    @CompileStatic
    private String getPropertyValue(String propertyName, Properties props, String defaultValue) {
        // First check whether we have a system property with the given name.
        def value = getValueFromSystemOrBuild(propertyName, props)

        // Return the BuildSettings value if there is one, otherwise
        // use the default.
        return value != null ? value : defaultValue
    }

    @CompileStatic
    private getValueFromSystemOrBuild(String propertyName, Properties props) {
        def value = System.getProperty(propertyName)
        if (value != null) return value

        // Now try the BuildSettings config.
        value = props[propertyName]
        return value
    }

    @CompileStatic
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

    File getBasePluginDescriptor() {
        File basePluginFile = baseDir?.listFiles()?.find { it.name.endsWith("GrailsPlugin.groovy")}

        if (basePluginFile?.exists()) {
            return basePluginFile
        }
        return null
    }

    boolean isPluginProject() {
        getBasePluginDescriptor() != null
    }

    /**
     * @return The path to the Config.groovy file (not to be confused with BuildConfig.groovy)
     */
    @CompileStatic
    String getConfigFilePath() {
        configFile.absolutePath
    }

    @CompileStatic
    File getConfigFile() {
        new File(baseDir, "grails-app/conf/Config.groovy")
    }

    @CompileStatic
    void initializeResourcesDir() {
        def targetPath = resourcesDir.path
        def dir = new File(baseDir, "grails-app/conf")
        resourcesDir.mkdirs()
        if (dir.exists()) {
            try {
                copyDirectoryContentsToTarget(dir, targetPath)
            } catch (e) {
                GrailsConsole.getInstance().error("Error initializing resources from grails-app/conf: ${e.message}", e)
            }
        }

    }

    @CompileStatic
    protected void copyDirectoryContentsToTarget(File dir, String targetPath) {
        dir.eachFile { File f ->
            final isFile = !f.isDirectory()
            final isHidden = f.isHidden()
            if (isFile && !isHidden && !f.name.endsWith('.groovy')) {
                IOUtils.copy(f, new File(targetPath, f.name))
            }
            else if (!isFile && !isHidden) {
                final fileName = f.name
                if (fileName != 'hibernate' && fileName != 'spring') {
                    final newDir = new File(targetPath, fileName)
                    newDir.mkdirs()
                    copyDirectoryContentsToTarget(f, newDir.path)
                }
            }
        }
    }
}
