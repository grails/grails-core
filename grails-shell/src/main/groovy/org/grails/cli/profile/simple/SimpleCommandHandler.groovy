package org.grails.cli.profile.simple

import groovy.json.JsonBuilder
import groovy.json.JsonParserType
import groovy.json.JsonSlurper

import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.Profile
import org.yaml.snakeyaml.Yaml

class SimpleCommandHandler implements CommandLineHandler {
    Collection<File> commandFiles
    Profile profile
    List<CommandDescription> commandDescriptions
    Map<String, SimpleCommand> commands
    
    void initialize() {
        Yaml yamlParser=new Yaml()
        // LAX parser for JSON: http://mrhaki.blogspot.ie/2014/08/groovy-goodness-relax-groovy-will-parse.html
        JsonSlurper jsonSlurper = new JsonSlurper().setType(JsonParserType.LAX)
        commands = commandFiles.collectEntries { File file ->
            Map data = file.withReader { 
                if(file.name.endsWith('.json')) {
                    jsonSlurper.parse(it) as Map
                } else {
                    yamlParser.loadAs(it, Map)
                }
            }
            String commandName = file.name - ~/\.(yml|json)$/
            
            //saveAsJson(new File(file.parent, "${commandName}.json~"), data)
            
            [commandName, new SimpleCommand(name: commandName, file: file, data: data, profile: profile)]
        }
        commandDescriptions = commands.collect { String name, SimpleCommand cmd ->
            cmd.description
        }
    }

    private saveAsJson(File file, Map data) {
        file.text = new JsonBuilder(data).toPrettyString()
    }

    @Override
    public boolean handleCommand(ExecutionContext context) {
        SimpleCommand cmd = commands.get(context.commandLine.getCommandName())
        if(cmd) {
            cmd.handleCommand(context)
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
