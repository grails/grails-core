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
 * Gant script that manages the application version
 *
 * @author Marc Palmer
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 * @author Graeme Rocher
 *
 * @since 0.5
 */

includeTargets << grailsScript("_GrailsEvents")

target ('default': "Sets the current application version") {

    if (isPluginProject) {
        if (!pluginSettings.basePluginDescriptor.filename) {
            grailsConsole.error "PluginDescripter not found to set version"
            exit 1
        }

        File file = new File(pluginSettings.basePluginDescriptor.filename)
        String descriptorContent = file.getText("UTF-8")

        def pattern = ~/def\s*version\s*=\s*"(.*)"/
        def matcher = (descriptorContent =~ pattern)

        String oldVersion = ''
        if (matcher.size() > 0) {
            oldVersion = matcher[0][0]
        }

        String newVersion
        if (!args) {
            ant.input addProperty: "app.version.new", message: "Enter the new version",
                defaultvalue: oldVersion - 'def version = '
            newVersion = ant.antProject.properties.'app.version.new'
        }
        else {
            newVersion = args
        }
        newVersion = newVersion?.trim()

        String newVersionString = "def version = \"${newVersion}\""

        if (matcher.size() > 0) {
            descriptorContent = descriptorContent.replaceFirst(/def\s*version\s*=\s*".*"/, newVersionString)
        }
        else {
            descriptorContent = descriptorContent.replaceFirst(/\{/,"{\n\t$newVersionString // added by set-version")
        }

        file.withWriter("UTF-8") { it.write descriptorContent }
        event("StatusFinal", [ "Plugin version updated to $newVersion"])
    }
    else {
        if (args) {
            ant.property(name:"app.version.new", value: args)
        }
        else {
            def oldVersion = metadata.'app.version'
            ant.input addProperty: "app.version.new", message: "Enter the new version", defaultvalue: oldVersion
        }

        def newVersion = ant.antProject.properties.'app.version.new'
        metadata.'app.version' = newVersion
        metadata.persist()
        event("StatusFinal", [ "Application version updated to $newVersion"])
    }
}

USAGE = """
    set-version [NUMBER]

where
    NUMBER     = The number to set the current application version to.
"""