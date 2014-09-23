package org.grails.cli

import groovy.transform.CompileStatic
import jline.console.completer.Completer

@CompileStatic
class SimpleProfile implements Profile {
    File profileDir
    String name
    List<CommandLineHandler> commandLineHandlers = null
    
    public SimpleProfile(String name, File profileDir) {
        this.name = name
        this.profileDir = profileDir 
    }

    @Override
    public Iterable<Completer> getCompleters() {
        null
    }

    @Override
    public Iterable<CommandLineHandler> getCommandLineHandlers() {
        if(commandLineHandlers == null) {
            commandLineHandlers = []
            File commandsDir = new File(profileDir, "commands")
            Collection<File> yamlFiles = commandsDir.listFiles().findAll { File file ->
                file.isFile() && file.name ==~ /^.*\.yml$/ 
            }
            commandLineHandlers << new YamlCommandHandler(commandFiles: yamlFiles, profile: this) 
        }
        commandLineHandlers
    }    
}
