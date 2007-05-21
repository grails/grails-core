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
 * Gant script that handles the creation of Grails plugins
 * 
 * @author Sergey Nebolsin
 *
 * @since 0.5.5
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU  
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.apache.xml.serialize.XMLSerializer
import org.apache.xml.serialize.OutputFormat

DEFAULT_PLUGIN_DIST = "http://plugins.grails.org"
BINARY_PLUGIN_DIST = "http://plugins.grails.org/dist"

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

pluginsHome = "${userHome}/.grails/plugins/"
Ant.mkdir(dir:"${pluginsHome}")
pluginsListFile = new File("${pluginsHome}/plugins-list.xml")


def recreateCache = false
if( !pluginsListFile.exists() ) {
    recreateCache = true
}
try {
    document = DOMBuilder.parse(new FileReader(pluginsListFile))
} catch( Exception e ) {
    recreateCache = true
}
if( recreateCache ) {
    event("StatusUpdate", ["Plugins list cache does not exist or is broken, recreating"])
    document = DOMBuilder.newInstance().createDocument()
    def root = document.createElement('plugins')
    root.setAttribute('revision','0')
    document.appendChild(root)
}

pluginsList = document.documentElement
builder = new DOMBuilder(document)

def indentingOutputFormat = new OutputFormat("XML","UTF-8",true)

def writePluginsFile = {
    XMLSerializer serializer = new XMLSerializer( new FileWriter(pluginsListFile), indentingOutputFormat );
    serializer.serialize(document)
}

def parseRemoteXML = { url ->
    DOMBuilder.parse(new URL(url).openStream().newReader())
}

task ( "default" : "Lists plug-ins that are hosted by the Grails server") {
    listPlugins()
}

task(listPlugins:"Implementation task") {
    depends(updatePluginsList)
    println '''
Plug-ins available in the Grails repository are listed below:
-------------------------------------------------------------
'''
    def spacesFormatter = "                    "
    def plugins = []
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
    // Sort plugin descriptions
    plugins.sort()
    plugins.each { println it }
    println '''
To find more info about plugin type 'grails plugin-info [NAME]'

To install type 'grails install-plugin [NAME] [VERSION]'

For further info visit http://grails.org/Plugins
'''
}

def buildReleaseInfo = { root, pluginName, releaseTag ->
    try {
        def properties = ['title','author','authorEmail','description']
        def releaseDescriptor = parseRemoteXML("${DEFAULT_PLUGIN_DIST}/grails-${pluginName}/tags/${releaseTag}/plugin.xml").documentElement
        def version = releaseDescriptor.'@version'
        def releaseNode = builder.createNode('release',[tag:releaseTag,version:version,type:'svn'])
        root.appendChild(releaseNode)
        properties.each {
            if( releaseDescriptor."${it}") {
                releaseNode.appendChild(builder.createNode(it,releaseDescriptor."${it}".text()))
            }
        }
        releaseNode.appendChild(builder.createNode('file',"${DEFAULT_PLUGIN_DIST}/grails-${pluginName}/tags/${releaseTag}/grails-${pluginName}-${version}.zip"))
    } catch( Exception e ) {
        // no plugin release info available
    }
}

def buildPluginInfo = {root, pluginName ->
    use( DOMCategory ) {
        def pluginNode = root.'plugin'.find { it.'@name' == pluginName }
        if( !pluginNode ) {
            pluginNode = builder.'plugin'(name:pluginName)
            root.appendChild(pluginNode)
        }
        def latestVersion = null
        try {
            latestVersion = parseRemoteXML("${DEFAULT_PLUGIN_DIST}/grails-${pluginName}/tags/LATEST_RELEASE/plugin.xml").documentElement.'@version'
        } catch( Exception e ) {
            // latest release version is not available
        }
        if( latestVersion ) pluginNode.setAttribute('latest-release',latestVersion as String)
        try {
            def releaseTagsList = new URL("${DEFAULT_PLUGIN_DIST}/grails-${pluginName}/tags/").text
            releaseTagsList.eachMatch( /<li><a href="(.+?)">/ ) {
                def releaseTag = it[1][0..-2]
                if( releaseTag != '..' && releaseTag != 'LATEST_RELEASE' && ! pluginNode.'release'.find{ it.'@tag' == releaseTag }) {
                    buildReleaseInfo(pluginNode, pluginName, releaseTag)
                }
            }
        } catch( Exception e ) {
            // no plugin release info available
        }
    }
}

def buildBinaryPluginInfo = {root, pluginName ->
    def matcher = (pluginName =~ /^([^\d]+)-(.++)/)
    def name = GCU.getScriptName( matcher[0][1] )
    def release = matcher[0][2]
    use( DOMCategory ) {
        def pluginNode = root.'plugin'.find { it.'@name' == name }
        if( !pluginNode ) {
            pluginNode = builder.'plugin'(name:name)
            root.appendChild(pluginNode)
        }
        def releaseNode = pluginNode.'release'.find { it.'@version' == release }
        if( !releaseNode ) {
            releaseNode = builder.'release'(type:'zip',version:release) {
                title("This is a zip release, no info available for it")
                file("${BINARY_PLUGIN_DIST}/grails-${pluginName}.zip")
            }
            pluginNode.appendChild(releaseNode)
        }
    }
}

task(updatePluginsList:"Implementation tast. Updates list of plugins hosted at Grails site if needed") {
    depends(configureProxy)
    try {
        def localRevision = pluginsList ? new Integer(pluginsList.getAttribute('revision')) : -1
        def remotePluginsList = new URL(DEFAULT_PLUGIN_DIST).text
        def remoteRevision = 0
        remotePluginsList.eachMatch( /Revision (.*):/ ) {
            remoteRevision = new Integer(it[1])
        }
        if( remoteRevision > localRevision ) {
            // Plugins list cache is expired, update needed
            event("StatusUpdate", ["Plugins list cache is expired, updating"])
            pluginsList.setAttribute('revision',remoteRevision as String)
            remotePluginsList.eachMatch( /<li><a href="grails-(.+?)">/ ) {
                def pluginName = it[1][0..-2]
                buildPluginInfo(pluginsList, pluginName)
            }
        }
        def binaryPluginsList = new URL(BINARY_PLUGIN_DIST).text
        binaryPluginsList.eachMatch(/<a href="grails-(.+?).zip">/) {
            buildBinaryPluginInfo(pluginsList, it[1])
        }
        writePluginsFile()
    } catch (Exception e) {
        event("StatusError", ["Unable to list plug-ins, please check you have a valid internet connection"])
    }
}
