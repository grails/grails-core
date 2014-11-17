package org.grails.cli

import grails.build.logging.GrailsConsole
import grails.config.CodeGenConfig
import grails.io.SystemStreamsRedirector
import grails.util.Environment
import groovy.transform.Canonical
import groovy.transform.CompileStatic

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
import org.grails.cli.profile.ProfileRepository
import org.grails.cli.profile.ProjectContext

@CompileStatic
class GrailsCli {
    public static final String DEFAULT_PROFILE_NAME = 'web'
    private static final int KEYPRESS_CTRL_C = 3
    private static final int KEYPRESS_ESC = 27
    private final SystemStreamsRedirector originalStreams = SystemStreamsRedirector.original() // store original System.in, System.out and System.err
    List<CommandLineHandler> commandLineHandlers=[]
    AggregateCompleter aggregateCompleter=new AggregateCompleter()
    CommandLineParser cliParser = new CommandLineParser()
    boolean keepRunning = true
    Boolean ansiEnabled = null
    Character defaultInputMask = null
    ProfileRepository profileRepository=new ProfileRepository()
    CodeGenConfig applicationConfig
    ProjectContext projectContext
    
    public int execute(String... args) {
        CommandLine mainCommandLine=cliParser.parse(args)
        if(mainCommandLine.hasOption("verbose")) {
            System.setProperty("grails.verbose", "true")
        }
        if(mainCommandLine.hasOption("stacktrace")) {
            System.setProperty("grails.show.stacktrace", "true")
        }
        
        File grailsAppDir=new File("grails-app")
        if(!grailsAppDir.isDirectory()) {
            if(!mainCommandLine || !mainCommandLine.commandName || !mainCommandLine.getRemainingArgs()) {
                System.err.println "usage: create-app appname --profile=web"
            }
            switch(mainCommandLine.commandName) {
                case "create-app":
                    return createApp(mainCommandLine, profileRepository)
                case "create-plugin":
                    return createPlugin(mainCommandLine, profileRepository)
                default:
                    System.err.println "usage: create-app appname --profile=web"
                    return 1

            }


        } else {
            applicationConfig = loadApplicationConfig()
        
            def commandName = mainCommandLine.getCommandName()
            GrailsConsole console=GrailsConsole.getInstance()
            console.setAnsiEnabled(!mainCommandLine.hasOption(CommandLine.NOANSI_ARGUMENT))
            console.defaultInputMask = defaultInputMask
            if(ansiEnabled != null) {
                console.setAnsiEnabled(ansiEnabled)
            }
            File baseDir = new File("").absoluteFile
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
            while(!commandFuture.isDone()) {
                if(nonBlockingInput.isNonBlockingEnabled()) {
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
        aggregateCompleter.getCompleters().addAll((profile.getCompleters(projectContext)?:[]) as Collection)
        aggregateCompleter.getCompleters().add(gradleHandler.createCompleter(projectContext))
    }
    
    private CodeGenConfig loadApplicationConfig() {
        CodeGenConfig config = new CodeGenConfig()
        File applicationYml = new File("grails-app/conf/application.yml")
        if(applicationYml.exists()) {
            config.loadYml(applicationYml)
        }
        config
    }

    private int createPlugin(CommandLine mainCommandLine, ProfileRepository profileRepository) {
        String groupAndAppName = mainCommandLine.getRemainingArgs()[0]
        CreatePluginCommand cmd = new CreatePluginCommand (profileRepository: profileRepository, groupAndAppName: groupAndAppName)
        cmd.run()
        return 0
    }

    private int createApp(CommandLine mainCommandLine, ProfileRepository profileRepository) {
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
    
    public static void main(String[] args) {
        GrailsCli cli=new GrailsCli()
        System.exit(cli.execute(args))
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
