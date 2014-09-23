package org.grails.cli

import grails.build.logging.GrailsConsole

import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.yaml.snakeyaml.Yaml;

class YamlCommandHandler implements CommandLineHandler {
    Collection<File> commandFiles
    Profile profile
    List<CommandDescription> commandDescriptions
    Map<String, YamlCommand> commands
    
    void initialize() {
        Yaml yaml=new Yaml()
        commands = commandFiles.collectEntries { File file ->
            def yamlContent = file.withReader { 
                yaml.load(it)
            }
            def commandName = file.name - /\.yml$/
            [commandName, new YamlCommand(name: commandName, file: file, yamlContent: yamlContent, profile: profile)]
        }
        commandDescriptions = commands.collect { String name, YamlCommand cmd ->
            cmd.description
        }
    }

    @Override
    public boolean handleCommand(CommandLine commandLine, GrailsConsole console) {
        YamlCommand cmd = commands.get(commandLine.getCommandName())
        if(cmd) {
            cmd.handleCommand(commandLine, console)
            return true
        }
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
