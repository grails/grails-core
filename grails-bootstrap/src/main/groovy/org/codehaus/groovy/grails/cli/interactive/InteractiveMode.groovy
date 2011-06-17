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

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.cli.GrailsScriptRunner
import org.codehaus.groovy.grails.cli.ScriptNotFoundException
import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import org.codehaus.groovy.grails.cli.parsing.ParseException

/**
 * Provides the implementation of interactive mode in Grails.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class InteractiveMode {

    static InteractiveMode current

    @Delegate GrailsConsole console = GrailsConsole.getInstance()

    GrailsScriptRunner scriptRunner
    BuildSettings settings
    boolean interactiveModeActive = false
    def grailsServer

    private MetaClassRegistryCleaner registryCleaner = new MetaClassRegistryCleaner();

    InteractiveMode(BuildSettings settings, GrailsScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner
        this.settings = settings;
        BuildSettingsHolder.settings = settings
        GroovySystem.getMetaClassRegistry().addMetaClassRegistryChangeEventListener(registryCleaner)
    }

    void setGrailsServer(grailsServer) {
        addStatus "Application loaded in interactive mode. Type 'exit' to shutdown."
        this.grailsServer = grailsServer
    }

    static boolean isActive() {
        getCurrent() != null && getCurrent().interactiveModeActive
    }

    void run() {
        current = this
        System.setProperty("grails.disable.exit", "true") // you can't exit completely in interactive mode from a script

        console.reader.addCompletor(new GrailsInteractiveCompletor(settings, scriptRunner.availableScripts))
        interactiveModeActive = true

        while(interactiveModeActive) {
            def scriptName = userInput("Enter a script name to run. Use TAB for completion: ")
            try {
                def trimmed = scriptName.trim()
                if (trimmed) {
                    if ("quit".equals(trimmed)) {
                        System.exit(0)
                    }
                    if ("exit".equals(trimmed)) {
                        if (grailsServer) {
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
                            System.exit(0)
                        }
                    }
                    else if (scriptName.startsWith("!")) {
                        try {
                            def process=new ProcessBuilder(scriptName[1..-1].split(" ")).redirectErrorStream(true).start()
                            log process.inputStream.text
                        } catch (e) {
                            error "Error occurred executing process: ${e.message}"
                        }
                    }
                    else {
                        def parser = GrailsScriptRunner.getCommandLineParser()
                        try {
                            def commandLine = parser.parseString(scriptName)
                            scriptRunner.executeScriptWithCaching(commandLine)
                        } catch (ParseException e) {
                            error "Invalid command: ${e.message}"
                        }
                    }

                }
                else {
                    error "Not script name specified"
                }
            } catch (ScriptNotFoundException e) {
                error "Script not found for name $scriptName"
            } catch (Throwable e) {
                error "Error running script $scriptName: ${e.message}", e
            }
            finally {
                if (grailsServer == null) {
                    try {
                        registryCleaner.clean()
                    } catch (e) {
                        // ignore
                    }
                }
            }
        }
    }
}
