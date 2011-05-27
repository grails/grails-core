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

import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.util.PluginBuildSettings
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder

/**
 *
 * Encapsulates the compilation logic required for a Grails application
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class GrailsProjectCompiler {

    public static final List<String> EXCLUDED_PATHS =  ["views", "i18n", "conf"]
    private static final String CLASSPATH_REF = "grails.compile.classpath"
    private static final List<String> PLUGIN_EXCLUDE_PATHS = ["views", "i18n"] // conf gets special handling

    private AntBuilder ant
    private File pluginDescriptor
    private CompilerConfiguration config
    private ClassLoader classLoader
    boolean verbose = false

    boolean isPluginProject = false
    List<String> srcDirectories
    Map javaOptions = [classpathref:CLASSPATH_REF, encoding:"UTF-8", debug:"yes"]
    String basedir
    String srcdir
    String encoding = "UTF-8"
    File targetClassesDir
    File targetPluginClassesDir

    def commonClasspath
    def compileClasspath
    def testClasspath
    def runtimeClasspath

    BuildSettings buildSettings
    PluginBuildSettings pluginSettings
    List<String> compilerExtensions = ['groovy', 'java']

    /**
     * Constructs a new GrailsProjectCompiler instance for the given PluginBuildSettings and optional classloader
     *
     * @param pluginBuildSettings The PluginBuildSettings
     * @param rootLoader The ClassLoader

     */
    GrailsProjectCompiler(PluginBuildSettings pluginBuildSettings, ClassLoader rootLoader = Thread.currentThread().getContextClassLoader()) {
        pluginSettings = pluginBuildSettings
        buildSettings = pluginBuildSettings.buildSettings
        this.targetClassesDir = buildSettings.classesDir
        this.targetPluginClassesDir = buildSettings.pluginClassesDir
        this.basedir = buildSettings.baseDir.absolutePath
        this.srcdir = buildSettings.sourceDir.absolutePath
        this.classLoader = rootLoader
        this.pluginDescriptor = new File(basedir).listFiles().find { it.name.endsWith("GrailsPlugin.groovy") }
        this.config = config
        isPluginProject = pluginDescriptor != null
        srcDirectories = [ "${basedir}/grails-app/conf".toString(),
                           "${basedir}/grails-app/conf/spring".toString(),
                           "${srcdir}/groovy".toString(),
                           "${srcdir}/java".toString()]

        def excludedPaths =  EXCLUDED_PATHS

        final grailsAppDirs = new File("${basedir}/grails-app").listFiles()
        if (grailsAppDirs != null) {
            for (dir in grailsAppDirs) {
                if (dir != null) {
                    if (!excludedPaths?.contains(dir.name) && dir.isDirectory()) {
                        srcDirectories << "${dir}".toString()
                    }
                }
            }
        }

        initializeAntClasspaths()

        Resource[] resources = pluginBuildSettings.getArtefactResources()
        GrailsResourceLoader resourceLoader = new GrailsResourceLoader(resources)
        GrailsResourceLoaderHolder.setResourceLoader(resourceLoader)
    }

    AntBuilder getAnt() {
        if (ant == null) {
           ant = new AntBuilder()
           ant.taskdef (name: 'groovyc', classname : 'org.codehaus.groovy.grails.compiler.Grailsc')
           ant.path(id: "grails.compile.classpath", compileClasspath)
        }
        return ant
    }

    void setAnt(AntBuilder ant) {
        this.ant = ant
    }

    /**
     * Configures the Grails classpath, should be called prior to any call to {@link #compile(Object) }
     */
    void configureClasspath() {

        ant.path(id: "grails.compile.classpath", compileClasspath)
        ant.path(id: "grails.test.classpath", testClasspath)
        ant.path(id: "grails.runtime.classpath", runtimeClasspath)

        def grailsDir = new File("${basedir}/grails-app").listFiles()
        StringBuffer cpath = new StringBuffer("")

        def jarFiles = getJarFiles()

        for (dir in grailsDir) {
            cpath << dir.absolutePath << File.pathSeparator
            // Adding the grails-app folders to the root loader causes re-load issues as
            // root loader returns old class before the grails GCL attempts to recompile it
            // rootLoader?.addURL(dir.URL)
        }
        cpath << targetClassesDir.absolutePath<< File.pathSeparator
        cpath << targetPluginClassesDir.absolutePath << File.pathSeparator

        cpath << "${basedir}/web-app/WEB-INF" << File.pathSeparator
        for (File jar in jarFiles) {
            cpath << jar.absolutePath << File.pathSeparator
        }

        // We need to set up this configuration so that we can compile the
        // plugin descriptors, which lurk in the root of the plugin's project directory.
        config = new CompilerConfiguration()
        config.setClasspath(cpath.toString())
        config.sourceEncoding = "UTF-8"

        // The resources directory must be created before it is added to
        // the root loader, otherwise it is quietly ignored. In other words,
        // if the directory is created after its path has been added to the
        // root loader, it will not be included in the classpath.
        def resourcesDir = new File(buildSettings.resourcesDir.path)
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs()
        }
        classLoader?.addURL(resourcesDir.toURI().toURL())
    }

    /**
     * Obtains all JAR files for the project that aren't declared via BuildConfig
     *
     * @return A list of JAR files
     */
    List<File> getJarFiles() {
        final libDirPath = "${basedir}/lib"
        def jarFiles = listJarFiles(libDirPath)
        def pluginJars = pluginSettings.pluginJarFiles

        for (pluginJar in pluginJars) {
            boolean matches = jarFiles.any {it.name == pluginJar.file.name}
            if (!matches) jarFiles.add(pluginJar.file)
        }

        def userJars = listJarFiles("${buildSettings.userHome}/.grails/lib")
        for (userJar in userJars) {
            jarFiles.add(userJar)
        }

        jarFiles.addAll(getExtraDependencies())

        jarFiles
    }

    private List<File> listJarFiles(String libDirPath) {
        return new File(libDirPath).listFiles({ File f -> f.name.endsWith(".jar")} as FileFilter)?.toList() ?: []
    }

    /**
     * Extra dependencies defined by the 'grails.compiler.dependencies' config option in BuildConfig
     *
     * @return
     */
    List<File> getExtraDependencies() {
        def jarFiles =[]
        final buildConfig = buildSettings.config
        if (buildConfig?.grails?.compiler?.dependencies) {
            def extraDeps = ant.fileScanner(buildConfig.grails.compiler.dependencies)
            for (jar in extraDeps) {
                jarFiles << jar
            }
        }
        jarFiles
    }

    /**
     * Compiles plugin and normal sources
     */
    void compileAll() {
        compilePlugins()
        compile()
    }

    /**
     * Compiles project sources using the target directory passed by PluginBuildSettings
     *
     */
    void compile() {
        compile(targetClassesDir)
    }

    /**
     * Compiles project sources to the given target directory
     *
     * @param targetDir The target directory to compile to
     */
    void compile(targetDir) {

        def compilerPaths = { String classpathId ->
            for (srcPath in srcDirectories) {
               if (new File(srcPath).exists()) {
                   src(path:srcPath)
               }
            }
            javac(javaOptions)
        }

        def classesDirPath = new File(targetDir.toString())
        ant.mkdir(dir:classesDirPath)
        String classpathId = "grails.compile.classpath"
        ant.groovyc(destdir:classesDirPath,
                    classpathref:classpathId,
                    encoding:encoding,
                    verbose: verbose,
                    listfiles: verbose,
                    compilerPaths.curry(classpathId))

        // If this is a plugin project, the descriptor is not included
        // in the compiler's source path. So, we manually compile it now.
        if (isPluginProject) {
            compilePluginDescriptor(pluginDescriptor, classesDirPath)
        }
    }

    void compilePlugins() {
        compilePlugins(targetPluginClassesDir)
    }

    /**
     * Compiles plugin sources files to the given target directory
     *
     * @param targetDir The target directory to compile to
     */
    void compilePlugins(targetDir) {
        def classesDirPath = targetDir
        ant.mkdir(dir:classesDirPath)

        // First compile the plugins so that we can exclude any
        // classes that might conflict with the project's.
        def classpathId = CLASSPATH_REF

        def excludedPaths = PLUGIN_EXCLUDE_PATHS // conf gets special handling
        def pluginResources = pluginSettings.pluginSourceFiles?.findAll {
            !excludedPaths.contains(it.file.name) && it.file.isDirectory()
        }

        if (pluginResources) {
            // Only perform the compilation if there are some plugins
            // installed or otherwise referenced.
            ant.groovyc(destdir:classesDirPath,
                        classpathref:classpathId,
                        encoding:encoding,
                        verbose: verbose,
                        listfiles: verbose) {
                for (dir in pluginResources.file) {
                    src(path:"${dir}")
                }
                exclude(name: "**/BootStrap.groovy")
                exclude(name: "**/BuildConfig.groovy")
                exclude(name: "**/Config.groovy")
                exclude(name: "**/*DataSource.groovy")
                exclude(name: "**/UrlMappings.groovy")
                exclude(name: "**/resources.groovy")
                javac(javaOptions)
            }
        }
    }

    /**
     * Compiles GSP pages for the given application name to the (optional) target directory
     *
     * @param grailsAppName The app name
     * @param classesDir The optional classes dir, defaults to one provided by PluginBuildSettings in constructor
     */
    void compileGroovyPages(String grailsAppName, classesDir = targetClassesDir) {
        ant.taskdef (name: 'gspc', classname : 'org.codehaus.groovy.grails.web.pages.GroovyPageCompilerTask')
        // compile gsps in grails-app/views directory
        File gspTmpDir = new File(buildSettings.projectWorkDir, "gspcompile")
        ant.gspc(destdir:classesDir,
                 srcdir:"${basedir}/grails-app/views",
                 packagename:GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(grailsAppName),
                 serverpath:"/WEB-INF/grails-app/views/",
                 classpathref:"grails.compile.classpath",
                 tmpdir:gspTmpDir)

        // compile gsps in web-app directory
        ant.gspc(destdir:classesDir,
                 srcdir:"${basedir}/web-app",
                 packagename:"${GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(grailsAppName)}_webapp",
                 serverpath:"/",
                 classpathref:"grails.compile.classpath",
                 tmpdir:gspTmpDir)

        // compile views in plugins
        def pluginInfos = pluginSettings.supportedPluginInfos
        if (pluginInfos) {
            for (GrailsPluginInfo info in pluginInfos) {
                File pluginViews = new File(info.pluginDir.file, "grails-app/views")
                if (pluginViews.exists()) {
                    def viewPrefix="/WEB-INF/plugins/${info.name}-${info.version}/grails-app/views/"
                    ant.gspc(destdir:classesDir,
                             srcdir:pluginViews,
                             packagename:GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(info.name),
                             serverpath:viewPrefix,
                             classpathref:"grails.compile.classpath",
                             tmpdir:gspTmpDir)
                }
            }
        }
    }

    /**
     * Compiles a given plugin descriptor file - *GrailsPlugin.groovy.
     */
    protected compilePluginDescriptor(File descriptor, File classesDir) {
        def className = descriptor.name - '.groovy'
        def classFile = new File(classesDir, "${className}.class")

        if (descriptor.lastModified() > classFile.lastModified()) {
            ant.echo(message: "Compiling plugin descriptor...")
            config.setTargetDirectory(classesDir)
            def cl = new URLClassLoader([classesDir,targetPluginClassesDir]*.toURL() as URL[], classLoader)

            def unit = new CompilationUnit(config, null, new GroovyClassLoader(cl))
            unit.addSource(descriptor)
            unit.compile()
        }
    }

    private initializeAntClasspaths() {

        commonClasspath = {
            def grailsDir = new File("${basedir}/grails-app").listFiles()
            for (File file in grailsDir) {
                pathelement(location: "${file.absolutePath}")
            }

            def pluginLibDirs = pluginSettings.pluginLibDirectories.findAll { it.exists() }
            for (pluginLib in pluginLibDirs) {
                fileset(dir: pluginLib.file.absolutePath)
            }
        }

        compileClasspath = {
            commonClasspath.delegate = delegate
            commonClasspath.call()

            def dependencies = buildSettings.compileDependencies
            if (dependencies) {
                for (File f in dependencies) {
                    if (f) {
                        pathelement(location: f.absolutePath)
                    }
                }
            }
            pathelement(location: "${targetPluginClassesDir.absolutePath}")
        }

        testClasspath = {
            commonClasspath.delegate = delegate
            commonClasspath.call()

            def dependencies = buildSettings.testDependencies
            if (dependencies) {

                for (File f in dependencies) {
                    if (f) {
                        pathelement(location: f.absolutePath)
                    }
                }
            }

            pathelement(location: "${targetClassesDir.absolutePath}")
            pathelement(location: "${targetPluginClassesDir.absolutePath}")
        }

        runtimeClasspath = {
            commonClasspath.delegate = delegate
            commonClasspath.call()

            def dependencies = buildSettings.runtimeDependencies
            if (dependencies) {
                for (File f in dependencies) {
                    if (f) {
                        pathelement(location: f.absolutePath)
                    }
                }
            }

            pathelement(location: "${targetPluginClassesDir.absolutePath}")
            pathelement(location: "${targetClassesDir.absolutePath}")
        }
    }
}
