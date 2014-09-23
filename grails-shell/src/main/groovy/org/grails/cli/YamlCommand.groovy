package org.grails.cli

import grails.build.logging.GrailsConsole;

import org.codehaus.groovy.grails.cli.parsing.CommandLine;

class YamlCommand {
    String name
    File file
    Object yamlContent
    Profile profile
    
    CommandDescription getDescription() {
        new CommandDescription(name: name, description: yamlContent?.description, usage: yamlContent?.usage)
    }
    
    public boolean handleCommand(CommandLine commandLine, GrailsConsole console) {
        
    }
}
