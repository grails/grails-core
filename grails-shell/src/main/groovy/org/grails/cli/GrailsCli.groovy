package org.grails.cli

import grails.build.logging.GrailsConsole
import grails.util.Environment
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jline.console.completer.AggregateCompleter

import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.codehaus.groovy.grails.cli.parsing.CommandLineParser
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.grails.cli.profile.ProfileRepository

@CompileStatic
class GrailsCli {
    public static final String DEFAULT_PROFILE_NAME = 'web'
    List<CommandLineHandler> commandLineHandlers=[]
    AggregateCompleter aggregateCompleter=new AggregateCompleter()
    CommandLineParser cliParser = new CommandLineParser()
    boolean keepRunning = true
    Boolean ansiEnabled = null
    Character defaultInputMask = null
    ProfileRepository profileRepository=new ProfileRepository()
    
    public int execute(String... args) {
        CommandLine mainCommandLine=cliParser.parse(args)
        File applicationProperties=new File("application.properties")
        if(!applicationProperties.exists()) {
            if(!mainCommandLine || !mainCommandLine.commandName || mainCommandLine.commandName != 'create-app' || !mainCommandLine.getRemainingArgs()) {
                System.err.println "usage: create-app appname --profile=web"
                return 1
            }
            return createApp(mainCommandLine, profileRepository)
        } else {
            Profile profile = profileRepository.getProfile(DEFAULT_PROFILE_NAME)
            commandLineHandlers.addAll(profile.getCommandLineHandlers() as Collection)
            aggregateCompleter.getCompleters().addAll((profile.getCompleters()?:[]) as Collection)
        
            def commandName = mainCommandLine.getCommandName()
            GrailsConsole console=GrailsConsole.getInstance()
            console.setAnsiEnabled(!mainCommandLine.hasOption(CommandLine.NOANSI_ARGUMENT))
            console.defaultInputMask = defaultInputMask
            if(ansiEnabled != null) {
                console.setAnsiEnabled(ansiEnabled)
            }
            File baseDir = new File(".").absoluteFile
            if(commandName) {
                handleCommand(mainCommandLine, console, baseDir)
            } else {
                System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")
                console.reader.addCompleter(aggregateCompleter)
                console.println("Starting interactive mode...")
                while(keepRunning) {
                    String commandLine = console.showPrompt()
                    if(commandLine==null) {
                        // CTRL-D was pressed, exit interactive mode
                        exitInteractiveMode()
                    } else {
                        handleCommand(cliParser.parseString(commandLine), console, baseDir)
                    }
                }
            }
        }
        return 0
    }

    private int createApp(CommandLine mainCommandLine, ProfileRepository profileRepository) {
        String appname = mainCommandLine.getRemainingArgs()[0]
        String profileName = mainCommandLine.optionValue('profile')
        if(!profileName) {
            profileName=DEFAULT_PROFILE_NAME
        }
        Profile profile = profileRepository.getProfile(profileName)
        if(profile) {
            CreateAppCommand cmd = new CreateAppCommand(profileRepository: profileRepository, appname: appname, profile: profileName)
            cmd.run()
            return 0
        } else {
            System.err.println "Cannot find profile $profileName"
            return 1
        }
    }
    
    boolean handleCommand(CommandLine commandLine, GrailsConsole console, File baseDir) {
        ExecutionContext context = new ExecutionContextImpl(commandLine, console, baseDir)
        
        if(handleBuiltInCommands(context)) {
            return true
        }
        for(CommandLineHandler handler : commandLineHandlers) {
             if(handler.handleCommand(context)) {
                 return true
             }
        }
        console.error("Command not found ${commandLine.commandName}")
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
            allCommands.addAll((handler.listCommands() ?: []) as Collection)
        }
        allCommands
    }
    
    public static void main(String[] args) {
        GrailsCli cli=new GrailsCli()
        System.exit(cli.execute(args))
    }
    
    @Canonical
    private static class ExecutionContextImpl implements ExecutionContext {
        CommandLine commandLine
        GrailsConsole console
        File baseDir
    }
}
