package org.grails.cli.profile.simple

import groovy.json.JsonBuilder
import groovy.json.JsonParserType
import groovy.json.JsonSlurper

import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProjectContext
import org.yaml.snakeyaml.Yaml

class SimpleCommandHandler implements CommandLineHandler {
    Collection<File> commandFiles
    SimpleProfile profile
    List<CommandDescription> commandDescriptions
    Map<String, SimpleCommand> commands
    Yaml yamlParser=new Yaml()
    // LAX parser for JSON: http://mrhaki.blogspot.ie/2014/08/groovy-goodness-relax-groovy-will-parse.html
    JsonSlurper jsonSlurper = new JsonSlurper().setType(JsonParserType.LAX)
    
    void initialize() {
        commands = commandFiles.collectEntries { File file ->
            Map data = readCommandFile(file)
            String commandName = file.name - ~/\.(yml|json)$/
            
            //saveAsJson(new File(file.parent, "${commandName}.json~"), data)
            
            [commandName, createCommand(commandName, file, data)]
        }
        commandDescriptions = commands.collect { String name, SimpleCommand cmd ->
            cmd.description
        }
    }

    protected Map readCommandFile(File file) {
        Map data = file.withReader {
            if(file.name.endsWith('.json')) {
                jsonSlurper.parse(it) as Map
            } else {
                yamlParser.loadAs(it, Map)
            }
        }
        return data
    }

    protected SimpleCommand createCommand(String commandName, File file, Map data) {
        SimpleCommand command = new SimpleCommand(name: commandName, file: file, data: data, profile: profile)
        Object minArguments = data?.minArguments
        command.minArguments = minArguments instanceof Integer ? minArguments : 1
        command
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
    public List<CommandDescription> listCommands(ProjectContext context) {
        if(commandDescriptions == null) {
            initialize()
        }
        return commandDescriptions;
    }    
}
