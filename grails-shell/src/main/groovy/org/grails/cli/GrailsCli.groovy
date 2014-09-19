package org.grails.cli

import grails.build.logging.GrailsConsole
import grails.util.Environment
import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter
import jline.console.completer.Completer

import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.codehaus.groovy.grails.cli.parsing.CommandLineParser

class GrailsCli {
    List<CommandLineHandler> commandLineHandlers=[]
    Completer aggregateCompleter=new AggregateCompleter()
    CommandLineParser cliParser = new CommandLineParser()
    
    public void run(String... args) {
        File applicationProperties=new File("application.properties")
        if(!applicationProperties.exists()) {
            if(!args) {
                println "usage: create-app appname --profile=web"
                System.exit(1)
            }
            if(args[0] == 'create-app') {
                def appname = args[1]
                def profile=null
                if(args.size() > 2) {
                    def matches = (args[2] =~ /^--profile=(.*?)$/)
                    if (matches) {
                        profile=matches.group(1)
                    }
                }
                println "app: $appname profile: $profile"
                CreateAppCommand cmd = new CreateAppCommand(appname: appname, profile: profile)
                cmd.run()
            }
        } else {
            CommandLine mainCommandLine=cliParser.parse(args)
            def commandName = mainCommandLine.getCommandName()
            if(commandName) {
                handleCommand(mainCommandLine, GrailsConsole.getInstance())
            } else {
                System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")
                GrailsConsole console=GrailsConsole.getInstance()
                console.reader.addCompleter(aggregateCompleter)
                console.println("Starting interactive mode...")
                while(true) {
                    String commandLine = console.showPrompt()
                    handleCommand(cliParser.parse(commandLine), console)
                }
            }
        }
    }
    
    boolean handleCommand(CommandLine commandLine, GrailsConsole console) {
        if(commandLine.getCommandName()=='help') {
            String remainingArgs = commandLine.getRemainingArgsString()
            if(remainingArgs?.trim()) {
                CommandLine remainingArgsCommand = cliParser.parse(remainingArgs) 
                String helpCommandName = remainingArgsCommand.getCommandName()
                for(CommandLineHandler handler : commandLineHandlers) {
                    if(handler.handleHelp(helpCommandName)) {
                        return true
                    }
                }
            }
            console.println("usage: help command")
            return false
        }        
        for(CommandLineHandler handler : commandLineHandlers) {
             if(handler.handleCommand(commandLine, console)) {
                 return true
             }
        }
        console.error("Command not found ${commandLine.commandName}")
        return false
    }
    
    public static void main(String[] args) {
        GrailsCli cli=new GrailsCli()
        cli.run(args)
    }
}
