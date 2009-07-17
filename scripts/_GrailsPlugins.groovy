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

import groovy.xml.dom.DOMCategory

/**
 * Gant script that handles the installation of Grails plugins
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsPackage")

ERROR_MESSAGE = """
You need to specify either the direct URL of the plugin or the name and version
of a distributed Grails plugin found at ${pluginSVN}
For example:
'grails install-plugin acegi 0.1'
or
'grails install-plugin ${pluginBinaryDistURL}/grails-acegi-0.1.zip"""

globalInstall = false

target(cachePlugin:"Implementation target") {
    depends(configureProxy)
    fullPluginName = cacheKnownPlugin(pluginName, pluginRelease)
}


doInstallPluginFromURL = { URL url ->
    withPluginInstall {
        downloadRemotePlugin(url, pluginsBase)
    }
}

doInstallPluginZip = { File file ->  
    withPluginInstall {
        cacheLocalPlugin(file)
    }
}

doInstallPlugin = { name, version = null ->
    withPluginInstall {
        cacheKnownPlugin(name,version)
    }
}

doInstallPluginFromGrailsHomeOrRepository = { name, version = null ->
    withPluginInstall {
        boolean hasVersion = name && version
        File pluginZip
        if(hasVersion) {
            def zipName = "grails-${name}-${version}"
            pluginZip = new File("${grailsSettings.grailsHome}/plugins/${zipName}.zip")
            if(!pluginZip.exists()) {
                pluginZip = new File("${grailsSettings.grailsWorkDir}/plugins/${zipName}.zip")
            }
        }
        else {
           def zipNameNoVersion = "grails-${name}"
           pluginZip = findZip(zipNameNoVersion,"${grailsSettings.grailsHome}/plugins")
           if(!pluginZip?.exists()) {
              pluginZip = findZip(zipNameNoVersion,"${grailsSettings.grailsWorkDir}/plugins")
           }
        }


        if(pluginZip?.exists()) {
           cacheLocalPlugin(pluginZip)
        }
        else {
           cacheKnownPlugin(name,version)
        }
    }
}

File findZip(String name, String dir) {
    new File(dir).listFiles().find {it.name.startsWith(name)}
}

private withPluginInstall(Closure callable) {
    try {
        fullPluginName = callable.call()
        completePluginInstall(fullPluginName)
    }
    catch (e) {
        logError("Error installing plugin: ${e.message}", e)
        exit(1)
    }
}


private completePluginInstall (fullPluginName) {
    classpath ()
    println "Installing plug-in $fullPluginName"
    installPluginForName(fullPluginName)
}


target(installPlugin:"Installs a plug-in for the given URL or name and version") {
    depends(checkVersion, parseArguments, configureProxy)
    try {
        def pluginArgs = argsMap['params']

        // fix for Windows-style path with backslashes

        if(pluginArgs) {
            if(argsMap['global']) {
                globalInstall = true
            }

            ant.mkdir(dir:pluginsBase)

            def pluginFile = new File(pluginArgs[0])
            def urlPattern = ~"^[a-zA-Z][a-zA-Z0-9\\-\\.\\+]*://"
            if(pluginArgs[0] =~ urlPattern) {
                def url = new URL(pluginArgs[0])
                doInstallPluginFromURL(url)
            }
            else if( pluginFile.exists() && pluginFile.name.startsWith("grails-") && pluginFile.name.endsWith(".zip" )) {
                doInstallPluginZip(pluginFile)
            }
            else {
                // The first argument is the plugin name, the second
                // (if provided) is the plugin version.
                doInstallPlugin(pluginArgs[0], pluginArgs[1])
            }
        }
        else {
            event("StatusError", [ ERROR_MESSAGE])
        }

    }
    catch(Exception e) {
        logError("Error installing plugin: ${e.message}", e)
        exit(1)
    }
}


target(installDefaultPluginSet:"Installs the default plugin set used by Grails") {
    for(plugin in grailsSettings.defaultPluginMap) {
        def zipName = "grails-${plugin.key}-${plugin.value}"
        def pluginZip = new File("${grailsSettings.grailsHome}/plugins/${zipName}.zip")
        if(!pluginZip.exists()) {
            pluginZip = new File("${grailsSettings.grailsWorkDir}/plugins/${zipName}.zip")
        }
        if(pluginZip.exists()) {
           doInstallPluginZip pluginZip
        }
        else {
           doInstallPlugin plugin.key, plugin.value
        }
    }    
}

target(uninstallPlugin:"Uninstalls a plug-in for a given name") {
    depends(checkVersion, parseArguments, clean)

    if(argsMap['global']) {
        globalInstall = true
    }

    def pluginArgs = argsMap['params']
    if(pluginArgs) {

        def pluginName = pluginArgs[0]
        def pluginRelease = pluginArgs[1]


        uninstallPluginForName(pluginName, pluginRelease)

        event("PluginUninstalled", ["The plugin ${pluginName}-${pluginRelease} has been uninstalled from the current application"])
    }
    else {
        event("StatusError", ["You need to specify the plug-in name and (optional) version, e.g. \"grails uninstall-plugin feeds 1.0\""])
    }

}

target(listPlugins: "Implementation target") {
    depends(parseArguments,configureProxy)    

    if(argsMap.repository) {
       configureRepositoryForName(argsMap.repository)
       updatePluginsList()
       printRemotePluginList(argsMap.repository)
       printInstalledPlugins()
    }
    else if(argsMap.installed) {
      printInstalledPlugins()
    }
    else {
      eachRepository { name, url ->
         printRemotePluginList(name)
         return true
      }
      printInstalledPlugins()
    }


    println '''
To find more info about plugin type 'grails plugin-info [NAME]'

To install type 'grails install-plugin [NAME] [VERSION]'

For further info visit http://grails.org/Plugins
'''
}

private printInstalledPlugins() {
  println '''
Plug-ins you currently have installed are listed below:
-------------------------------------------------------------
'''

  def installedPlugins = []
  def pluginXmls = readAllPluginXmlMetadata()
  for (p in pluginXmls) {
    installedPlugins << formatPluginForPrint(p.@name.text(), p.@version.text(), p.title.text())
  }

  if (installedPlugins) {
    installedPlugins.sort()
    installedPlugins.each { println it }
  }
  else {
    println "You do not have any plugins installed."
  }
}

private printRemotePluginList(name) {
  println """
Plug-ins available in the $name repository are listed below:
-------------------------------------------------------------
"""
  def plugins = []
  use(DOMCategory) {
    pluginsList.'plugin'.each {plugin ->
      def version
      def pluginLine = plugin.'@name'
      def versionLine = "<no releases>"
      def title = "No description available"
      if (plugin.'@latest-release') {
        version = plugin.'@latest-release'
        versionLine = "<${version}>"
      }
      else if (plugin.'release'.size() > 0) {
        // determine latest release by comparing version names in lexicografic order
        version = plugin.'release'[0].'@version'
        plugin.'release'.each {
          if (!"${it.'@version'}".endsWith("SNAPSHOT") && "${it.'@version'}" > version) version = "${it.'@version'}"
        }
        versionLine = "<${version} (?)>\t"
      }
      def release = plugin.'release'.find {rel -> rel.'@version' == version}
      if (release?.'title') {
        title = release?.'title'.text()
      }
      plugins << formatPluginForPrint(pluginLine, versionLine, title)
    }
  }
  // Sort plugin descriptions
  if (plugins) {
    plugins.sort()
    plugins.each {println it}
    println ""
  }
  else {
    println "No plugins found in repository: ${pluginSVN}"
  }
}

formatPluginForPrint = { pluginName, pluginVersion, pluginTitle ->
    "${pluginName.padRight(20, " ")}${pluginVersion.padRight(16, " ")} --  ${pluginTitle}"
}


def displayHeader = {
    println '''
--------------------------------------------------------------------------
Information about Grails plugin
--------------------------------------------------------------------------\
'''
}

def displayPluginInfo = { pluginName, version ->

    use(DOMCategory) {
        def plugin = findPlugin(pluginName)
        if( plugin == null ) {
            event("StatusError", ["Plugin with name '${pluginName}' was not found in the configured repositories"])
            System.exit(1)
        } else {
            def line = "Name: ${pluginName}"
            def releaseVersion = null
            if( !version ) {
                releaseVersion = plugin.'@latest-release'
                def naturalVersion = releaseVersion
                if( ! releaseVersion ) {
                    plugin.'release'.each {
                        if( !releaseVersion || (!"${it.'@version'}".endsWith("SNAPSHOT") && "${it.'@version'}" > releaseVersion )) releaseVersion = "${it.'@version'}"
                    }
                    if( releaseVersion ) naturalVersion = "${releaseVersion} (?)"
                    else naturalVersion = '<no info available>'
                }
                line += "\t| Latest release: ${naturalVersion}"
            } else {
                releaseVersion = version
                line += "\t| Release: ${releaseVersion}"
            }
            println line
            println '--------------------------------------------------------------------------'
            if( releaseVersion ) {
                def release = plugin.'release'.find{ rel -> rel.'@version' == releaseVersion }
                if( release ) {
                    if( release.'title'.text() ) {
                        println "${release.'title'.text()}"
                    } else {
                        println "No info about this plugin available"
                    }
                    println '--------------------------------------------------------------------------'
                    if( release.'author'.text() ) {
                        println "Author: ${release.'author'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                    if( release.'authorEmail'.text() ) {
                        println "Author's e-mail: ${release.'authorEmail'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                    if( release.'documentation'.text() ) {
                        println "Find more info here: ${release.'documentation'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                    if( release.'description'.text() ) {
                        println "${release.'description'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                } else {
                    println "<release ${releaseVersion} not found for this plugin>"
                    println '--------------------------------------------------------------------------'
                }
           }

            def releases = ""
            plugin.'release'.findAll{ it.'@type' == 'svn'}.each {
                releases += " ${it.'@version'}"
            }
            def zipReleases = ""
            plugin.'release'.findAll{ it.'@type' == 'zip'}.each {
                zipReleases += " ${it.'@version'}"
            }
            if( releases ) {
                println "Available full releases: ${releases}"
            } else {
                println "Available full releases: <no full releases available for this plugin now>"
            }
            if( zipReleases ) {
                println "Available zip releases:  ${zipReleases}"
            }
        }
    }
}



def displayFullPluginInfo = { pluginName ->
    use(DOMCategory) {
        pluginsList.'plugin'.each { plugin ->
            def pluginLine = plugin.'@name'
            def version = "unknown"
            def title = "No description available"
            if( plugin.'@latest-release' ) {
                version = plugin.'@latest-release'
                def release = plugin.'release'.find{ rel -> rel.'@version' == plugin.'@latest-release' }
                if( release?.'title' ) {
                    title = release?.'title'.text()
                }
            }
            pluginLine += "${spacesFormatter[pluginLine.length()..-1]}<${version}>"
            pluginLine += "\t--\t${title}"
            plugins << pluginLine
        }
    }
}

def displayFooter = {
    println '''
To get info about specific release of plugin 'grails plugin-info [NAME] [VERSION]'

To get list of all plugins type 'grails list-plugins'

To install latest version of plugin type 'grails install-plugin [NAME]'

To install specific version of plugin type 'grails install-plugin [NAME] [VERSION]'

For further info visit http://grails.org/Plugins
'''
}

target(pluginInfo:"Implementation target") {
    depends(parseArguments)

    if( argsMap.params ) {
        depends(updatePluginsList)
        displayHeader()
        def pluginName = argsMap.params[0]
        def version = argsMap.params.size() > 1 ? argsMap.params[1] : null
        displayPluginInfo( pluginName, version )
        displayFooter()
    } else {
        event("StatusError", ["Usage: grails plugin-info <plugin-name> [version]"])
    }
}
