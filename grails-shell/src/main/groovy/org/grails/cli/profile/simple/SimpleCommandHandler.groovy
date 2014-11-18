/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.cli.profile.simple

import groovy.json.JsonBuilder
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProjectContext
import org.yaml.snakeyaml.Yaml

/**
 * A {@link CommandLineHandler} that can read commands defined in JSON or YAML
 *
 * @author Lari Hotari
 * @author Graeme Rcher
 */
@CompileStatic
class SimpleCommandHandler implements CommandLineHandler {

    Collection<File> commandFiles
    SimpleProfile profile

    protected List<CommandDescription> commandDescriptions
    protected Map<String, SimpleCommand> commands
    protected Yaml yamlParser=new Yaml()
    // LAX parser for JSON: http://mrhaki.blogspot.ie/2014/08/groovy-goodness-relax-groovy-will-parse.html
    protected JsonSlurper jsonSlurper = new JsonSlurper().setType(JsonParserType.LAX)

    SimpleCommandHandler(Collection<File> commandFiles, SimpleProfile profile) {
        this.commandFiles = commandFiles
        this.profile = profile
    }

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

    @Override
    public boolean handle(ExecutionContext context) {
        if(commandDescriptions == null) {
            initialize()
        }

        SimpleCommand cmd = commands.get(context.commandLine.getCommandName())
        if(cmd) {
            return cmd.handle(context)
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

    protected Map readCommandFile(File file) {
        Map data = file.withReader { BufferedReader reader ->
            if(file.name.endsWith('.json')) {
                jsonSlurper.parse(reader) as Map
            } else {
                yamlParser.loadAs(reader, Map)
            }
        }
        return data
    }

    protected SimpleCommand createCommand(String commandName, File file, Map data) {
        SimpleCommand command = new SimpleCommand( commandName, profile, file, data )
        Object minArguments = data?.minArguments
        command.minArguments = minArguments instanceof Integer ? (Integer)minArguments : 1
        command
    }

    private saveAsJson(File file, Map data) {
        file.text = new JsonBuilder(data).toPrettyString()
    }


}
