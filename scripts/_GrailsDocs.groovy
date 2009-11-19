import grails.util.GrailsUtil
import org.radeox.engine.context.BaseInitialRenderContext
import grails.doc.DocEngine
import org.codehaus.groovy.grails.documentation.DocumentationContext
import org.codehaus.groovy.grails.documentation.DocumentedMethod
import grails.util.GrailsNameUtils
import grails.doc.DocPublisher

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
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Sep 20, 2007
 */

includeTargets << grailsScript("_GrailsPackage")

javadocDir = "${basedir}/docs/api"
groovydocDir = "${basedir}/docs/gapi"
docEncoding = "UTF-8"
docSourceLevel = "1.5"
links = [
            'http://java.sun.com/j2se/1.5.0/docs/api/'
        ]

target(docs: "Produces documentation for a Grails project") {
    depends(compile, javadoc, groovydoc, refdocs)
}

target(setupDoc:"Sets up the doc directories") {
    ant.mkdir(dir:"${basedir}/docs")
    ant.mkdir(dir:groovydocDir)
    ant.mkdir(dir:javadocDir)
}

target(groovydoc:"Produces groovydoc documentation") {
    ant.taskdef(name:"groovydoc", classname:"org.codehaus.groovy.ant.Groovydoc")
    event("DocStart", ['groovydoc'])
    try {
        ant.groovydoc(destdir:groovydocDir, sourcepath:".", use:"true", windowtitle:grailsAppName,'private':"true")
    }
    catch(Exception e) {
       event("StatusError", ["Error generating groovydoc: ${e.message}"])
    }
    event("DocEnd", ['groovydoc'])
}

target(javadoc:"Produces javadoc documentation") {
   depends(setupDoc)
    event("DocStart", ['javadoc'])
    File javaDir = new File("${basedir}/src/java")
    if(javaDir.listFiles().find{ !it.name.startsWith(".")}) {
       try {
           ant.javadoc( access:"protected",
                        destdir:javadocDir,
                        encoding:docEncoding,
                        classpathref:"grails.compile.classpath",
                        use:"yes",
                        windowtitle:grailsAppName,
                        docencoding:docEncoding,
                        charset:docEncoding,
                        source:docSourceLevel,
                        useexternalfile:"yes",
                        breakiterator:"true",
                        linksource:"yes",
                        maxmemory:"128m",
                        failonerror:false,
                        sourcepath:javaDir.absolutePath) {
               for(i in links) {
                   link(href:i)
               }
           }
       }
       catch(Exception e) {
          event("StatusError", ["Error generating javadoc: ${e.message}"])
          // ignore, empty src/java directory 
       }
   }
    event("DocEnd", ['javadoc'])

}

target(refdocs:"Generates Grails style reference documentation") {
    depends(createConfig,loadPlugins)
    
    def srcDocs = new File("${basedir}/src/docs")



    def context = DocumentationContext.getInstance()
    if(context?.hasMetadata()) {
        for(DocumentedMethod m in context.methods) {
            if(m.artefact && m.artefact != 'Unknown') {
                String refDir = "${srcDocs}/ref/${GrailsNameUtils.getNaturalName(m.artefact)}"
                ant.mkdir(dir:refDir)
                def refFile = new File("${refDir}/${m.name}.gdoc")
                if(!refFile.exists()) {
                    println "Generating documentation ${refFile}"
                    refFile.write """
h1. ${m.name}

h2. Purpose

${m.text ?: ''}

h2. Examples

{code:java}
foo.${m.name}(${m.arguments?.collect {GrailsNameUtils.getPropertyName(it)}.join(',')})
{code}

h2. Description

${m.text ?: ''}

Arguments:

${m.arguments?.collect { '* @'+GrailsNameUtils.getPropertyName(it)+'@\n' }}
"""
                }
            }
        }
    }
    if(srcDocs.exists()) {
        File refDocsDir = new File("${basedir}/docs/manual")
        def publisher = new DocPublisher(srcDocs, refDocsDir)
        publisher.ant = ant
        publisher.title = grailsAppName
        publisher.subtitle = grailsAppName
        publisher.version = grailsAppVersion
        publisher.authors = ""
        publisher.license = ""
        publisher.copyright = ""
        publisher.footer = ""
        publisher.engineProperties = config?.grails?.doc
        // if this is a plugin obtain additional metadata from the plugin
        readPluginMetadataForDocs(publisher)
        readDocProperties(publisher)


        publisher.publish()


        println "Built user manual at ${refDocsDir}/index.html"        
    }

}



def readPluginMetadataForDocs(DocPublisher publisher) {
    def basePlugin = loadBasePlugin()
    if (basePlugin) {
        if (basePlugin.hasProperty("title"))
            publisher.title = basePlugin.title
        if (basePlugin.hasProperty("description"))
            publisher.subtitle = basePlugin.description
        if (basePlugin.hasProperty("version"))
            publisher.version = basePlugin.version
        if (basePlugin.hasProperty("license"))
            publisher.license = basePlugin.license
        if (basePlugin.hasProperty("author"))
            publisher.authors = basePlugin.author
    }
}

def readDocProperties(DocPublisher publisher) {
    readIfSet(publisher,"copyright")
    readIfSet(publisher,"license")
    readIfSet(publisher,"authors")
    readIfSet(publisher,"footer")

}
private readIfSet(DocPublisher publisher,String prop) {
    if(config.grails.doc."$prop") { 
        publisher[prop] = config.grails.doc."$prop"
    }

}
private def loadBasePlugin() {
		pluginManager?.allPlugins?.find { it.basePlugin }
}