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
package org.grails.cli.profile.commands.factory

import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile
import org.grails.cli.profile.commands.DefaultMultiStepCommand
import org.yaml.snakeyaml.Yaml

import java.util.regex.Pattern


/**
 * A {@link CommandFactory} that can discover commands defined in YAML or JSON
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class YamlCommandFactory extends FileBasedCommandFactory<Map> {
    protected Yaml yamlParser=new Yaml()
    // LAX parser for JSON: http://mrhaki.blogspot.ie/2014/08/groovy-goodness-relax-groovy-will-parse.html
    protected JsonSlurper jsonSlurper = new JsonSlurper().setType(JsonParserType.LAX)

    final Pattern fileExtensionPattern = ~/\.(yml|json)$/
    final Pattern fileNamePattern = ~/^.*\.(yml|json)$/


    @Override
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

    protected Command createCommand(Profile profile, String commandName, File file, Map data) {
        Command command = new DefaultMultiStepCommand( commandName, profile, file, data )
        Object minArguments = data?.minArguments
        command.minArguments = minArguments instanceof Integer ? (Integer)minArguments : 1
        command
    }
}
