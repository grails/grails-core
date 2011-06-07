/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.cli.interactive

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.cli.GrailsScriptRunner
import org.codehaus.groovy.grails.cli.ScriptNotFoundException
import org.codehaus.groovy.grails.cli.logging.GrailsConsole

/**
 * Provides the implementation of interactive mode in Grails
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class InteractiveMode {

    @Delegate GrailsConsole console = GrailsConsole.getInstance()

    GrailsScriptRunner scriptRunner
    BuildSettings settings
    boolean active = false

    InteractiveMode(BuildSettings settings, GrailsScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner
        this.settings = settings;
    }

    void run() {

        console.reader.addCompletor(new GrailsInteractiveCompletor(settings, scriptRunner.availableScripts))
        active = true

        while(active) {
            def scriptName = userInput("Enter a script name to run. Use TAB for completion: ")
            try {
                if(scriptName.trim()) {
                    if(scriptName.startsWith("!")) {
                        try {
                            def process=new ProcessBuilder(scriptName[1..-1].split(" ")).redirectErrorStream(true).start()
                            log process.inputStream.text
                        } catch (e) {
                            error "Error occurred executing process: ${e.message}"
                        }
                    }
                    else if(scriptName.contains(" ")) {
                        def i = scriptName.indexOf(" ")
                        def args = scriptName[i..-1]
                        scriptName = scriptName[0..i]
                        scriptRunner.executeScriptWithCaching(GrailsNameUtils.getNameFromScript(scriptName), Environment.current.name, args )
                    }
                    else {
                        scriptRunner.executeScriptWithCaching(GrailsNameUtils.getNameFromScript(scriptName), Environment.current.name)
                    }
                }
                else {
                    error "Not script name specified"
                }
            } catch (ScriptNotFoundException e) {
                error "Script not found for name $scriptName"
            }
        }
    }
}
