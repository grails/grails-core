/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.cli.fork

import gant.Gant
import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.cli.interactive.InteractiveMode

import java.lang.reflect.Method

import org.apache.commons.logging.Log

/**
 * Helper class for kicking off forked JVM processes, helpful in managing the setup and
 * execution of the forked process. Subclasses should provided a static void main method.
 *
 * @author Graeme Rocher
 * @since 2.2
 */
abstract class ForkedGrailsProcess {

    int maxMemory = 1024
    int minMemory = 64
    int maxPerm = 256
    boolean debug = false
    boolean reloading = true
    boolean forkReserve
    File reloadingAgent
    List<String> jvmArgs
    ClassLoader forkedClassLoader
    ExecutionContext executionContext

    private String resumeIndicatorName

    ForkedGrailsProcess() {
        resumeIndicatorName = "${getClass().simpleName}-process-resume"
    }

    @CompileStatic
    void configure(Map forkConfig) {
        final Map<String, Object> runSettings = (Map<String, Object>) forkConfig
        runSettings.each { Map.Entry<String, Object> entry ->
            try {
                GroovyObject go = (GroovyObject) this
                if (go.hasProperty(entry.key))
                    go.setProperty(entry.getKey(),entry.getValue())
            } catch (MissingPropertyException e) {
                // ignore
            }
        }
    }

    /**
     * @return Whether this process is a reserve process. A reserve process is an additional JVM, bootstrapped and idle that can resume execution at a later date
     */
    static boolean isReserveProcess() {
        System.getProperty("grails.fork.reserve")!=null
    }

    @CompileStatic
    protected void discoverAndSetAgent(ExecutionContext executionContext) {
        try {
            final agentClass = Thread.currentThread().contextClassLoader.loadClass('org.springsource.loaded.ReloadEventProcessorPlugin')
            setReloadingAgent(findJarFile(agentClass))
        } catch (e) {
            final grailsHome = executionContext.grailsHome
            if (grailsHome && grailsHome.exists()) {
                def agentHome = new File(grailsHome, "lib/org.springsource.springloaded/springloaded-core/jars")
                final agentJar = agentHome.listFiles().find { File f -> f.name.endsWith(".jar")}
                if (agentJar) {
                    setReloadingAgent(agentJar)
                }
            }
        }
    }

    @CompileStatic
    protected void waitForResume() {
        // wait for resume indicator
        def resumeDir = new File(executionContext.projectWorkDir, resumeIndicatorName)
        resumeDir.mkdirs()
        startIdleKiller()
        while (resumeDir.exists()) {
            sleep(100)
        }
    }

    @CompileStatic
    void killAfterTimeout() {
        int idleTime = 1 * 60 // one hour

        try {
            Thread.sleep(idleTime * 60 * 1000) // convert minutes to ms
        } catch (e) {
            return;
        }

        def lockDir = new File(executionContext.projectWorkDir, "process-lock")
        if (lockDir.mkdir()) {
            System.exit 0
        } else {
            // someone is already connected; let the process finish
        }
    }

    @CompileStatic
    private void startIdleKiller() {
        def idleKiller = new Thread({
            killAfterTimeout()
        } as Runnable)

        idleKiller.daemon = true
        idleKiller.start()
    }


    @CompileStatic
    Process fork(Map argsMap = new LinkedHashMap()) {
        ExecutionContext executionContext = getExecutionContext()
        executionContext.argsMap = argsMap
        if (reloading) {
            discoverAndSetAgent(executionContext)
        }

        def resumeDir = new File(executionContext.projectWorkDir, resumeIndicatorName)
        if (isForkingReserveEnabled() && resumeDir.exists()) {
            resumeDir.delete()
            sleep(100)

            String classpathString = getBoostrapClasspath(executionContext)
            List<String> cmd = buildProcessCommand(executionContext, classpathString, true)

            forkReserveProcess(cmd, executionContext)
        }
        else {
            String classpathString = getBoostrapClasspath(executionContext)
            List<String> cmd = buildProcessCommand(executionContext, classpathString)

            def processBuilder = new ProcessBuilder()
            processBuilder
                .directory(executionContext.getBaseDir())
                .redirectErrorStream(false)
                .command(cmd)

            def process = processBuilder.start()

            if (isForkingReserveEnabled()) {
                List<String> reserveCmd = buildProcessCommand(executionContext, classpathString, true)
                forkReserveProcess(reserveCmd, executionContext)

            }

            return attachOutputListener(process)
        }

    }

    protected boolean isForkingReserveEnabled() {
        forkReserve && InteractiveMode.isActive() && !debug
    }

    @CompileStatic
    protected void forkReserveProcess(List<String> cmd, ExecutionContext executionContext) {
        final builder = new ProcessBuilder()
            .directory(executionContext.getBaseDir())
            .redirectErrorStream(false)
            .command(cmd)
        Thread.start {

            sleep 2000
            final p2 = builder.start()

            attachOutputListener(p2)

        }
    }

    @CompileStatic
    protected Process attachOutputListener(Process process, boolean async = false) {
        def is = process.inputStream
        def es = process.errorStream
        def t1 = new Thread(new TextDumper(is))
        def t2 = new Thread(new TextDumper(es))
        t1.start()
        t2.start()


        def callable = {
            int result = process.waitFor()
            if (result == 1) {
                try { t1.join() } catch (InterruptedException ignore) {}
                try { t2.join() } catch (InterruptedException ignore) {}
                try { es.close() } catch (IOException ignore) {}
                try { is.close() } catch (IOException ignore) {}

                throw new RuntimeException("Forked Grails VM exited with error")
            }

        }
        if (async) {
            Thread.start callable
        }
        else {
            callable.call()
        }
        return process
    }

    @CompileStatic
    protected String getBoostrapClasspath(ExecutionContext executionContext) {
        def cp = new StringBuilder()
        for (File file : executionContext.getBuildDependencies()) {
            cp << file << File.pathSeparator
        }

        final classpathString = cp.toString()
        classpathString
    }

    @CompileStatic
    protected List<String> buildProcessCommand(ExecutionContext executionContext, String classpathString, boolean isReserve = false) {
        def baseName = executionContext.getBaseDir().canonicalFile.name
        File tempFile = File.createTempFile(baseName, "grails-execution-context")

        tempFile.deleteOnExit()

        tempFile.withOutputStream { OutputStream fos ->
            def oos = new ObjectOutputStream(fos)
            oos.writeObject(executionContext)
        }

        List<String> cmd = ["java", "-Xmx${maxMemory}M".toString(), "-Xms${minMemory}M".toString(), "-XX:MaxPermSize=${maxPerm}m".toString(), "-Dgrails.fork.active=true", "-Dgrails.build.execution.context=${tempFile.canonicalPath}".toString(), "-cp", classpathString]
        if (debug && !isReserve) {
            cmd.addAll(["-Xdebug", "-Xnoagent", "-Dgrails.full.stacktrace=true", "-Djava.compiler=NONE", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"])
        }
        final console = GrailsConsole.getInstance()
        if (isReserve) {
            cmd.add "-Dgrails.fork.reserve=true"
        }
        if (console.isVerbose()) {
            cmd.add("-Dgrails.verbose=true")
            cmd.add("-Dgrails.full.stacktrace=true")
        }
        if (console.isStacktrace()) {
            cmd.add("-Dgrails.show.stacktrace=true")
        }
        if (reloadingAgent != null) {
            cmd.addAll(["-javaagent:" + reloadingAgent.getCanonicalPath(), "-noverify", "-Dspringloaded=profile=grails"])
        }
        cmd << getClass().name
        if (jvmArgs) {
            cmd.addAll(jvmArgs)
        }
        return cmd
    }

    @CompileStatic
    ExecutionContext readExecutionContext() {
        String location = System.getProperty("grails.build.execution.context")

        if (location != null) {
            final file = new File(location)
            if (file.exists()) {
                return (ExecutionContext)file.withInputStream { InputStream fis ->
                    def ois = new ObjectInputStream(fis)
                    return (ExecutionContext)ois.readObject()
                }
            }
        }
        return null
    }

    @CompileStatic
    public static List<File> buildMinimalIsolatedClasspath(BuildSettings buildSettings) {
        List<File> buildDependencies = []

        File groovyJar = buildSettings.compileDependencies.find { File f -> f.name.contains "groovy-all" }

        if (!groovyJar)
            groovyJar = findJarFile(GroovySystem)


        buildDependencies.add groovyJar
        buildDependencies.add findJarFile(Log)
        buildDependencies.add findJarFile(Gant)

        List<File> bootstrapJars = []
        for (File f in buildSettings.runtimeDependencies) {
            final fileName = f.name
            if (fileName.contains('log4j') ) {
                bootstrapJars.add(f)
            }
        }
        for (File f in buildSettings.buildDependencies) {
            final fileName = f.name
            if (fileName.contains('grails-bootstrap') ||
                    fileName.contains('slf4j-api') ||
                    fileName.contains('ivy') ||
                    fileName.contains('ant') ||
                    fileName.contains('jline') ||
                    fileName.contains('jansi') ) {
                bootstrapJars.add(f)
            }
        }

        buildDependencies.addAll bootstrapJars
        buildDependencies
    }

    @CompileStatic
    public static File findJarFile(Class targetClass) {
        def absolutePath = targetClass.getResource('/' + targetClass.name.replace(".", "/") + ".class").getPath()
        final jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"))
        new File(jarPath)
    }

    @CompileStatic
    public static Collection<File> findTomcatJars(BuildSettings buildSettings) {
        return buildSettings.buildDependencies.findAll { File it -> it.name.contains("tomcat") && !it.name.contains("grails-plugin-tomcat") } +
            buildSettings.providedDependencies.findAll { File it -> it.name.contains("tomcat") && !it.name.contains("grails-plugin-tomcat") }
    }

    @CompileStatic
    protected GroovyClassLoader createClassLoader(BuildSettings buildSettings) {
        def classLoader = new GroovyClassLoader()

        for(File f in buildSettings.runtimeDependencies) {
            classLoader.addURL(f.toURI().toURL())
        }
        for(File f in buildSettings.providedDependencies) {
            classLoader.addURL(f.toURI().toURL())
        }
        classLoader.addURL(buildSettings.classesDir.toURI().toURL())
        classLoader.addURL(buildSettings.pluginClassesDir.toURI().toURL())
        classLoader.addURL(buildSettings.pluginBuildClassesDir.toURI().toURL())
        classLoader.addURL(buildSettings.pluginProvidedClassesDir.toURI().toURL())


        return classLoader
    }

    protected void setupReloading(URLClassLoader classLoader, BuildSettings buildSettings) {
        try {
            final projectCompiler = classLoader.loadClass("org.codehaus.groovy.grails.compiler.GrailsProjectCompiler").newInstance(new PluginBuildSettings(buildSettings), classLoader)
            projectCompiler.configureClasspath()
            final holders = classLoader.loadClass("grails.util.Holders")
            final projectWatcher = classLoader.loadClass("org.codehaus.groovy.grails.compiler.GrailsProjectWatcher").newInstance(projectCompiler, holders.getPluginManager())
            projectWatcher.run()
        } catch (e) {
            e.printStackTrace()
            println "WARNING: There was an error setting up reloading. Changes to classes will not be reflected: ${e.message}"
        }
    }

    @CompileStatic
    protected URLClassLoader initializeClassLoader(BuildSettings buildSettings) {
        URLClassLoader classLoader = createClassLoader(buildSettings)
        forkedClassLoader = classLoader
        classLoader
    }

    @CompileStatic
    protected BuildSettings initializeBuildSettings(ExecutionContext ec) {
        def buildSettings = new BuildSettings(ec.grailsHome, ec.baseDir)
        buildSettings.loadConfig()

        BuildSettingsHolder.settings = buildSettings
        buildSettings
    }

    protected void initializeLogging(File grailsHome, ClassLoader classLoader) {
        try {
            Class<?> cls = classLoader.loadClass("org.apache.log4j.PropertyConfigurator")
            Method configure = cls.getMethod("configure", URL)
            configure.setAccessible(true)
            File f = new File(grailsHome.absolutePath + "/scripts/log4j.properties")
            configure.invoke(cls, f.toURI().toURL())
        } catch (Throwable e) {
            println("Log4j was not found on the classpath and will not be used for command line logging. Cause "+e.getClass().getName()+": " + e.getMessage())
        }
    }

    @CompileStatic
    static class TextDumper implements Runnable {
        InputStream input

        TextDumper(InputStream input) {
            this.input = input
        }

        void run() {
            def isr = new InputStreamReader(input)
            new BufferedReader(isr).eachLine { String next ->
                if (next) {
                    GrailsConsole.getInstance().log(next)
                }
            }
        }
    }
}

@CompileStatic
class ExecutionContext implements Serializable {

    private static final long serialVersionUID = 1

    List<File> runtimeDependencies
    List<File> buildDependencies
    List<File> providedDependencies
    List<File> testDependencies

    File grailsWorkDir
    File projectWorkDir
    File classesDir
    File testClassesDir
    File resourcesDir
    File projectPluginsDir
    File baseDir

    String env
    File grailsHome
    Map argsMap

    void initialize(BuildSettings settings) {
        List<File> isolatedBuildDependencies = buildMinimalIsolatedClasspath(settings)

        buildDependencies = isolatedBuildDependencies
        runtimeDependencies = settings.runtimeDependencies
        providedDependencies = settings.providedDependencies
        testDependencies = settings.testDependencies
        baseDir = settings.baseDir
        env = Environment.current.name
        grailsHome = settings.grailsHome
        classesDir = settings.classesDir
        grailsWorkDir = settings.grailsWorkDir
        projectWorkDir = settings.projectWorkDir
        projectPluginsDir = settings.projectPluginsDir
        testClassesDir = settings.testClassesDir
    }

    @CompileStatic
    protected List<File> buildMinimalIsolatedClasspath(BuildSettings buildSettings) {
        return ForkedGrailsProcess.buildMinimalIsolatedClasspath(buildSettings)
    }
}
