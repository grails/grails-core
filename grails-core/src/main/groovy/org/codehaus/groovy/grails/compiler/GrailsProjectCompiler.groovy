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

import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

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
    private List<String> srcDirectories
    private File pluginDescriptor
    private CompilerConfiguration config
    private ClassLoader classLoader

    boolean verbose = false
    boolean isPluginProject = false
    Map javaOptions = [classpathref:CLASSPATH_REF, encoding:"UTF-8", debug:"yes"]
    String basedir
    String srcdir
    String encoding = "UTF-8"
    Resource[] pluginSourceDirectories


    GrailsProjectCompiler(String basedir, String srcdir, Resource[] pluginSrcDirectories, CompilerConfiguration config, ClassLoader rootLoader = Thread.currentThread().getContextClassLoader()) {
        this.basedir = basedir
        this.srcdir = srcdir
        this.pluginSourceDirectories = pluginSrcDirectories
        this.classLoader = rootLoader
        this.pluginDescriptor = new File(basedir).listFiles().find { it.name.endsWith("GrailsPlugin.groovy") }
        this.config = config
        isPluginProject = pluginDescriptor != null
        srcDirectories = [ "${basedir}/grails-app/conf",
                           "${basedir}/grails-app/conf/spring",
                           "${srcdir}/groovy",
                           "${srcdir}/java"]

        def excludedPaths =  EXCLUDED_PATHS

        for (dir in new File("${basedir}/grails-app").listFiles()) {
            if (!excludedPaths.contains(dir.name) && dir.isDirectory()) {
                srcDirectories << "${dir}"
            }
        }
    }

    AntBuilder getAnt() {
        if(this.ant == null) ant = new AntBuilder()
        return ant
    }

    void setAnt(AntBuilder ant) {
        this.ant = ant
    }

    /**
     * Compiles project sources to the given target directory
     *
     * @param targetDir The target directory to compile to
     */
    void compile(targetDir) {

        def compilerPaths = { String classpathId ->
            for(srcPath in srcDirectories) {
               if(new File(srcPath).exists()) {
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
        if (isPluginProject) compilePluginDescriptor(pluginDescriptor, classesDirPath)
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
        def pluginResources = pluginSourceDirectories?.findAll {
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
     * Compiles a given plugin descriptor file - *GrailsPlugin.groovy.
     */
    protected compilePluginDescriptor(File descriptor, File classesDir) {
        def className = descriptor.name - '.groovy'
        def classFile = new File(classesDir, "${className}.class")

        if (descriptor.lastModified() > classFile.lastModified()) {
            ant.echo(message: "Compiling plugin descriptor...")
            config.setTargetDirectory(classesDir)
            def unit = new CompilationUnit(config, null, new GroovyClassLoader(classLoader))
            unit.addSource(descriptor)
            unit.compile()
        }
    }

    /**
     * Starts a daemon thread that recompiles sources when they change
     */
    DirectoryWatcher startCompilerDaemon(classesDir, pluginClassesDir) {
        def allDirectories = []
        allDirectories.addAll(srcDirectories.collect { new File(it) })
        allDirectories.addAll( pluginSourceDirectories.findAll { Resource r -> r.file.isDirectory() }.collect { it.file } )
        DirectoryWatcher watcher = new DirectoryWatcher(allDirectories as File[], ['.groovy', '.java'] as String[] )
        watcher.addListener(new DirectoryWatcher.FileChangeListener() {
            void onChange(File file) {
                try {
                    compilePlugins(pluginClassesDir)
                    compile(classesDir)
                }
                catch(e) {
                    println e.message
                }
            }

            void onNew(File file) {
                try {
                    sleep(5000) // sleep for a little while to wait for the final to become valid
                    compilePlugins(pluginClassesDir)
                    compile(classesDir)
                }
                catch(e) {
                    println e.message
                }
            }

        })
        watcher.start()
        return watcher
    }
}
