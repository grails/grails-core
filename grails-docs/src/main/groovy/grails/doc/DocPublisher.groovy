/* Copyright 2004-2005 the original author or authors.
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

package grails.doc

import grails.doc.internal.StringEscapeCategory
import groovy.text.Template

import org.radeox.engine.context.BaseInitialRenderContext

/**
 * Coordinated the DocEngine the produce documentation based on the gdoc format.
 *
 * @see DocEngine
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class DocPublisher {

    /** The source directory of the documentation */
    File src
    /** The target directory to publish to */
    File target
    /** The temporary work directory */
    File workDir
    /** The directory containing any images to use (will override defaults) **/
    File images
    /** The directory containing any CSS to use (will override defaults) **/
    File css
    /** The directory containing any Javascript to use (will override defaults) **/
    File js
    /** The directory cotnaining any templates to use (will override defaults) **/
    File style
    /** The AntBuilder instance to use */
    AntBuilder ant
    /** The encoding to use (default is UTF-8) */
    String encoding = "UTF-8"
    /** The title of the documentation */
    String title
    /** The subtitle of the documentation */
    String subtitle = ""
    /** The version of the documentation */
    String version
    /** The authors of the documentation */
    String authors = ""
    /** The documentation license */
    String license = ""
    /** The copyright message */
    String copyright = ""
    /** The footer to include */
    String footer = ""
    /** HTML markup that renders the left logo */
    String logo
    /** HTML markup that renders the right logo */
    String sponsorLogo

    /** Properties used to configure the DocEngine */
    Properties engineProperties

    private context
    private engine
    private customMacros = []

    DocPublisher() {
        this(null, null)
    }

    DocPublisher(File src, File target) {
        this.src = src
        this.target = target

        try {
            engineProperties.load(getClass().classLoader.getResourceAsStream("grails/doc/doc.properties"))
        }
        catch (e) {
            // ignore
        }
    }

    /** Returns the engine properties. */
    Properties getEngineProperties() { engineProperties }

    /** Sets the engine properties. Allows clients to override the defaults. */
    void setEngineProperties(Properties p) {
        engineProperties = p
    }

    /**
     * Registers a custom Radeox macro. If the macro has an 'initialContext'
     * property, it is set to the render context before first use.
     */
    void registerMacro(macro) {
        customMacros << macro
    }

    void publish() {
        use(StringEscapeCategory) {
            catPublish()
        }
    }

    private void catPublish() {
        initialize()
        if (!src?.exists()) {
            return
        }

        // unpack documentation resources
        String docResources = "${workDir}/doc-resources"
        ant.mkdir(dir: docResources)
        unpack(dest: docResources, src: "grails-doc-files.jar")

        def refDocsDir = target?.absolutePath ?: "./docs"
        def refGuideDir = "$refDocsDir/guide"
        def refPagesDir = "$refGuideDir/pages"

        ant.mkdir(dir: refDocsDir)
        ant.mkdir(dir: refGuideDir)
        ant.mkdir(dir: refPagesDir)
        ant.mkdir(dir: "$refDocsDir/ref")

        String imgsDir = "${refDocsDir}/img"
        ant.mkdir(dir: imgsDir)
        String cssDir = "${refDocsDir}/css"
        ant.mkdir(dir: cssDir)
        String jsDir = "${refDocsDir}/js"
        ant.mkdir(dir: jsDir)
        ant.mkdir(dir: "${refDocsDir}/ref")

        ant.copy(todir: imgsDir) {
            fileset(dir: "${docResources}/img")
        }

        if (images && images.exists()) {
            ant.copy(todir: imgsDir, overwrite: true, failonerror:false) {
                fileset(dir: images)
            }
        }
        ant.copy(todir: cssDir) {
            fileset(dir: "${docResources}/css")
        }
        if (css && css.exists()) {
            ant.copy(todir: cssDir, overwrite: true, failonerror:false) {
                fileset(dir: css)
            }
        }
        ant.copy(todir: jsDir) {
            fileset(dir: "${docResources}/js")
        }
        if (js && js.exists()) {
            ant.copy(todir: jsDir, overwrite: true, failonerror:false) {
                fileset(dir: js)
            }
        }
        if (style && style.exists()) {
            ant.copy(todir: "${docResources}/style", overwrite: true, failonerror:false) {
                fileset(dir: style)
            }
        }

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

        def files = new File("${src}/guide").listFiles()?.findAll { it.name.endsWith(".gdoc") }?.sort(comparator) ?: []
        def templateEngine = new groovy.text.SimpleTemplateEngine()

        // A tree of book sections, where 'book' is a list of the top-level
        // sections and each of those has a list of sub-sections and so on.
        def book = []
        for (f in files) {
            // Chapter is filename - '.gdoc' suffix.
            def chapter = f.name[0..-6]
            def section = new Expando(title: chapter, file: f, subSections: [])

            def level = 0
            def matcher = (chapter =~ /^(\S+?)\.?\s/) // drops last '.' of "xx.yy. "
            if (matcher) {
                level = matcher.group(1).split(/\./).size() - 1
            }

            // This cryptic line finds the appropriate parent section list based
            // on the current section's level. If the level is 0, then it's 'book'.
            def parent = (0..<level).inject(book) { sectionList, n -> sectionList[-1].subSections }
            parent << section
        }

        // Reference menu items.
        def sectionFilter = { it.directory && !it.name.startsWith('.') } as FileFilter
        files = new File("${src}/ref").listFiles(sectionFilter)?.toList()?.sort() ?: []
        def refCategories = files.collect { f ->
            new Expando(
                    name: f.name,
                    usage: new File("${src}/ref/${f.name}.gdoc"),
                    sections: f.listFiles().findAll { it.name.endsWith(".gdoc") }.sort())
        }

        def fullToc = new StringBuilder()

        def pathToRoot = ".."
        def vars = [
            encoding: encoding,
            title: title,
            subtitle: subtitle,
            footer: footer, // TODO - add a way to specify footer
            authors: authors,
            version: version,
            refMenu: refCategories,
            toc: book,
            copyright: copyright,
            logo: injectPath(logo, pathToRoot),
            sponsorLogo: injectPath(sponsorLogo, pathToRoot),
            single: false,
            path: pathToRoot,
            prev: null,
            next: null
        ]

        // Build the user guide sections first.
        def template = templateEngine.createTemplate(new File("${docResources}/style/guideItem.html").newReader(encoding))
        def sectionTemplate = templateEngine.createTemplate(new File("${docResources}/style/section.html").newReader(encoding))
        def fullContents = new StringBuilder()

        def chapterVars
        book.eachWithIndex{ chapter, i ->
            chapterVars = [*:vars]
            if (i != 0) {
                chapterVars['prev'] = book[i - 1]
            }
            if (i != (book.size() - 1)) {
                chapterVars['next'] = book[i + 1]
            }
            writeChapter(chapter, template, sectionTemplate, refGuideDir, fullContents, chapterVars)
        }

        files = new File("${src}/ref").listFiles()?.toList()?.sort() ?: []
        def reference = [:]
        template = templateEngine.createTemplate(new File("${docResources}/style/referenceItem.html").newReader(encoding))

        pathToRoot = "../.."
        vars.logo = injectPath(logo, pathToRoot)
        vars.sponsorLogo = injectPath(sponsorLogo, pathToRoot)
        vars.path = pathToRoot

        for (f in files) {
            if (f.directory && !f.name.startsWith(".")) {
                def section = f.name
                vars.section = section

                new File("${refDocsDir}/ref/${section}").mkdirs()
                def textiles = f.listFiles().findAll { it.name.endsWith(".gdoc")}.sort()
                def usageFile = new File("${src}/ref/${section}.gdoc")
                if (usageFile.exists()) {
                    def data = usageFile.text
                    context.set(DocEngine.SOURCE_FILE, usageFile)
                    context.set(DocEngine.CONTEXT_PATH, pathToRoot)
                    vars.content = engine.render(data, context)

                    new File("${refDocsDir}/ref/${section}/Usage.html").withWriter(encoding) {out ->
                        template.make(vars).writeTo(out)
                    }
                }
                for (txt in textiles) {
                    def name = txt.name[0..-6]
                    def data = txt.text
                    context.set(DocEngine.SOURCE_FILE, txt.name)
                    context.set(DocEngine.CONTEXT_PATH, pathToRoot)
                    vars.content = engine.render(data, context)

                    new File("${refDocsDir}/ref/${section}/${name}.html").withWriter(encoding) {out ->
                        template.make(vars).writeTo(out)
                    }
                }
            }
        }

        vars.remove("section")
        vars.content = fullContents.toString()
        vars.single = true

        pathToRoot = ".."
        vars.logo = injectPath(logo, pathToRoot)
        vars.sponsorLogo = injectPath(sponsorLogo, pathToRoot)
        vars.path = pathToRoot

        template = templateEngine.createTemplate(new File("${docResources}/style/layout.html").newReader(encoding))
        new File("${refGuideDir}/single.html").withWriter(encoding) {out ->
            template.make(vars).writeTo(out)
        }

        vars.content = ""
        vars.single = false
        new File("${refGuideDir}/index.html").withWriter(encoding) {out ->
            template.make(vars).writeTo(out)
        }

        pathToRoot = "."
        vars.logo = injectPath(logo, pathToRoot)
        vars.sponsorLogo = injectPath(sponsorLogo, pathToRoot)
        vars.path = pathToRoot

        new File("${refDocsDir}/index.html").withWriter(encoding) {out ->
            template.make(vars).writeTo(out)
        }

        ant.echo "Built user manual at ${refDocsDir}/index.html"
    }

    void writeChapter(section, Template layoutTemplate, Template sectionTemplate, String targetDir, fullContents, vars) {
        fullContents << writePage(section, layoutTemplate, sectionTemplate, targetDir, "", "..", 0, vars)
    }

    String writePage(section, Template layoutTemplate, Template sectionTemplate, String targetDir, String subDir, path, level, vars) {
        context.set(DocEngine.SOURCE_FILE, section.file)
        context.set(DocEngine.CONTEXT_PATH, path)

        def varsCopy = [*:vars]
        varsCopy.title = section.title
        varsCopy.path = path
        varsCopy.level = level
        varsCopy.sectionToc = section.subSections
        varsCopy.content = engine.render(section.file.text, context)

        // First create the section content, which usually consists of a header
        // and the translated gdoc content.
        def sectionContent = new StringWriter()
        sectionTemplate.make(varsCopy).writeTo(sectionContent)

        // Aggregate the section content and sub-sections.
        def accumulatedContent = new StringBuilder()
        accumulatedContent << sectionContent.toString()

        // Create the sub-section pages.
        level++
        for (s in section.subSections) {
            accumulatedContent << writePage(s, layoutTemplate, sectionTemplate, targetDir, "pages", path, level, vars)
        }

        // TODO PAL - I don't see why these pages are necessary, plus there seems
        // to be no way to get embedded images to display properly (since the path
        // passed to the Wiki rendering engine is wrong for pages written to a
        // 'pages' subdirectory). Keeping them in case someone, somewhere depends
        // on them.
        //
        // Create the HTML page for this section, which includes the content
        // from all the sub-sections too.
        if (subDir) {
            if (subDir.endsWith('/')) subDir = subDir[0..-2]
            targetDir = "$targetDir/$subDir"

            varsCopy.path = "../${path}"
            varsCopy.logo = injectPath(logo, varsCopy.path)
            varsCopy.sponsorLogo = injectPath(sponsorLogo, varsCopy.path)
        }

        new File("${targetDir}/${section.title}.html").withWriter(encoding) { writer ->
            varsCopy.content = accumulatedContent.toString()
            layoutTemplate.make(varsCopy).writeTo(writer)
        }

        return varsCopy.content
    }

    protected void initialize() {
        if (!workDir) {
            workDir = new File(System.getProperty("java.io.tmpdir"))
        }
        if (!ant) {
            ant = new AntBuilder()
        }
        def metaProps = DocPublisher.metaClass.properties
        def props = engineProperties
        for (MetaProperty mp in metaProps) {
            if (mp.type == String) {
                def value = props[mp.name]
                if (value) {
                    this[mp.name] = value
                }
            }
        }

        context = new BaseInitialRenderContext()
        context.set(DocEngine.CONTEXT_PATH, "..")
        context.set(DocEngine.BASE_DIR, src.absolutePath)
        context.set(DocEngine.API_BASE_PATH, target.absolutePath)

        engine = new DocEngine(context)
        engine.engineProperties = engineProperties
        context.renderEngine = engine

        // Add any custom macros registered with this publisher to the engine.
        for (m in customMacros) {
            if (m.metaClass.hasProperty(m, "initialContext")) {
                m.initialContext = context
            }
            engine.addMacro(m)
        }
    }

    private String injectPath(String source, String path) {
        if (!source) return source

        def templateEngine = new groovy.text.SimpleTemplateEngine()
        def out = new StringWriter()
        templateEngine.createTemplate(source).make(path: path).writeTo(out)
        return out.toString()
    }

    private unpack(Map args) {

        def dir = args["dest"] ?: "."
        def src = args["src"]
        def overwriteOption = args["overwrite"] == null ? true : args["overwrite"]

        // Can't unjar a file from within a JAR, so we copy it to
        // the destination directory first.
        try {
            URL url = getClass().getClassLoader().getResource(src)
            if (url) {
                url.withInputStream { InputStream input ->
                    new File("$dir/$src").withOutputStream { out ->
                        def buffer = new byte[1024]
                        int len
                        while ((len = input.read(buffer)) != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                }
            }
            // Now unjar it, excluding the META-INF directory.
            ant.unjar(dest: dir, src: "${dir}/${src}", overwrite: overwriteOption) {
                patternset {
                    exclude(name: "META-INF/**")
                }
            }
        }
        finally {
            // Don't need the JAR file any more, so remove it.
            ant.delete(file: "${dir}/${src}", failonerror:false)
        }
    }
}
