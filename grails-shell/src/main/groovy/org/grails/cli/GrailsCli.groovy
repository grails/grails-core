/*
 * Copyright 2014 the original author or authors.
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
package org.grails.cli

import grails.build.logging.GrailsConsole
import grails.build.proxy.SystemPropertiesAuthenticator
import grails.config.ConfigMap
import grails.io.support.SystemStreamsRedirector
import grails.util.BuildSettings
import grails.util.Environment
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jline.UnixTerminal
import jline.console.UserInterruptException
import jline.console.completer.ArgumentCompleter
import jline.internal.NonBlockingInputStream
import org.gradle.tooling.BuildActionExecuter
import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.ProjectConnection
import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser
import org.grails.build.parsing.DefaultCommandLine
import org.grails.cli.gradle.ClasspathBuildAction
import org.grails.cli.gradle.GradleAsyncInvoker
import org.grails.cli.gradle.GradleUtil
import org.grails.cli.gradle.cache.MapReadingCachedGradleOperation
import org.grails.cli.interactive.completers.EscapingFileNameCompletor
import org.grails.cli.interactive.completers.RegexCompletor
import org.grails.cli.interactive.completers.SortedAggregateCompleter
import org.grails.cli.profile.*
import org.grails.cli.profile.commands.CommandRegistry
import org.grails.cli.profile.repository.MavenProfileRepository
import org.grails.cli.profile.repository.StaticJarProfileRepository
import org.grails.config.CodeGenConfig
import org.grails.config.NavigableMap
import org.grails.exceptions.ExceptionUtils
import org.grails.gradle.plugin.model.GrailsClasspath
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration

import java.util.concurrent.*

/**
 * Main class for the Grails command line. Handles interactive mode and running Grails commands within the context of a profile
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 *
 * @since 3.0
 */
@CompileStatic
class GrailsCli {
    static final String ARG_SPLIT_PATTERN = /(?<!\\)\s+/
    public static final String DEFAULT_PROFILE_NAME = ProfileRepository.DEFAULT_PROFILE_NAME
    private static final int KEYPRESS_CTRL_C = 3
    private static final int KEYPRESS_ESC = 27
    private static final String USAGE_MESSAGE = "create-app [NAME] --profile=web"
    private static final String PLUGIN_USAGE_MESSAGE = "create-plugin [NAME] --profile=web-plugin"
    private final SystemStreamsRedirector originalStreams = SystemStreamsRedirector.original() // store original System.in, System.out and System.err
    private static ExecutionContext currentExecutionContext = null

    private static boolean interactiveModeActive
    private static final NavigableMap SETTINGS_MAP = new NavigableMap()

    static {
        if(BuildSettings.SETTINGS_FILE.exists()) {
            try {
                SETTINGS_MAP.merge new ConfigSlurper().parse(BuildSettings.SETTINGS_FILE.toURI().toURL())
            } catch (Throwable e) {
                e.printStackTrace()
                System.err.println("ERROR: Problem loading $BuildSettings.SETTINGS_FILE: ${e.message}")
            }

            try {
                Runtime.addShutdownHook {
                    currentExecutionContext?.cancel()
                }
            } catch (e) {
                // ignore
            }
        }
    }





    SortedAggregateCompleter aggregateCompleter=new SortedAggregateCompleter()
    CommandLineParser cliParser = new CommandLineParser()
    boolean keepRunning = true
    Boolean ansiEnabled = null
    boolean integrateGradle = true
    Character defaultInputMask = null
    ProfileRepository profileRepository
    CodeGenConfig applicationConfig
    ProjectContext projectContext
    Profile profile = null
    List<RepositoryConfiguration> profileRepositories = [MavenProfileRepository.DEFAULT_REPO]

    /**
     * Obtains a value from USER_HOME/.grails/settings.yml
     *
     * @param key the property name to resolve
     * @param targetType the expected type of the property value
     * @param defaultValue The default value
     */
    public static <T> T getSetting(String key, Class<T> targetType = Object.class, T defaultValue = null) {
        def value = SETTINGS_MAP.get(key, defaultValue)
        if(value == null) {
            return null
        }

        else if(targetType.isInstance(value)) {
            return (T)value
        }
        else {
            try {
                return value.asType(targetType)
            } catch (Throwable e) {
                return null
            }
        }
    }
    /**
     * Main method for running via the command line
     *
     * @param args The arguments
     */
    public static void main(String[] args) {

        Authenticator.setDefault( getSetting( BuildSettings.AUTHENTICATOR, Authenticator,  new SystemPropertiesAuthenticator() ) )
        def proxySelector = getSetting(BuildSettings.PROXY_SELECTOR, ProxySelector)
        if(proxySelector != null) {
            ProxySelector.setDefault( proxySelector )
        }

        GrailsCli cli=new GrailsCli()
        try {
            exit(cli.execute(args))
        }
        catch(BuildCancelledException e) {
            GrailsConsole.instance.addStatus("Build stopped.")
            exit(0)
        }
        catch (Throwable e) {
            e = ExceptionUtils.getRootCause(e)
            GrailsConsole.instance.error("Error occurred running Grails CLI: $e.message", e)
            exit(1)
        }
    }

    static void exit(int code) {
        GrailsConsole.instance.cleanlyExit(code)
    }

    static boolean isInteractiveModeActive() {
        return interactiveModeActive
    }

    private int getBaseUsage() {
        System.out.println "Usage: \n\t $USAGE_MESSAGE \n\t $PLUGIN_USAGE_MESSAGE \n\n"
        this.execute "list-profiles"
        System.out.println "\nType 'grails help' or 'grails -h' for more information."

        return 1
    }

    /**
     * Execute the given command
     *
     * @param args The arguments
     * @return The exit status code
     */
    public int execute(String... args) {
        CommandLine mainCommandLine=cliParser.parse(args)

        if(mainCommandLine.hasOption(CommandLine.VERBOSE_ARGUMENT)) {
            System.setProperty("grails.verbose", "true")
        }
        if(mainCommandLine.hasOption(CommandLine.STACKTRACE_ARGUMENT)) {
            System.setProperty("grails.show.stacktrace", "true")
        }

        if(mainCommandLine.hasOption(CommandLine.VERSION_ARGUMENT) || mainCommandLine.hasOption('v')) {
            def console = GrailsConsole.instance
            console.addStatus("Grails Version: ${GrailsCli.getPackage().implementationVersion}")
            console.addStatus("Groovy Version: ${GroovySystem.version}")
            console.addStatus("JVM Version: ${System.getProperty('java.version')}")
            exit(0)
        }



        if(mainCommandLine.hasOption(CommandLine.HELP_ARGUMENT) || mainCommandLine.hasOption('h')) {
            profileRepository = createMavenProfileRepository()
            def cmd = CommandRegistry.getCommand("help", profileRepository)
            cmd.handle(createExecutionContext(mainCommandLine))
            exit(0)
        }

        if(mainCommandLine.environmentSet) {
            System.setProperty(Environment.KEY, mainCommandLine.environment)
        }

        File grailsAppDir=new File("grails-app")
        File applicationGroovy =new File("Application.groovy")
        File profileYml =new File("profile.yml")
        if(!grailsAppDir.isDirectory() && !applicationGroovy.exists() && !profileYml.exists()) {
            profileRepository = createMavenProfileRepository()
            if(!mainCommandLine || !mainCommandLine.commandName) {
                return getBaseUsage()
            }
            def cmd = CommandRegistry.getCommand(mainCommandLine.commandName, profileRepository)
            if(cmd) {
                def arguments = cmd.description.arguments
                def requiredArgs = arguments.count { CommandArgument arg -> arg.required }
                if(mainCommandLine.remainingArgs.size() < requiredArgs) {
                    outputMissingArgumentsMessage cmd
                    return 1
                }
                else {
                    return cmd.handle(createExecutionContext( mainCommandLine )) ? 0 : 1
                }
            }
            else {
                return getBaseUsage()
            }

        } else {
            applicationConfig = loadApplicationConfig()
            if(profileYml.exists()) {
                // use the profile for profiles
                applicationConfig.put(BuildSettings.PROFILE, "profile")
            }
        
            def commandName = mainCommandLine.commandName
            GrailsConsole console = GrailsConsole.instance
            console.ansiEnabled = !mainCommandLine.hasOption(CommandLine.NOANSI_ARGUMENT)
            console.defaultInputMask = defaultInputMask
            if(ansiEnabled != null) {
                console.ansiEnabled = ansiEnabled
            }
            File baseDir = new File(".").canonicalFile
            projectContext = new ProjectContextImpl(console, baseDir, applicationConfig)
            initializeProfile()
            if(commandName) {
                return handleCommand(mainCommandLine) ? 0 : 1
            } else {
                handleInteractiveMode()
            }
        }
        return 0
    }

    protected MavenProfileRepository createMavenProfileRepository() {
        def profileRepos = getSetting(BuildSettings.PROFILE_REPOSITORIES, Map.class, Collections.emptyMap())
        if(!profileRepos.isEmpty()) {
            profileRepositories.clear()
            for (repoName in profileRepos.keySet()) {
                def data = profileRepos.get(repoName)
                if(data instanceof Map) {
                    def uri = data.get("url")
                    def snapshots = data.get('snapshotsEnabled')
                    if(uri != null) {
                        boolean enableSnapshots = snapshots != null ? Boolean.valueOf(snapshots.toString()) : false
                        profileRepositories.add( new RepositoryConfiguration(repoName.toString(), new URI(uri.toString()), enableSnapshots))
                    }
                }
            }
        }
        return new MavenProfileRepository(profileRepositories)
    }

    protected void outputMissingArgumentsMessage(Command cmd) {
        def console = GrailsConsole.instance
        console.error("Command $cmd.name is missing required arguments:")
        for (CommandArgument arg in cmd.description.arguments.findAll { CommandArgument ca -> ca.required }) {
            console.log("* $arg.name - $arg.description")
        }
    }

    ExecutionContext createExecutionContext(CommandLine commandLine) {
        new ExecutionContextImpl(commandLine, projectContext)
    }
    
    Boolean handleCommand( CommandLine commandLine ) {

        handleCommand(createExecutionContext(commandLine))
    }
    
    Boolean handleCommand( ExecutionContext context ) {
        synchronized(GrailsCli) {
            try {
                currentExecutionContext = context
                if(handleBuiltInCommands(context)) {
                    return true
                }

                def console = GrailsConsole.getInstance()
                if(context.getCommandLine().hasOption(CommandLine.STACKTRACE_ARGUMENT)) {
                    console.setStacktrace(true);
                }
                else {
                    console.setStacktrace(false);
                }
        
                if(profile.handleCommand(context)) {
                    return true;
                }
                return false
            } finally {
                currentExecutionContext = null
            }
        }
    }


    private void handleInteractiveMode() {
        System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")
        GrailsConsole console = projectContext.console
        console.reader.setHandleUserInterrupt(true)
        def completers = aggregateCompleter.getCompleters()

        // add bang operator completer
        completers.add(new ArgumentCompleter(
                new RegexCompletor("!\\w+"), new EscapingFileNameCompletor())
        )

        completers.addAll((profile.getCompleters(projectContext)?:[]) as Collection)
        console.reader.addCompleter(aggregateCompleter)

        console.updateStatus("Starting interactive mode...")
        ExecutorService commandExecutor = Executors.newFixedThreadPool(1)
        try {
            interactiveModeLoop(commandExecutor)
        } finally {
            commandExecutor.shutdownNow()
        }
    }

    private void interactiveModeLoop(ExecutorService commandExecutor) {
        GrailsConsole console = projectContext.console
        NonBlockingInputStream nonBlockingInput = (NonBlockingInputStream)console.reader.getInput()
        interactiveModeActive = true
        boolean firstRun = true
        while(keepRunning) {
            try {
                if(integrateGradle)
                    GradleUtil.prepareConnection(projectContext.baseDir)
                if(firstRun) {
                    console.addStatus("Enter a command name to run. Use TAB for completion:")
                    firstRun = false
                }
                String commandLine = console.showPrompt()
                if(commandLine==null) {
                    // CTRL-D was pressed, exit interactive mode
                    exitInteractiveMode()
                } else if (commandLine.trim()) {
                    if(nonBlockingInput.isNonBlockingEnabled()) {
                        handleCommandWithCancellationSupport(commandLine, commandExecutor, nonBlockingInput)
                    } else {
                        handleCommand( cliParser.parseString(commandLine))
                    }
                }
            } catch (BuildCancelledException cancelledException) {
                console.updateStatus("Build stopped.")
            }catch (UserInterruptException e) {
                exitInteractiveMode()
            } catch (Exception e) {
                console.error "Caught exception ${e.message}", e
            }
        }
    }

    private Boolean handleCommandWithCancellationSupport(String commandLine, ExecutorService commandExecutor, NonBlockingInputStream nonBlockingInput) {
        ExecutionContext executionContext = createExecutionContext( cliParser.parseString(commandLine))
        Future<?> commandFuture = commandExecutor.submit({ handleCommand(executionContext) } as Callable<Boolean>)
        def terminal = projectContext.console.reader.terminal
        if (terminal instanceof UnixTerminal) {
            ((UnixTerminal) terminal).disableInterruptCharacter()
        }
        try {
            while(!commandFuture.done) {
                if(nonBlockingInput.nonBlockingEnabled) {
                    int peeked = nonBlockingInput.peek(100L)
                    if(peeked > 0) {
                        // read peeked character from buffer
                        nonBlockingInput.read(1L)
                        if(peeked == KEYPRESS_CTRL_C || peeked == KEYPRESS_ESC) {
                            executionContext.console.log('  ')
                            executionContext.console.updateStatus("Stopping build. Please wait...")
                            executionContext.cancel()
                        }
                    }
                }
            }
        } finally {
            if (terminal instanceof UnixTerminal) {
                ((UnixTerminal) terminal).enableInterruptCharacter()
            }
        }
        if(!commandFuture.isCancelled()) {
            try {
                return commandFuture.get()
            } catch (ExecutionException e) {
                throw e.cause
            }
        } else {
            return false
        }
    }

    private initializeProfile() {
        BuildSettings.TARGET_DIR?.mkdirs()

        if(!new File(BuildSettings.BASE_DIR, "profile.yml").exists()) {
            populateContextLoader()
        }
        else {
            this.profileRepository = createMavenProfileRepository()
        }

        String profileName = applicationConfig.get(BuildSettings.PROFILE) ?: getSetting(BuildSettings.PROFILE, String, DEFAULT_PROFILE_NAME)
        this.profile = profileRepository.getProfile(profileName)

        if(profile == null) {
            throw new IllegalStateException("No profile found for name [$profileName].")
        }
    }

    protected void populateContextLoader() {
        try {
            if(new File(BuildSettings.BASE_DIR, "build.gradle").exists()) {
                def dependencyMap = new MapReadingCachedGradleOperation<List<URL>>(projectContext, ".dependencies") {

                    @Override
                    List<URL> createMapValue(Object value) {
                        if(value instanceof List) {
                            return ((List)value).collect() { new URL(it.toString()) } as List<URL>
                        }
                        else {
                            return []
                        }
                    }


                    @Override
                    Map<String, List<URL>> readFromGradle(ProjectConnection connection) {
                        def config = applicationConfig
                        GrailsClasspath grailsClasspath = GradleUtil.runBuildActionWithConsoleOutput(connection, projectContext, new ClasspathBuildAction()) { BuildActionExecuter<GrailsClasspath> executer ->
                            executer.withArguments("-Dgrails.profile=${config.navigate("grails", "profile")}")
                        }
                        if(grailsClasspath.error) {
                            GrailsConsole.instance.error("${grailsClasspath.error} Type 'gradle dependencies' for more information")
                            exit 1
                        }
                        return [
                            dependencies: grailsClasspath.dependencies,
                            profiles: grailsClasspath.profileDependencies
                        ]
                    }
                }.call()

                def urls = (List<URL>)dependencyMap.get("dependencies")
                try {
                    // add tools.jar
                    urls.add(new File("${System.getenv('JAVA_HOME')}/lib/tools.jar").toURI().toURL())
                } catch (Throwable e) {
                    // ignore
                }
                def profiles = (List<URL>)dependencyMap.get("profiles")
                URLClassLoader classLoader = new URLClassLoader(urls as URL[])
                this.profileRepository = new StaticJarProfileRepository(classLoader, profiles as URL[])
                Thread.currentThread().contextClassLoader = classLoader
            }
        } catch (Throwable e) {
            e = ExceptionUtils.getRootCause(e)
            GrailsConsole.instance.error("Error initializing classpath: $e.message", e)
            exit(1)
        }
    }


    private CodeGenConfig loadApplicationConfig() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File("grails-app/conf/application.yml")
        if(applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        else {
            applicationYml = new File("application.yml")
            if(applicationYml.exists()) {
                config.loadYml(applicationYml)
            }
        }
        config
    }

    private boolean handleBuiltInCommands(ExecutionContext context) {
        CommandLine commandLine = context.commandLine
        def commandName = commandLine.commandName

        if(commandName && commandName.size()>1 && commandName.startsWith('!')) {
            return executeProcess(context, commandLine.rawArguments)
        }
        else {
            switch(commandName) {
                case '!':
                    return bang(context)
                case 'exit':
                    exitInteractiveMode()
                    return true
                    break
                case 'quit':
                    exitInteractiveMode()
                    return true
                    break
            }
        }

        return false
    }

    protected boolean executeProcess(ExecutionContext context, String[] args) {
        def console = context.console
        try {
            args[0] = args[0].substring(1)
            def process = new ProcessBuilder(args).redirectErrorStream(true).start()
            console.log process.inputStream.getText('UTF-8')
            return true
        } catch (e) {
            console.error "Error occurred executing process: $e.message"
            return false
        }
    }

    /**
     * Removes '\' escape characters from the given string.
     */
    private String unescape(String str) {
        return str.replace('\\', '')
    }

    protected Boolean bang(ExecutionContext context) {
        def console = context.console
        def history = console.reader.history

        //move one step back to !
        history.previous()

        if (!history.previous()) {
            console.error "! not valid. Can not repeat without history"
        }

        //another step to previous command
        String historicalCommand = history.current()
        if (historicalCommand.startsWith("!")) {
            console.error "Can not repeat command: $historicalCommand"
        }
        else {
            return handleCommand(cliParser.parseString(historicalCommand))
        }
        return false
    }

    private void exitInteractiveMode() {
        keepRunning = false
        try {
            GradleAsyncInvoker.POOL.shutdownNow()
        } catch (Throwable e) {
            // ignore
        }
    }


    @Canonical
    public static class ExecutionContextImpl implements ExecutionContext {
        CommandLine commandLine
        @Delegate(excludes = 'getConsole') ProjectContext projectContext
        GrailsConsole console = GrailsConsole.getInstance()

        ExecutionContextImpl(CodeGenConfig config) {
            this(new DefaultCommandLine(), new ProjectContextImpl(GrailsConsole.instance, new File("."), config))
        }

        ExecutionContextImpl(CommandLine commandLine, ProjectContext projectContext) {
            this.commandLine = commandLine
            this.projectContext = projectContext
            if(projectContext?.console) {
                console = projectContext.console
            }
        }

        private List<CommandCancellationListener> cancelListeners=[]
        
        @Override
        public void addCancelledListener(CommandCancellationListener listener) {
            cancelListeners << listener
        }    
        
        @Override
        public void cancel() {
            for(CommandCancellationListener listener : cancelListeners) {
                try {
                    listener.commandCancelled()
                } catch (Exception e) {
                    console.error("Error notifying listener about cancelling command", e)
                }
            }
        }
    }
    
    @Canonical
    private static class ProjectContextImpl implements ProjectContext {
        GrailsConsole console = GrailsConsole.getInstance()
        File baseDir
        CodeGenConfig grailsConfig

        @Override
        public String navigateConfig(String... path) {
            grailsConfig.navigate(path)
        }

        @Override
        ConfigMap getConfig() {
            return grailsConfig
        }

        @Override
        public <T> T navigateConfigForType(Class<T> requiredType, String... path) {
            grailsConfig.navigate(requiredType, path)
        }        
    }
}
