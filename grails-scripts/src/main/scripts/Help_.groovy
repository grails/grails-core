/*
 * Copyright 2004-2005 the original author or authors.
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

/**
 * Gant script that evaluates all installed scripts to create help output
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import grails.util.GrailsNameUtils
import grails.util.Environment
import org.codehaus.groovy.grails.cli.GrailsScriptRunner

includeTargets << grailsScript("_GrailsInit")

class HelpEvaluatingCategory {

    static defaultTask = ""
    static helpText = [:]
    static target(Object obj, Map args, Closure callable) {
        def entry = args.entrySet().iterator().next()
        obj[entry.key] = entry.key
        helpText[(entry.key)] = entry.value

        if (entry.key == "default") {
            defaultTask = "default"
        }
    }

    static getDefaultDescription(Object obj) {
        return helpText[defaultTask]
    }

    static setDefaultTarget(Object obj, val) {
        defaultTask = val
    }
}

File getHelpFile(File script) {
    File helpDir = new File(grailsTmp, "help")
    if (!helpDir.exists()) helpDir.mkdir()
    String scriptname = script.getName()
    return new File(helpDir, scriptname.substring(0, scriptname.lastIndexOf('.')) + ".txt")
}

boolean shouldGenerateHelp(File script) {
    File file = getHelpFile(script)
    return (!file.exists() || file.lastModified() < script.lastModified())
}

target ('default' : "Prints out the help for each script") {
    depends(parseArguments)

    ant.mkdir(dir:grailsTmp)
    def scripts = pluginSettings.availableScripts.collect { it.file }

    def helpText = ""

    if (argsMap["params"]) {
        showHelp(argsMap["params"][0], scripts)
    }
    else {
        println """
Usage (optionals marked with *):
grails [environment]* [options]* [target] [arguments]*

Examples:
grails dev run-app
grails create-app books

"""
        println GrailsScriptRunner.commandLineParser.optionsHelpMessage
        println """
Available Targets (type grails help 'target-name' for more info):"""

        scripts.unique { it.name }. sort{ it.name }.each { file ->
            def scriptName = GrailsNameUtils.getScriptName(file.name)
            if (System.getProperty(Environment.INTERACTIVE_MODE_ENABLED)) {
                println scriptName
            } else {
                println "grails ${scriptName}"
            }
        }
    }
}

showHelp = { String cmd, scripts ->
    def fileName = GrailsNameUtils.getNameFromScript(cmd)
    def file = scripts.find {
        def scriptFileName = it.name[0..-8]
        if (scriptFileName.endsWith("_")) scriptFileName = scriptFileName[0..-2]
        scriptFileName == fileName
    }

    if (file) {
        def gcl = new GroovyClassLoader()
        use(HelpEvaluatingCategory) {
            if (shouldGenerateHelp(file)) {
                try {
                    def script = gcl.parseClass(file).newInstance()
                    script.binding = binding
                    script.run()

                    def scriptName = GrailsNameUtils.getScriptName(file.name)
                    helpText = """
    grails ${scriptName} -- ${getDefaultDescription()}
"""
                    helpText += getUsage(cmd, binding)
                    File helpFile = getHelpFile(file)
                    if (!helpFile.exists()) {
                        helpFile.createNewFile()
                    }
                    helpFile.write(helpText, 'UTF-8')
                }
                catch(Throwable t) {
                    println "Warning: Error caching created help for ${file}: ${t.message}"
                    exit 1
                }
            }
            else {
                helpText = getHelpFile(file).getText("UTF-8")
            }
            println helpText
        }
    }
    else {
        println "No script found for name: $cmd"
    }
}

/**
 * Gets the usage string from the given binding. If no usage string can
 * be found, a default is returned.
 */
private getUsage(cmd, b) {
    if (b.variables.containsKey("USAGE")) {
        return """
Usage (optionals in square brackets):
${binding.USAGE}"""
    }

    return """
Usage (optionals marked with *):
    grails [environment]* ${cmd}
"""
}
