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
import grails.build.logging.GrailsConsole
import grails.util.BuildSettingsHolder

/**
 * Provides the implementation of interactive mode in Grails
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class InteractiveMode {

    static InteractiveMode current

    @Delegate GrailsConsole console = GrailsConsole.getInstance()

    GrailsScriptRunner scriptRunner
    BuildSettings settings
    boolean active = false
    def grailsServer

    InteractiveMode(BuildSettings settings, GrailsScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner
        this.settings = settings;
        BuildSettingsHolder.settings = settings
    }

    void setGrailsServer(grailsServer) {
        addStatus "Application loaded in interactive mode. Type 'exit' to shutdown."
        this.grailsServer = grailsServer
    }

    void run() {
        current = this

        console.reader.addCompletor(new GrailsInteractiveCompletor(settings, scriptRunner.availableScripts))
        active = true

        while(active) {
            def scriptName = userInput("Enter a script name to run. Use TAB for completion: ")
            try {
                def trimmed = scriptName.trim()
                if(trimmed) {
                    if("quit".equals(trimmed)) {
                        break
                    }
                    if("exit".equals(trimmed)) {
                        if(grailsServer) {
                           try {
                               updateStatus "Stopping Grails server"
                               grailsServer.stop()
                           } catch (e) {
                               error "Error stopping server: ${e.message}", e
                           }
                           finally {
                               grailsServer = null
                           }

                        }
                        else {
                            break
                        }
                    }
                    else if(scriptName.startsWith("!")) {
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
            } catch (Exception e) {
                error "Error running script $scriptName: ${e.message}", e
            }

        }
    }
}
