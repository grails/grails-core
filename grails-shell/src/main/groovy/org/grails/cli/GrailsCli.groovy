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
import grails.config.CodeGenConfig
import grails.io.SystemStreamsRedirector
import grails.util.BuildSettings
import grails.util.Environment
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jline.console.completer.ArgumentCompleter
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.ExternalDependency
import org.gradle.tooling.model.eclipse.EclipseProject
import org.grails.cli.gradle.ClasspathBuildAction
import org.grails.cli.gradle.GradleUtil
import org.grails.cli.gradle.cache.ListReadingCachedGradleOperation
import org.grails.cli.interactive.completers.EscapingFileNameCompletor
import org.grails.cli.interactive.completers.RegexCompletor
import org.grails.cli.profile.Command
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.commands.CommandRegistry
import org.grails.cli.profile.commands.CreateAppCommand
import org.grails.cli.profile.commands.CreatePluginCommand

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import jline.UnixTerminal
import jline.console.UserInterruptException
import jline.console.completer.AggregateCompleter
import jline.internal.NonBlockingInputStream

import org.grails.build.parsing.CommandLine
import org.grails.build.parsing.CommandLineParser
import org.grails.cli.profile.CommandCancellationListener
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.git.GitProfileRepository
import org.grails.cli.profile.ProjectContext


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
    private static final String USAGE_MESSAGE = "Usage: create-app [NAME] --profile=web"
    private final SystemStreamsRedirector originalStreams = SystemStreamsRedirector.original() // store original System.in, System.out and System.err


    AggregateCompleter aggregateCompleter=new AggregateCompleter()
    CommandLineParser cliParser = new CommandLineParser()
    boolean keepRunning = true
    Boolean ansiEnabled = null
    boolean integrateGradle = true
    Character defaultInputMask = null
    GitProfileRepository profileRepository=new GitProfileRepository()
    CodeGenConfig applicationConfig
    ProjectContext projectContext
    Profile profile = null

    /**
     * Main method for running via the command line
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        GrailsCli cli=new GrailsCli()
        System.exit(cli.execute(args))
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

        if(mainCommandLine.environmentSet) {
            System.setProperty(Environment.KEY, mainCommandLine.environment)
        }

        File grailsAppDir=new File("grails-app")
        if(!grailsAppDir.isDirectory()) {
            if(!mainCommandLine || !mainCommandLine.commandName || !mainCommandLine.getRemainingArgs()) {
                System.err.println USAGE_MESSAGE
                return 1
            }
            def cmd = CommandRegistry.getCommand(mainCommandLine.commandName, profileRepository)
            if(cmd) {
                return cmd.handle(createExecutionContext( mainCommandLine )) ? 0 : 1
            }
            else {
                System.err.println USAGE_MESSAGE
                return 1
            }

        } else {
            applicationConfig = loadApplicationConfig()
        
            def commandName = mainCommandLine.commandName
            GrailsConsole console = GrailsConsole.instance
            console.ansiEnabled = !mainCommandLine.hasOption(CommandLine.NOANSI_ARGUMENT)
            console.defaultInputMask = defaultInputMask
            if(ansiEnabled != null) {
                console.ansiEnabled = ansiEnabled
            }
            File baseDir = new File(".").absoluteFile
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

    ExecutionContext createExecutionContext(CommandLine commandLine) {
        new ExecutionContextImpl(commandLine, projectContext)
    }
    
    Boolean handleCommand( CommandLine commandLine ) {
        handleCommand(createExecutionContext(commandLine))
    }
    
    Boolean handleCommand( ExecutionContext context ) {
        if(handleBuiltInCommands(context)) {
            return true
        }

        if(profile.handleCommand(context)) {
            return true;
        }
        return false
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
        while(keepRunning) {
            try {
                if(integrateGradle)
                    GradleUtil.prepareConnection(projectContext.baseDir)
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
            } catch (UserInterruptException e) {
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
        BuildSettings.TARGET_DIR.mkdirs()

        populateContextLoader()

        String profileName = applicationConfig.navigate('grails', 'profile') ?: DEFAULT_PROFILE_NAME
        this.profile = profileRepository.getProfile(profileName)

        if(profile == null) {
            throw new IllegalStateException("No profile found for name [$profileName].")
        }
    }

    protected void populateContextLoader() {
        def urls = new ListReadingCachedGradleOperation<URL>(projectContext, ".dependencies") {
            @Override
            protected URL createListEntry(String str) {
                return new URL(str)
            }

            @Override
            List<URL> readFromGradle(ProjectConnection connection) {
                EclipseProject project = connection.action(new ClasspathBuildAction()).run()
                return project.getClasspath().collect { dependency -> ((ExternalDependency)dependency).file.toURI().toURL() }
            }
        }.call()

        URLClassLoader classLoader = new URLClassLoader(urls as URL[])
        Thread.currentThread().contextClassLoader = classLoader
    }


    private CodeGenConfig loadApplicationConfig() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File("grails-app/conf/application.yml")
        if(applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        config
    }

    private int createPlugin( CommandLine mainCommandLine ) {
        CreatePluginCommand cmd = new CreatePluginCommand ()
        return cmd.handle( createExecutionContext( mainCommandLine ) ) ? 0 : 1
    }

    private int createApp( CommandLine mainCommandLine) {
        CreateAppCommand cmd = new CreateAppCommand()
        return cmd.handle(createExecutionContext( mainCommandLine )) ? 0 : 1
    }

    private boolean handleBuiltInCommands(ExecutionContext context) {
        CommandLine commandLine = context.commandLine
        GrailsConsole console = context.console
        def commandName = commandLine.commandName

        if(commandName && commandName.size()>1 && commandName.startsWith('!')) {
            return executeProcess(context, commandName)
        }
        else {
            switch(commandName) {
                case 'help':
                    Collection<CommandDescription> allCommands=findAllCommands()
                    String remainingArgs = commandLine.getRemainingArgsString()
                    if(remainingArgs?.trim()) {
                        CommandLine remainingArgsCommand = cliParser.parseString(remainingArgs)
                        String helpCommandName = remainingArgsCommand.getCommandName()
                        for (CommandDescription desc : allCommands) {
                            if(desc.name == helpCommandName) {
                                console.println "${desc.name}\t${desc.description}\n${desc.usage}"
                                return true
                            }
                        }
                        console.error "Help for command $helpCommandName not found"
                        return false
                    } else {
                        for (CommandDescription desc : allCommands) {
                            console.println "${desc.name}\t${desc.description}"
                        }
                        console.println("detailed usage with help [command]")
                        return true
                    }
                    break
                case '!':
                    return bang(context)
                case 'exit':
                    exitInteractiveMode()
                    return true
                    break
            }
        }

        return false
    }

    protected boolean executeProcess(ExecutionContext context, String cmd) {
        def console = context.console
        try {
            def args = cmd[1..-1].split(ARG_SPLIT_PATTERN).collect { String it -> unescape(it) }
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
    }

    private Collection<CommandDescription> findAllCommands() {
        profile.getCommands(projectContext).collect() { Command cmd -> cmd.description }.sort(false) { CommandDescription itDesc ->  itDesc.name }
    }

    
    @Canonical
    private static class ExecutionContextImpl implements ExecutionContext {
        CommandLine commandLine
        @Delegate ProjectContext projectContext
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
        GrailsConsole console
        File baseDir
        CodeGenConfig grailsConfig

        @Override
        public String navigateConfig(String... path) {
            grailsConfig.navigate(path)
        }

        @Override
        public <T> T navigateConfigForType(Class<T> requiredType, String... path) {
            grailsConfig.navigate(requiredType, path)
        }        
    }
}
