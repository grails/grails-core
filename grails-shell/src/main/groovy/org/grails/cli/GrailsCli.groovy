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
import grails.util.Environment
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jline.console.completer.ArgumentCompleter
import org.grails.cli.interactive.completors.EscapingFileNameCompletor
import org.grails.cli.interactive.completors.RegexCompletor

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
import org.grails.cli.gradle.GradleConnectionCommandLineHandler
import org.grails.cli.profile.CommandCancelledListener
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.GitProfileRepository
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
    public static final String DEFAULT_PROFILE_NAME = 'web'
    private static final int KEYPRESS_CTRL_C = 3
    private static final int KEYPRESS_ESC = 27
    private static final String USAGE_MESSAGE = "Usage: create-app [NAME] --profile=web"
    private final SystemStreamsRedirector originalStreams = SystemStreamsRedirector.original() // store original System.in, System.out and System.err


    List<CommandLineHandler> commandLineHandlers=[]
    AggregateCompleter aggregateCompleter=new AggregateCompleter()
    CommandLineParser cliParser = new CommandLineParser()
    boolean keepRunning = true
    Boolean ansiEnabled = null
    Character defaultInputMask = null
    GitProfileRepository profileRepository=new GitProfileRepository()
    CodeGenConfig applicationConfig
    ProjectContext projectContext

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
        
        File grailsAppDir=new File("grails-app")
        if(!grailsAppDir.isDirectory()) {
            if(!mainCommandLine || !mainCommandLine.commandName || !mainCommandLine.getRemainingArgs()) {
                System.err.println USAGE_MESSAGE
            }
            switch(mainCommandLine.commandName) {
                case "create-app":
                    return createApp(mainCommandLine, profileRepository)
                case "create-plugin":
                    return createPlugin(mainCommandLine, profileRepository)
                default:
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
                handleCommand(args.join(" "), mainCommandLine)
            } else {
                handleInteractiveMode()
            }
        }
        return 0
    }

    ExecutionContext createExecutionContext(String unparsedCommandLine, CommandLine commandLine) {
        new ExecutionContextImpl(unparsedCommandLine, commandLine, projectContext)
    }
    
    Boolean handleCommand(String unparsedCommandLine, CommandLine commandLine) {
        handleCommand(createExecutionContext(unparsedCommandLine, commandLine))
    }
    
    Boolean handleCommand(ExecutionContext context) {
        if(handleBuiltInCommands(context)) {
            return true
        }
        for(CommandLineHandler handler : commandLineHandlers) {
             if(handler.handleCommand(context)) {
                 return true
             }
        }
        context.console.error("Command not found ${context.commandLine.commandName}")
        return false
    }


    private void handleInteractiveMode() {
        System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")
        GrailsConsole console = projectContext.console
        console.reader.setHandleUserInterrupt(true)
        console.reader.addCompleter(aggregateCompleter)
        console.println("Starting interactive mode...")
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
                String commandLine = console.showPrompt()
                if(commandLine==null) {
                    // CTRL-D was pressed, exit interactive mode
                    exitInteractiveMode()
                } else if (commandLine.trim()) {
                    if(nonBlockingInput.isNonBlockingEnabled()) {
                        handleCommandWithCancellationSupport(commandLine, commandExecutor, nonBlockingInput)
                    } else {
                        handleCommand(commandLine, cliParser.parseString(commandLine))
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
        ExecutionContext executionContext = createExecutionContext(commandLine, cliParser.parseString(commandLine))
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
        String profileName = applicationConfig.navigate('grails', 'profile') ?: DEFAULT_PROFILE_NAME
        Profile profile = profileRepository.getProfile(profileName)
        commandLineHandlers.addAll(profile.getCommandLineHandlers(projectContext) as Collection)
        def gradleHandler = new GradleConnectionCommandLineHandler()
        commandLineHandlers.add(gradleHandler)

        def completers = aggregateCompleter.getCompleters()

        completers.add(new ArgumentCompleter(
                new RegexCompletor("!\\w+"), new EscapingFileNameCompletor())
        )
        completers.addAll((profile.getCompleters(projectContext)?:[]) as Collection)
        completers.add(gradleHandler.createCompleter(projectContext))
    }

    private CodeGenConfig loadApplicationConfig() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File("grails-app/conf/application.yml")
        if(applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        config
    }

    private int createPlugin(CommandLine mainCommandLine, GitProfileRepository profileRepository) {
        String groupAndAppName = mainCommandLine.getRemainingArgs()[0]
        CreatePluginCommand cmd = new CreatePluginCommand (profileRepository: profileRepository, groupAndAppName: groupAndAppName)
        cmd.run()
        return 0
    }

    private int createApp(CommandLine mainCommandLine, GitProfileRepository profileRepository) {
        String groupAndAppName = mainCommandLine.getRemainingArgs()[0]
        String profileName = mainCommandLine.optionValue('profile')
        if(!profileName) {
            profileName=DEFAULT_PROFILE_NAME
        }
        Profile profile = profileRepository.getProfile(profileName)
        if(profile) {
            CreateAppCommand cmd = new CreateAppCommand(profileRepository: profileRepository, groupAndAppName: groupAndAppName, profile: profileName)
            cmd.run()
            return 0
        } else {
            System.err.println "Cannot find profile $profileName"
            return 1
        }
    }
    private boolean handleBuiltInCommands(ExecutionContext context) {
        CommandLine commandLine = context.commandLine
        GrailsConsole console = context.console
        switch(commandLine.getCommandName()) {
            case 'help':
                List<CommandDescription> allCommands=findAllCommands()
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
            case 'exit':
                exitInteractiveMode()
                return true
                break
        }
        return false
    }
    
    private void exitInteractiveMode() {
        keepRunning = false
    }

    private List<CommandDescription> findAllCommands() {
        List<CommandDescription> allCommands=[]
        for(CommandLineHandler handler : commandLineHandlers) {
            allCommands.addAll((handler.listCommands(projectContext) ?: []) as Collection)
        }
        allCommands = allCommands.sort(false) { CommandDescription desc ->
            desc.getName()
        }
        allCommands
    }

    
    @Canonical
    private static class ExecutionContextImpl implements ExecutionContext {
        String unparsedCommandLine
        CommandLine commandLine
        @Delegate ProjectContext projectContext
        private List<CommandCancelledListener> cancelListeners=[]
        
        @Override
        public void addCancelledListener(CommandCancelledListener listener) {
            cancelListeners << listener
        }    
        
        @Override
        public void cancel() {
            for(CommandCancelledListener listener : cancelListeners) {
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
