package org.grails.cli

import grails.build.logging.GrailsConsole;

import java.util.List;

import org.codehaus.groovy.grails.cli.parsing.CommandLine;

class YamlCommandHandler implements CommandLineHandler {
    Collection<File> commandFiles
    Profile profile
    List<CommandDescription> commandDescriptions
    
    void initialize() {
        commandDescriptions = commandFiles.collect { File file -> new CommandDescription(name: file.name - '.yml') }
    }

    @Override
    public boolean handleCommand(CommandLine commandLine, GrailsConsole console) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<CommandDescription> listCommands() {
        if(commandDescriptions == null) {
            initialize()
        }
        return commandDescriptions;
    }    
}
