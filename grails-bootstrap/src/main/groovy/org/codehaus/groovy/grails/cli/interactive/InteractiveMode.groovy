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
import grails.util.PluginBuildSettings

import java.awt.Desktop

import org.codehaus.groovy.grails.cli.GrailsScriptRunner
import org.codehaus.groovy.grails.cli.ScriptExitException
import org.codehaus.groovy.grails.cli.ScriptNotFoundException
import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.codehaus.groovy.grails.cli.parsing.ParseException
import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import org.codehaus.groovy.grails.cli.support.UaaIntegration

/**
 * Provides the implementation of interactive mode in Grails.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class InteractiveMode {
    static final List FIXED_OPEN_OPTIONS = Collections.unmodifiableList([ 'test-report', 'dep-report' ])

    /** Use this to split strings on unescaped whitespace. */
    static final String ARG_SPLIT_PATTERN = /(?<!\\)\s+/

    static InteractiveMode current

    @Delegate GrailsConsole console = GrailsConsole.getInstance()

    GrailsScriptRunner scriptRunner
    BuildSettings settings
    boolean interactiveModeActive = false
    def grailsServer

    /** Options supported by the 'open' command. */
    def openOptions

    protected MetaClassRegistryCleaner registryCleaner = MetaClassRegistryCleaner.createAndRegister()

    InteractiveMode(BuildSettings settings, GrailsScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner
        this.settings = settings
        BuildSettingsHolder.settings = settings
        GroovySystem.getMetaClassRegistry().addMetaClassRegistryChangeEventListener(registryCleaner)

        // Initialise the command options map supported by the 'open' command.
        openOptions = [
            'test-report': [
                path: new File(settings.testReportsDir, "html/index.html").absolutePath,
                description: "Opens the current test report (if it exists)" ],
            'dep-report': [
                path: new File(settings.projectTargetDir, "dependency-report/index.html").absolutePath,
                description: "Opens the current dependency report (if it exists)"] ]
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
        System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")

        if (UaaIntegration.isAvailable() && !UaaIntegration.isEnabled()) {
            UaaIntegration.enable(settings, new PluginBuildSettings(settings), true)
        }
        String originalGrailsEnv = System.getProperty(Environment.KEY)
        String originalGrailsEnvDefault = System.getProperty(Environment.DEFAULT)
        addStatus("Enter a script name to run. Use TAB for completion: ")

        while (interactiveModeActive) {
            String scriptName = showPrompt()
            if (scriptName == null) {
                goodbye()
            }

            try {
                def trimmed = scriptName.trim()
                if (trimmed) {
                    if (trimmed.startsWith("create-app")) {
                        error "You cannot create an application in interactive mode."
                    }
                    else if ("quit".equals(trimmed)) {
                        goodbye()
                    }
                    else if ("exit".equals(trimmed)) {
                        exit()
                    }
                    else if (scriptName.startsWith("open ")) {
                        open scriptName
                    }
                    else if ("!".equals(trimmed)) {
                        bang()
                    }
                    else if (scriptName.startsWith("!")) {
                        execute scriptName
                    }
                    else {
                        try {
                            parseAndExecute(scriptName)
                        } catch (ParseException e) {
                            error "Invalid command: $e.message"
                        }
                    }
                }
            }
            catch (ScriptExitException e) {
                // do nothing. just return to consuming input
            }
            catch (ScriptNotFoundException e) {
                error "Script not found for name $scriptName"
            } catch (Throwable e) {
                error "Error running script $scriptName: $e.message", e
            }
            finally {
                // Reset Environment after each script run
                if (originalGrailsEnv != null) {
                    System.setProperty(Environment.KEY, originalGrailsEnv)
                } else {
                    System.clearProperty(Environment.KEY)
                }
                if (originalGrailsEnvDefault != null) {
                    System.setProperty(Environment.DEFAULT, originalGrailsEnvDefault)
                } else {
                    System.clearProperty(Environment.DEFAULT)
                }
                if (grailsServer == null) {
                    try {
                        registryCleaner.clean()
                    } catch (ignored) {
                        // ignore
                    }
                }
            }
        }

        interactiveModeActive = false
        System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")
    }

    protected void goodbye() {
        updateStatus "Good Bye!"
        System.exit(0)
    }

    protected void exit() {
        if (grailsServer) {
           try {
               updateStatus "Stopping Grails server"
               grailsServer.stop()
           } catch (e) {
               error "Error stopping server: $e.message", e
           }
           finally {
               grailsServer = null
           }
        }
        else {
            goodbye()
        }
    }

    protected void open(String scriptName) {
        String fileName = scriptName[5..-1].trim()

        try {
            final Desktop desktop = Desktop.getDesktop()
            final args = fileName.split(ARG_SPLIT_PATTERN)

            for (arg in args) {
                arg = unescape(arg)

                // Is this arg one of the fixed options for 'open'?
                String fixedOption = openOptions.find { option, value ->
                    // No match if a file matching the name of the
                    // option exists.
                    arg == option && !new File(option).exists()
                }

                File file = new File(fixedOption ? fixedOption.value.path : arg)
                if (file.exists()) {
                    desktop.open file
                }
                else {
                    error "File $arg does not exist"
                }
            }
        } catch (e) {
            error "Could not open file $fileName: $e.message"
        }
    }

    protected void bang() {
        def history = console.reader.history

        //move one step back to !
        history.previous()

        if (!history.previous()) {
            error "! not valid. Can not repeat without history"
            return
        }

        //another step to previous command
        String scriptName = history.current()
        if (scriptName.startsWith("!")) {
            error "Can not repeat command: $scriptName"
        }
        else {
            try {
                parseAndExecute(scriptName)
            } catch (ParseException e) {
                error "Invalid command: $e.message"
            }
        }
    }

    protected void execute(String scriptName) {
        try {
            def args = scriptName[1..-1].split(ARG_SPLIT_PATTERN).collect { unescape(it) }
            def process = new ProcessBuilder(args).redirectErrorStream(true).start()
            log process.inputStream.text
        } catch (e) {
            error "Error occurred executing process: $e.message"
        }
    }

    void parseAndExecute(String scriptName) throws ParseException {
        def parser = GrailsScriptRunner.getCommandLineParser()
        def commandLine = parser.parseString(scriptName)
        prepareConsole(commandLine)
        scriptRunner.executeScriptWithCaching(commandLine)
    }

    void prepareConsole(commandLine) {
        final console = GrailsConsole.instance
        console.stacktrace = commandLine.hasOption(CommandLine.STACKTRACE_ARGUMENT)
        console.verbose = commandLine.hasOption(CommandLine.VERBOSE_ARGUMENT)
    }

    /**
     * Removes '\' escape characters from the given string.
     */
    protected unescape(String str) {
        return str.replace('\\', '')
    }
}
