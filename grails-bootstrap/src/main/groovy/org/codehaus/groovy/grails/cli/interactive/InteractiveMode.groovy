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
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProcess
import org.codehaus.groovy.grails.cli.fork.testing.ForkedGrailsTestRunner

import java.awt.Desktop

import org.codehaus.groovy.grails.cli.GrailsScriptRunner
import org.codehaus.groovy.grails.cli.ScriptExitException
import org.codehaus.groovy.grails.cli.ScriptNotFoundException
import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.codehaus.groovy.grails.cli.parsing.ParseException
import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi

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

    protected GrailsInteractiveCompletor interactiveCompletor

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
        addStatus "Application loaded in interactive mode. Type 'stop-app' to shutdown."
        this.grailsServer = grailsServer
    }

    static boolean isActive() {
        getCurrent() != null && getCurrent().interactiveModeActive
    }

    void run() {
        current = this
        System.setProperty("grails.disable.exit", "true") // you can't exit completely in interactive mode from a script

        interactiveCompletor = new GrailsInteractiveCompletor(settings, scriptRunner.availableScripts)
        console.reader.addCompletor(interactiveCompletor)
        interactiveModeActive = true
        System.setProperty(Environment.INTERACTIVE_MODE_ENABLED, "true")

        new BaseSettingsApi(settings, false).enableUaa()
        String originalGrailsEnv = System.getProperty(Environment.KEY)
        String originalGrailsEnvDefault = System.getProperty(Environment.DEFAULT)
        addStatus("Enter a script name to run. Use TAB for completion: ")

        Runtime.addShutdownHook {
            final files = settings.projectWorkDir.listFiles()
            if (files) {
                final toDelete = files.findAll { File f -> f.name.endsWith("-resume") && f.directory }
                toDelete*.delete()
            }
        }

        startBackgroundTestRunner()

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
                    else if (trimmed.startsWith("install-plugin")) {
                        error "You cannot install a plugin in interactive mode."
                    }
                    else if (trimmed.startsWith("uninstall-plugin")) {
                        error "You cannot uninstall a plugin in interactive mode."
                    }
                    else if ("quit".equals(trimmed)) {
                        quit()
                    } else if ('stop-app'.equals(trimmed)) {
                        stopApp()
                    } else if ("exit".equals(trimmed)) {
                        exit()
                    }
                    else if ("restart-daemon".equals(trimmed)) {
                        restartDaemon()
                    }
                    else if ("start-daemon".equals(trimmed)) {
                        restartDaemon()
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

    boolean backgroundTestRunnerStarted = false
    @CompileStatic
    protected void startBackgroundTestRunner() {
        if (backgroundTestRunnerStarted) return

        backgroundTestRunnerStarted = true
        Thread.start {
            // start a background JVM ready to run tests
            if (settings.forkSettings.test) {
                final runner = new ForkedGrailsTestRunner(settings)
                if (settings.forkSettings.test instanceof Map) {
                    runner.configure((Map) settings.forkSettings.test)
                }
                if (runner.isForkingReserveEnabled()) {
                    runner.forkReserve()
                }
                else if (runner.daemon) {
                    runner.restartDaemon()
                }
            }
        }
    }

    @CompileStatic
    void restartDaemon() {
        final runner = new ForkedGrailsTestRunner(settings)
        if (settings.forkSettings.test instanceof Map) {
            runner.configure((Map) settings.forkSettings.test)
        }
        runner.restartDaemon()
    }
    protected void quit() {
        exit true
    }

    protected void goodbye() {
        updateStatus "Goodbye"
        System.exit(0)
    }

    protected void stopApp() {
        if (settings.forkSettings?.get('run')) {
            parseAndExecute 'stop-app'
        } else if (grailsServer) {
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
    }

    protected void exit(boolean shouldStopApp = false) {
        if (shouldStopApp) {
            stopApp()
        }
        goodbye()
    }

    protected void open(String scriptName) {
        if (!Desktop.isDesktopSupported()) {
            error "The Desktop API isn't supported for this platform"
            return
        }

        String fileName = scriptName[5..-1].trim()

        try {
            final Desktop desktop = Desktop.getDesktop()
            final args = fileName.split(ARG_SPLIT_PATTERN)

            for (arg in args) {
                arg = unescape(arg)

                // Is this arg one of the fixed options for 'open'?
                def fixedOption = openOptions.find { option, value ->
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
        if (commandLine.hasOption(CommandLine.DEBUG_FORK)) {
            System.setProperty(ForkedGrailsProcess.DEBUG_FORK, "true")
        }
        else {
            System.setProperty(ForkedGrailsProcess.DEBUG_FORK, "false")
        }
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

    void refresh() {
        final scripts = scriptRunner.getAvailableScripts()
        interactiveCompletor.setCandidateStrings(GrailsInteractiveCompletor.getScriptNames(scripts))
    }
}
