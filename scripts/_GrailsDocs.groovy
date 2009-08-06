import grails.util.GrailsUtil
import org.radeox.engine.context.BaseInitialRenderContext
import grails.doc.DocEngine
import org.codehaus.groovy.grails.documentation.DocumentationContext
import org.codehaus.groovy.grails.documentation.DocumentedMethod
import grails.util.GrailsNameUtils

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
        // unpack documentation resources
        String docResources = "${grailsWorkDir}/doc-resources"
        ant.mkdir(dir:docResources)
        grailsUnpack(dest: docResources, src: "grails-doc-files.jar")


        refDocsDir = "${basedir}/docs/manual"
        refGuideDir = "$refDocsDir/guide"
        refPagesDir = "$refGuideDir/pages"

        ant.mkdir(dir:refDocsDir)
        ant.mkdir(dir:refGuideDir)
        ant.mkdir(dir:refPagesDir)
        ant.mkdir(dir:"$refDocsDir/ref")

        ant.mkdir(dir: "${refDocsDir}/img")
        ant.mkdir(dir: "${refDocsDir}/css")
        ant.mkdir(dir: "${refDocsDir}/ref")



        ant.copy(todir: "${refDocsDir}/img") {
            fileset(dir: "${docResources}/img")
        }
        ant.copy(todir: "${refDocsDir}/css") {
            fileset(dir: "${docResources}/css")
        }
        ant.copy(todir: "${refDocsDir}/ref") {
            fileset(dir: "${docResources}/style/ref")
        }

        title = grailsAppName
        subtitle = grailsAppName
        version = grailsAppVersion
        authors = ""
        license = ""
        copyright = ""
        footer = ""
        // if this is a plugin obtain additional metadata from the plugin
        readPluginMetadataForDocs()
        readDocProperties()

        def comparator = [compare: {o1, o2 ->
            def idx1 = o1.name[0..o1.name.indexOf(' ') - 1]
            def idx2 = o2.name[0..o2.name.indexOf(' ') - 1]
            def nums1 = idx1.split(/\./).findAll { it.trim() != ''}*.toInteger()
            def nums2 = idx2.split(/\./).findAll { it.trim() != ''}*.toInteger()
            // pad out with zeros to ensure accurate comparison
            while (nums1.size() < nums2.size()) {
                nums1 << 0
            }
            while (nums2.size() < nums1.size()) {
                nums2 << 0
            }
            def result = 0
            for (i in 0..<nums1.size()) {
                result = nums1[i].compareTo(nums2[i])
                if (result != 0) break
            }
            result
        },
         equals: { false }] as Comparator

        files = new File("${srcDocs}/guide").listFiles()?.findAll { it.name.endsWith(".gdoc") }?.sort(comparator) ?: []
        context = new BaseInitialRenderContext()
        context.set(DocEngine.CONTEXT_PATH, "..")

        engine = new DocEngine(context)
        templateEngine = new groovy.text.SimpleTemplateEngine()
        context.renderEngine = engine

        book = [:]
        for (f in files) {
            def chapter = f.name[0..-6]
            book[chapter] = f
        }

        toc = new StringBuffer()
        soloToc = new StringBuffer()
        fullContents = new StringBuffer()
        chapterContents = new StringBuffer()
        chapterTitle = null





        new File("${docResources}/style/guideItem.html").withReader {reader ->
            template = templateEngine.createTemplate(reader)

            for (entry in book) {
                //println "Generating documentation for $entry.key"
                def title = entry.key
                def level = 0
                def matcher = (title =~ /^(\S+?)\.? /) // drops last '.' of "xx.yy. "
                if (matcher.find()) {
                    level = matcher.group(1).split(/\./).size() - 1
                }
                def margin = level * 10

                if (level == 0) {
                    if (chapterTitle) // initially null, to collect sections
                        writeChapter(chapterTitle, chapterContents)

                    chapterTitle = title // after previous used to write prev chapter

                    soloToc << "<div class=\"tocItem\" style=\"margin-left:${margin}px\"><a href=\"${chapterTitle}.html\">${chapterTitle}</a></div>"
                }
                else {
                    soloToc << "<div class=\"tocItem\" style=\"margin-left:${margin}px\"><a href=\"${chapterTitle}.html#${entry.key}\">${entry.key}</a></div>"
                }        // level 0=h1, (1..n)=h2


                def hLevel = level == 0 ? 1 : 2
                def header = "<h$hLevel><a name=\"${title}\">${title}</a></h$hLevel>"

                context.set(DocEngine.SOURCE_FILE, entry.value)
                context.set(DocEngine.CONTEXT_PATH, "..")
                def body = engine.render(entry.value.text, context)

                toc << "<div class=\"tocItem\" style=\"margin-left:${margin}px\"><a href=\"#${title}\">${title}</a></div>"
                fullContents << header << body
                chapterContents << header << body

                new File("${refPagesDir}/${title}.html").withWriter {
                    template.make(title: title, content: body).writeTo(it)
                }
            }
        }
        if (chapterTitle) // write final chapter collected (if any seen)
            writeChapter(chapterTitle, chapterContents)



        vars = [
                title: title,
                subtitle: subtitle,
                footer: footer, // TODO - add a way to specify footer
                authors: authors,
                version: version,
                copyright: copyright,

                toc: toc.toString(),
                body: fullContents.toString()
        ]

        new File("${docResources}/style/layout.html").withReader {reader ->
            template = templateEngine.createTemplate(reader)
            new File("${refGuideDir}/single.html").withWriter {out ->
                template.make(vars).writeTo(out)
            }
            vars.toc = soloToc
            vars.body = ""
            new File("${refGuideDir}/index.html").withWriter {out ->
                template.make(vars).writeTo(out)
            }
        }

        new File("${docResources}/style/index.html").withReader {reader ->
            template = templateEngine.createTemplate(reader)
            new File("${refDocsDir}/index.html").withWriter {out ->
                template.make(vars).writeTo(out)
            }
        }


        menu = new StringBuilder()
        files = new File("${srcDocs}/ref").listFiles()?.toList()?.sort() ?: []
        reference = [:]
        new File("${docResources}/style/referenceItem.html").withReader {reader ->
            template = templateEngine.createTemplate(reader)
            for (f in files) {
                if (f.directory && !f.name.startsWith(".")) {
                    def section = f.name
                    reference."${section}" = [:]
                    menu << "<h1 class=\"menuTitle\">${f.name}</h1>"
                    new File("${refDocsDir}/ref/${f.name}").mkdirs()
                    def textiles = f.listFiles().findAll { it.name.endsWith(".gdoc")}.sort()
                    def usageFile = new File("${basedir}/src/ref/${f.name}.gdoc")
                    if (usageFile.exists()) {
                        def data = usageFile.text
                        reference."${section}".usage = data
                        context.set(DocEngine.SOURCE_FILE, usageFile.name)
                        context.set(DocEngine.CONTEXT_PATH, "../..")
                        def contents = engine.render(data, context)
                        new File("${refDocsDir}/ref/${f.name}/Usage.html").withWriter {out ->
                            template.make(content: contents).writeTo(out)
                        }
                        menu << "<div class=\"menuUsageItem\"><a href=\"${f.name}/Usage.html\" target=\"mainFrame\">Usage</a></div>"
                    }
                    for (txt in textiles) {
                        def name = txt.name[0..-6]
                        menu << "<div class=\"menuItem\"><a href=\"${f.name}/${name}.html\" target=\"mainFrame\">${name}</a></div>"
                        def data = txt.text
                        reference."${section}".put(name,data)
                        context.set(DocEngine.SOURCE_FILE, txt.name)
                        context.set(DocEngine.CONTEXT_PATH, "../..")
                        def contents = engine.render(data, context)
                        //println "Generating reference item: ${name}"
                        new File("${refDocsDir}/ref/${f.name}/${name}.html").withWriter {out ->
                            template.make(content: contents).writeTo(out)
                        }
                    }
                }
            }

        }
        vars.menu = menu
        new File("${docResources}/style/menu.html").withReader {reader ->
            template = templateEngine.createTemplate(reader)
            new File("${refDocsDir}/ref/menu.html").withWriter {out ->
                template.make(vars).writeTo(out)
            }
        }




        println "Built user manual at ${refDocsDir}/index.html"        
    }

}


void writeChapter(String title, StringBuffer content) {
        new File("${refGuideDir}/${title}.html").withWriter {
            template.make(title: title, content: content.toString()).writeTo(it)
        }
        content.delete(0, content.size()) // clear buffer
    }


def readPluginMetadataForDocs() {
    def basePlugin = loadBasePlugin()
    if (basePlugin) {
        if (basePlugin.hasProperty("title"))
            title = basePlugin.title
        if (basePlugin.hasProperty("description"))
            subtitle = basePlugin.description
        if (basePlugin.hasProperty("version"))
            version = basePlugin.version
        if (basePlugin.hasProperty("license"))
            license = basePlugin.license
        if (basePlugin.hasProperty("author"))
            authors = basePlugin.author
    }
}

def readDocProperties() {
    readIfSet("copyright")
    readIfSet("license")
    readIfSet("authors")
    readIfSet("footer")

}
private readIfSet(String prop) {
    if(config.grails.doc."$prop") { 
        binding[prop] = config.grails.doc."$prop"
    }

}
private def loadBasePlugin() {
		pluginManager?.allPlugins?.find { it.basePlugin }
}