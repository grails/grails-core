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
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleErrorPrintStream
import org.codehaus.groovy.grails.cli.logging.GrailsConsolePrintStream

import java.lang.reflect.Method

import org.apache.commons.logging.Log
import org.codehaus.groovy.grails.cli.interactive.InteractiveMode
import org.codehaus.groovy.grails.cli.support.PluginPathDiscoverySupport

/**
 * Helper class for kicking off forked JVM processes, helpful in managing the setup and
 * execution of the forked process. Subclasses should provided a static void main method.
 *
 * @author Graeme Rocher
 * @since 2.2
 */
abstract class ForkedGrailsProcess {

    public static final String DEBUG_FORK = "grails.debug.fork"
    public static final int DEFAULT_DAEMON_PORT = 8091
    public static final String DEFAULT_DEBUG_ARGS = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

    int maxMemory = 1024
    int minMemory = 64
    int maxPerm = 256
    boolean debug = false
    String debugArgs = DEFAULT_DEBUG_ARGS
    boolean reloading = true
    boolean forkReserve
    boolean daemon
    int daemonPort = DEFAULT_DAEMON_PORT
    File reloadingAgent
    List<String> jvmArgs
    URLClassLoader forkedClassLoader
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

        executionContext.daemonPort = this.daemonPort
    }


    @CompileStatic
    void startDaemon(Closure callable) {

        if (!isDaemonRunning()) {
            def serverSocket = new ServerSocket(daemonPort)
            try {

                Thread.start {
                    killAfterTimeout()
                }
                final currentOut = System.out
                final currentErr = System.err
                final grailsConsole = GrailsConsole.instance
                final consoleOut = grailsConsole.out
                final consoleErr = grailsConsole.err


                while(true) {
                    final clientSocket = serverSocket.accept()
                    try {
                        try {
                            clientSocket.withStreams { InputStream sockIn, OutputStream sockOut ->
                                final outStream = new GrailsConsolePrintStream(sockOut)
                                final errStream = new GrailsConsoleErrorPrintStream(sockOut)
                                System.out = outStream
                                System.err = errStream
                                grailsConsole.out = new PrintStream(sockOut)
                                grailsConsole.err = new PrintStream(sockOut)


                                final contextFile = readLine(sockIn)
                                if (contextFile) {
                                    if ("exit" == contextFile) {
                                        System.exit(0)
                                    }
                                    else {
                                        def loadedContext = readExecutionContext(contextFile)
                                        if (loadedContext) {
                                            this.executionContext = loadedContext
                                        } else {
                                            // Forked daemon is regarded as command when contextFile cannot be loaded.
                                            executionContext.argsMap["params"] = contextFile.split(/\s/)
                                        }

                                        callable.call(clientSocket)
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            GrailsConsole.instance.error("Error executing daemon: ${e.message}")
                        }
                    }
                    finally {
                        clientSocket.close()
                        System.out = currentOut
                        System.err = currentErr
                        grailsConsole.out = consoleOut
                        grailsConsole.err = consoleErr

                    }
                }
            } catch (SocketException se) {
                // ignore
            }
        }

    }

    boolean isDaemonRunning() {
        try {
            def clientSocket = new Socket("localhost", daemonPort)
            clientSocket.withStreams { InputStream sockIn, OutputStream sockOut ->
                sockOut << '\n'
                sockOut.flush()
            }
            return true
        } catch (SocketException e) {
            return false
        }

    }

    @CompileStatic
    static String readLine(InputStream inputStream) {
        def out = new ByteArrayOutputStream()
        int ch
        while ((ch = inputStream.read()) != -1) {
            if (ch == '\n') {
                break
            }
            out.write(ch)
        }
        return out.toString().trim()
    }

    /**
     * @return Whether this process is a reserve process. A reserve process is an additional JVM, bootstrapped and idle that can resume execution at a later date
     */
    protected boolean isReserveProcess() {
        System.getProperty("grails.fork.reserve")!=null
    }

    /**
     * @return Whether this process is a reserve process. A reserve process is an additional JVM, bootstrapped and idle that can resume execution at a later date
     */
    protected boolean isDaemonProcess() {
        System.getProperty("grails.fork.daemon")!=null
    }

    @CompileStatic
    protected void discoverAndSetAgent(ExecutionContext executionContext) {
        final jarFromContext = executionContext.agentJar
        if (jarFromContext) {
            setReloadingAgent(jarFromContext)
        }
        else {
            try {
                final agentClass = Thread.currentThread().contextClassLoader.loadClass('org.springsource.loaded.ReloadEventProcessorPlugin')
                setReloadingAgent(findJarFile(agentClass))
            } catch (e) {
                final grailsHome = executionContext.grailsHome
                if (grailsHome && grailsHome.exists()) {
                    def agentHome = new File(grailsHome, "lib/org.springsource.springloaded/springloaded-core/jars")
                    final agentJar = agentHome.listFiles().find { File f -> f.name.endsWith(".jar") && !f.name.contains('sources') && !f.name.contains('javadoc')}
                    if (agentJar) {
                        setReloadingAgent(agentJar)
                    }
                }
            }
        }
    }

    @CompileStatic
    protected void waitForResume() {
        // wait for resume indicator
        def resumeDir = getResumeDir()
        resumeDir.mkdirs()
        startIdleKiller()
        while (resumeDir.exists()) {
            sleep(100)
        }
    }

    protected File getResumeDir() {
        new File(executionContext.projectWorkDir, resumeIndicatorName)
    }

    @CompileStatic
    void killAfterTimeout() {
        int idleTime = 1 * 60 // one hour

        try {
            Thread.sleep(idleTime * 60 * 1000) // convert minutes to ms
        } catch (e) {
            return
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

        final resumeDir = getResumeDir()
        if (isForkingReserveEnabled() && resumeDir.exists()) {
            resumeDir.delete()
            sleep(100)
            storeExecutionContext(executionContext)
            forkReserve(executionContext)
        }
        else {

            boolean connectedToDaemon = false
            if (shouldRunWithDaemon()) {
                try {
                    final contextFile = storeExecutionContext(executionContext)
                    final daemonCmd = contextFile.absolutePath
                    runDaemonCommand(daemonCmd)
                    connectedToDaemon = true
                } catch (SocketException e) {
                    connectedToDaemon = false
                }

            }
            if (!connectedToDaemon) {
                if (daemon && !connectedToDaemon) {
                    GrailsConsole.instance.updateStatus("Running without daemon...")
                }
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
                else if(shouldRunWithDaemon()) {
                    GrailsConsole.instance.updateStatus("Starting daemon...")
                    forkDaemon(executionContext)
                }

                return attachOutputListener(process)
            }
            else {
                return null
            }

        }
    }

    @CompileStatic
    protected void runDaemonCommand(String daemonCmd) {
        def clientSocket = new Socket("localhost", daemonPort)
        clientSocket.withStreams { InputStream sockIn, OutputStream sockOut ->

            sockOut << daemonCmd << '\n'
            sockOut.flush()

            new TextDumper(sockIn).run()
        }
    }

    @CompileStatic
    protected boolean shouldRunWithDaemon() {
        daemon && InteractiveMode.active && !isDebugForkEnabled()
    }

    @CompileStatic
    protected boolean isDebugForkEnabled() {
        debug || Boolean.getBoolean(DEBUG_FORK)
    }

    @CompileStatic
    void forkReserve(ExecutionContext executionContext = getExecutionContext()) {
        if (reloading) {
            discoverAndSetAgent(executionContext)
        }

        String classpathString = getBoostrapClasspath(executionContext)
        List<String> cmd = buildProcessCommand(executionContext, classpathString, true)

        forkReserveProcess(cmd, executionContext)
    }

    @CompileStatic
    void forkDaemon(ExecutionContext executionContext = getExecutionContext()) {
        if (reloading) {
            discoverAndSetAgent(executionContext)
        }

        executionContext.daemonPort = daemonPort
        String classpathString = getBoostrapClasspath(executionContext)
        List<String> cmd = buildProcessCommand(executionContext, classpathString, false, true)

        forkReserveProcess(cmd, executionContext)
    }

    @CompileStatic
    void restartDaemon(ExecutionContext executionContext = getExecutionContext()) {
        if (reloading) {
            discoverAndSetAgent(executionContext)
        }

        final console = GrailsConsole.instance
        console.updateStatus("Stopping daemon...")
        while(isDaemonRunning()) {
            runDaemonCommand("exit")
        }
        console.updateStatus("Starting daemon...")
        forkDaemon(executionContext)
        while(!isDaemonRunning()) {
            console.indicateProgress()
        }
        console.updateStatus("Daemon Started")
    }

    @CompileStatic
    boolean isForkingReserveEnabled() {
        return forkReserve && InteractiveMode.isActive() && !isDebugForkEnabled() && !daemon
    }

    @CompileStatic
    protected void forkReserveProcess(List<String> cmd, ExecutionContext executionContext, boolean attachListener =true) {
        final builder = new ProcessBuilder()
            .directory(executionContext.getBaseDir())
            .redirectErrorStream(false)
            .command(cmd)

        Thread.start {

            sleep 2000
            final p2 = builder.start()

            if (attachListener) {
                attachOutputListener(p2)
            }
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

                GrailsConsole.instance.error("Forked Grails VM exited with error")
                if(!InteractiveMode.active) {
                    System.exit(1)
                }
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

        cp.toString()
    }

    @CompileStatic
    protected boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") != -1
    }

    @CompileStatic
    protected List<String> buildProcessCommand(ExecutionContext executionContext, String classpathString, boolean isReserve = false, boolean isDaemon = false) {
        File tempFile = storeExecutionContext(executionContext)
        final javaHomeEnv = System.getenv("JAVA_HOME")

        def javaCommand
        if (javaHomeEnv && !isWindows()) {
            javaCommand = new File(javaHomeEnv, "bin/java").canonicalPath
        }
        else {
            javaCommand = "java" // assume it is correctly configured using PATH
        }

        List<String> cmd = [javaCommand]

        if (jvmArgs) {
            cmd.addAll(jvmArgs)
        }

        cmd.addAll(["-Xmx${maxMemory}M".toString(), "-Xms${minMemory}M".toString(),
            "-XX:MaxPermSize=${maxPerm}m".toString(), "-Dgrails.fork.active=true",
            "-Dgrails.build.execution.context=${tempFile.canonicalPath}".toString(), "-cp", classpathString])

        if (isDebugForkEnabled() && !isReserve) {
            cmd.addAll(["-Xdebug", "-Xnoagent", "-Dgrails.full.stacktrace=true", "-Djava.compiler=NONE"])
            cmd << (debugArgs ?: DEFAULT_DEBUG_ARGS)
        }
        final console = GrailsConsole.getInstance()
        if (isReserve) {
            cmd.add "-Dgrails.fork.reserve=true"
        }
        else if (isDaemon) {
            cmd.add "-Dgrails.fork.daemon=true"
        }
        if (console.isVerbose()) {
            cmd.add("-Dgrails.verbose=true")
            cmd.add("-Dgrails.full.stacktrace=true")
        }
        if (console.isStacktrace()) {
            cmd.add("-Dgrails.show.stacktrace=true")
        }
        if (reloadingAgent != null) {
            cmd.addAll(["-javaagent:" + reloadingAgent.getCanonicalPath(), "-noverify", "-Dspringloaded.synchronize=true", "-Djdk.reflect.allowGetCallerClass=true"])
            def cacheDir=System.getenv("GRAILS_AGENT_CACHE_DIR") ?: BuildSettingsHolder.settings.grailsWorkDir.canonicalPath
            cmd.add("-Dspringloaded=profile=grails;cacheDir=${cacheDir}".toString())
        }

        cmd << getClass().name

        return cmd
    }

    protected File storeExecutionContext(ExecutionContext executionContext) {
        def baseName = executionContext.getBaseDir().canonicalFile.name
        if (baseName.length() < 3) {
            baseName+='ec'
        }
        File tempFile = File.createTempFile(baseName, "grails-execution-context")

        tempFile.deleteOnExit()
        tempFile.delete()

        tempFile.withOutputStream { OutputStream fos ->
            new ObjectOutputStream(fos).writeObject(executionContext)
        }
        tempFile
    }

    @CompileStatic
    ExecutionContext readExecutionContext() {
        String location = System.getProperty("grails.build.execution.context")

        return readExecutionContext(location)
    }

    @CompileStatic
    protected ExecutionContext readExecutionContext(String location) {
        if (location != null) {
            final file = new File(location)
            if (file.exists()) {
                return (ExecutionContext) file.withInputStream { InputStream fis ->
                    def ois = new ObjectInputStream(fis)
                    ExecutionContext executionContext = (ExecutionContext) ois.readObject()
                    executionContext.process = this
                    this.daemonPort = executionContext.daemonPort
                    return executionContext
                }
            }
        }
        return null
    }

    @CompileStatic
    protected List<File> buildMinimalIsolatedClasspath(BuildSettings buildSettings) {
        List<File> buildDependencies = []

        File groovyJar = buildSettings.compileDependencies.find { File f -> f.name.contains "groovy-all" }
        File toolsJar = findToolsJar()

        if (toolsJar?.exists()) {
            buildDependencies.add(toolsJar)
        }

        if (!groovyJar) {
            groovyJar = findJarFile(GroovySystem)
        }

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
                    fileName.matches(/^ant-.+$/) ||
                    fileName.contains('ant-junit') ||
                    fileName.contains('jline') ||
                    fileName.contains('jansi') ) {
                bootstrapJars.add(f)
            }
        }

        buildDependencies.addAll bootstrapJars
        buildDependencies
    }

    @CompileStatic
    protected File findToolsJar() {
        final javaHome = System.getenv("JAVA_HOME")
        File toolsJar = javaHome ? new File(javaHome, "lib/tools.jar") : null
        if (!toolsJar?.exists()) {
            try {
                final toolsClass = Thread.currentThread().getContextClassLoader().loadClass('sun.tools.native2ascii.Main')
                toolsJar = findJarFile(toolsClass)
            } catch (e) {
                // ignore
            }
        }
        toolsJar
    }

    @CompileStatic
    protected File findJarFile(Class targetClass) {
        def absolutePath = targetClass.getResource('/' + targetClass.name.replace(".", "/") + ".class").getPath()
        final jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"))
        new File(jarPath)
    }

    @CompileStatic
    Collection<File> findSystemClasspathJars(BuildSettings buildSettings) {
        return buildSettings.buildDependencies.findAll { File it -> it.name.contains("tomcat") } +
            buildSettings.providedDependencies.findAll { File it -> it.name.contains("tomcat") }
    }

    @CompileStatic
    protected GroovyClassLoader createClassLoader(BuildSettings buildSettings) {
        def classLoader = new GroovyClassLoader()

        for (File f in buildSettings.runtimeDependencies) {
            classLoader.addURL(f.toURI().toURL())
        }
        for (File f in buildSettings.providedDependencies) {
            classLoader.addURL(f.toURI().toURL())
        }
        classLoader.addURL(buildSettings.classesDir.toURI().toURL())
        classLoader.addURL(buildSettings.pluginClassesDir.toURI().toURL())
        classLoader.addURL(buildSettings.pluginBuildClassesDir.toURI().toURL())
        classLoader.addURL(buildSettings.pluginProvidedClassesDir.toURI().toURL())
        classLoader.addURL(buildSettings.resourcesDir.toURI().toURL())

        def pluginSupport = new PluginPathDiscoverySupport(buildSettings)

        for (File f in pluginSupport.listJarsInPluginLibs()) {
            classLoader.addURL(f.toURI().toURL())
        }

        return classLoader
    }

    /**
     *
     * @param classLoader
     * @param buildSettings
     */
    protected void setupReloading(URLClassLoader classLoader, BuildSettings buildSettings) {
        Thread.start {
            final holders = classLoader.loadClass("grails.util.Holders")
            while(!holders.getPluginManager()) {
                sleep(1000)
            }
            startProjectWatcher(classLoader, buildSettings)
        }

    }

    protected void startProjectWatcher(URLClassLoader classLoader, BuildSettings buildSettings) {
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
        URLClassLoader newClassLoader = createClassLoader(buildSettings)
        if (forkedClassLoader && forkedClassLoader.URLs.toList().containsAll(newClassLoader.URLs.toList())) {
            // If the existing class loader includes all URLs of new class loader, the existing one should be used.
            // Otherwise a level of nested class loaders would become so deep that slow test causes.
            return forkedClassLoader
        }
        forkedClassLoader = newClassLoader
        return newClassLoader
    }

    @CompileStatic
    protected BuildSettings initializeBuildSettings(ExecutionContext ec) {
        final sysProps = ec.systemProps
        for(entry in sysProps.entrySet()) {
            if (entry.value) {
                System.setProperty(entry.key, entry.value)
            }
        }
        def buildSettings = new BuildSettings(ec.grailsHome, ec.baseDir)
        buildSettings.setDependenciesExternallyConfigured(true)
        buildSettings.loadConfig()
        buildSettings.setRuntimeDependencies(ec.runtimeDependencies)
        buildSettings.setCompileDependencies(ec.runtimeDependencies)
        buildSettings.setTestDependencies(ec.testDependencies)
        buildSettings.setProvidedDependencies(ec.providedDependencies)
        buildSettings.setBuildDependencies(ec.buildDependencies)
        buildSettings.setForkSettings(ec.forkConfig)

        BuildSettingsHolder.settings = buildSettings
        configureFork(buildSettings)
        buildSettings
    }

    protected void configureFork(BuildSettings buildSettings) {
        final runConfig = buildSettings.forkSettings.run
        if (runConfig instanceof Map)
            configure(runConfig)
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
    File agentJar

    String env
    File grailsHome
    Map<String, String> systemProps = [:]
    Map forkConfig = [:]
    Map argsMap = new LinkedHashMap()
    int daemonPort = ForkedGrailsProcess.DEFAULT_DAEMON_PORT

    transient ForkedGrailsProcess process

    ExecutionContext() {
        // empty constructor for deserialization
    }

    ExecutionContext(ForkedGrailsProcess process) {
        this.process = process
        this.daemonPort = process.daemonPort
    }

    void initialize(BuildSettings settings) {
        List<File> isolatedBuildDependencies = buildMinimalIsolatedClasspath(settings)
        for( prop in System.properties.keySet() ) {
            String p = prop.toString()
            if(p.startsWith("grails.")) {
                final value = System.properties.get(prop)
                if (value)
                    systemProps[p] = value
            }
        }

        buildDependencies = isolatedBuildDependencies
        runtimeDependencies = new ArrayList<>(settings.runtimeDependencies)
        runtimeDependencies.addAll settings.pluginRuntimeDependencies
        runtimeDependencies.addAll settings.applicationJars
        providedDependencies = new ArrayList<>(settings.providedDependencies)
        providedDependencies.addAll settings.pluginProvidedDependencies
        testDependencies = new ArrayList<>(settings.testDependencies)
        testDependencies.addAll settings.pluginTestDependencies
        baseDir = settings.baseDir
        env = Environment.current.name
        grailsHome = settings.grailsHome
        classesDir = settings.classesDir
        grailsWorkDir = settings.grailsWorkDir
        projectWorkDir = settings.projectWorkDir
        projectPluginsDir = settings.projectPluginsDir
        testClassesDir = settings.testClassesDir
        final currentForkConfig = (Map<String, Object>) settings.getForkSettings()
        currentForkConfig.each { key, value ->
            def forkConf
            if(value instanceof Boolean) {
                forkConf = value
            }
            else if (value instanceof Map) {
                forkConf = [:] + (Map)value
            }
            forkConfig[key] = forkConf
        }

        final agentReport = settings.dependencyManager.resolveAgent()
        if(agentReport && agentReport.jarFiles) {
            agentJar = agentReport.jarFiles[0]
        }
    }

    @CompileStatic
    protected List<File> buildMinimalIsolatedClasspath(BuildSettings buildSettings) {
        return process.buildMinimalIsolatedClasspath(buildSettings)
    }
}
