package org.grails.cli

import grails.build.logging.GrailsConsole
import grails.util.Environment
import groovy.transform.CompileStatic
import jline.console.completer.AggregateCompleter

import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.codehaus.groovy.grails.cli.parsing.CommandLineParser
import org.grails.cli.profile.CommandDescription
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
    
    public int execute(String... args) {
        CommandLine mainCommandLine=cliParser.parse(args)
        ProfileRepository profileRepository=new ProfileRepository()
        File applicationProperties=new File("application.properties")
        if(!applicationProperties.exists()) {
            if(!mainCommandLine || !mainCommandLine.commandName || mainCommandLine.commandName != 'create-app' || !mainCommandLine.getRemainingArgs()) {
                System.err.println "usage: create-app appname --profile=web"
                return 1
            }
            createApp(mainCommandLine, profileRepository)
        } else {
            Profile profile = profileRepository.getProfile('web')
            commandLineHandlers.addAll(profile.getCommandLineHandlers() as Collection)
            aggregateCompleter.getCompleters().addAll((profile.getCompleters()?:[]) as Collection)
        
            def commandName = mainCommandLine.getCommandName()
            GrailsConsole console=GrailsConsole.getInstance()
            console.setAnsiEnabled(!mainCommandLine.hasOption(CommandLine.NOANSI_ARGUMENT))
            if(ansiEnabled != null) {
                console.setAnsiEnabled(ansiEnabled)
            }
            if(commandName) {
                handleCommand(mainCommandLine, console)
            } else {
                System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")
                console.reader.addCompleter(aggregateCompleter)
                console.println("Starting interactive mode...")
                while(keepRunning) {
                    String commandLine = console.showPrompt()
                    handleCommand(cliParser.parseString(commandLine), console)
                }
            }
        }
        return 0
    }

    private createApp(CommandLine mainCommandLine, ProfileRepository profileRepository) {
        String appname = mainCommandLine.getRemainingArgs()[0]
        String profileName = mainCommandLine.optionValue('profile')
        if(!profileName) {
            profileName=DEFAULT_PROFILE_NAME
        }
        Profile profile = profileRepository.getProfile(profileName)
        if(profile) {
            CreateAppCommand cmd = new CreateAppCommand(profileRepository: profileRepository, appname: appname, profile: profileName)
            cmd.run()
        } else {
            System.err.println "Cannot find profile $profileName"
            return 1
        }
    }
    
    boolean handleCommand(CommandLine commandLine, GrailsConsole console) {
        if(handleBuiltInCommands(commandLine, console)) {
            return true
        }
        for(CommandLineHandler handler : commandLineHandlers) {
             if(handler.handleCommand(commandLine, console)) {
                 return true
             }
        }
        console.error("Command not found ${commandLine.commandName}")
        return false
    }

    private boolean handleBuiltInCommands(CommandLine commandLine, GrailsConsole console) {
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
}
