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

package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.springframework.web.context.ServletContextAware
import org.codehaus.groovy.grails.commons.GrailsApplication
import javax.servlet.ServletContext
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.mxp1.MXParser
import org.xml.sax.InputSource
import javax.xml.parsers.SAXParserFactory
import org.springframework.util.Assert
import org.springframework.core.io.Resource
import org.springframework.web.context.support.ServletContextResource
import org.springframework.core.io.FileSystemResource
import grails.util.BuildSettingsHolder


/**
 * A class that resolves all of the available tag libraries from web.xml and all available JAR files.
 *
 * Kudos to the Freemarker (http://freemarker.sourceforge.net/) library for providing the inspiration for this code.
 *
 * @author Graeme Rocher
 */
class TagLibraryResolver implements ServletContextAware, GrailsApplicationAware{

    GrailsApplication grailsApplication
    ServletContext servletContext
    private tagLibs = [:]
    private tagLibLocations = [:]

    /**
     * Resolves a JspTagLib instance for the given URI
     **/
    JspTagLib resolveTagLibrary(String uri) {
        if(tagLibs[uri]) return tagLibs[uri]

        JspTagLib jspTagLib = null

        String loc = tagLibLocations[uri]

        if(loc) {
            if(loc.startsWith("jar:")) {
                def jarURLs = grailsApplication.isWarDeployed() ? getJarsFromServletContext() : resolveRootLoader().getURLs()
                def fileLoc = loc[4..loc.indexOf('!')-1]
                String pathWithinZip = loc[loc.indexOf('!')+1..-1]
                URL jarFile = jarURLs.find { it.toExternalForm() == fileLoc}
                if(jarFile) {
                    ZipInputStream zipInput = new ZipInputStream(jarFile.openStream())
                    ZipEntry entry = zipInput.getNextEntry()
                    while(entry) {
                        if(entry.name == pathWithinZip) {
                            jspTagLib = loadJspTagLib(uri, loc, new InputStreamReader(zipInput))
                            break
                        }
                        entry = zipInput.getNextEntry()
                    }
                }
            }
            else {
                jspTagLib = loadJspTagLib(uri, loc, new InputStreamReader(getTldFromServletContext(loc)))                
            }
        }
        else {
            Assert.notNull servletContext, "TagLibraryResolver requires an instance of the ServletContext!"
            Resource webXml = getWebXmlFromServletContext()

            if(webXml?.exists()) {
                loadTagLibLocations(webXml)
            }

            if(tagLibLocations[uri]) {
                // in this case the tag lib was discovered in the web.xml so we use the servlet context
                loc = tagLibLocations[uri]
                jspTagLib = loadJspTagLib(uri, loc, new InputStreamReader(getTldFromServletContext(loc)))
            }
            else {
                def jarURLs = grailsApplication.isWarDeployed() ? getJarsFromServletContext() : (resolveRootLoader().getURLs())
                for(url in jarURLs) {
                    if(url.file.endsWith(".jar")) {
                        jspTagLib = attempLoadTagLibFromJAR(uri, url, new ZipInputStream(url.openStream()))
                        if(jspTagLib) break
                    }
                }
            }
        }
        return jspTagLib
    }

    private loadTagLibLocations(Resource webXml) {
        if(webXml) {

            def source = new InputSource(webXml.getInputStream())

            SAXParserFactory factory = SAXParserFactory.newInstance()
            factory.namespaceAware = false
            factory.validating = false
            def reader = factory.newSAXParser().getXMLReader()
            WebXmlTagLibraryReader webXmlReader = new WebXmlTagLibraryReader()
            reader.setContentHandler webXmlReader
            reader.setEntityResolver new LocalEntityResolver()
            reader.parse source

            for(entry in webXmlReader.getTagLocations()) {
                tagLibLocations[entry.key] = entry.value
            }
        }
    }
    protected InputStream getTldFromServletContext(String loc) {
        servletContext.getResourceAsStream(loc)
    }
    protected Resource getWebXmlFromServletContext() {
        if(grailsApplication.isWarDeployed()) {
            return new ServletContextResource(servletContext, "/WEB-INF/web.xml")
        }
        else {
            def projectResourcesDir = BuildSettingsHolder.settings?.resourcesDir
            if(projectResourcesDir)
                return new FileSystemResource("${projectResourcesDir.path}/web.xml")
        }
    }
    protected List getJarsFromServletContext() {
        def files = servletContext.getResourcePaths("/WEB-INF/lib")
        files = files.findAll {  it.endsWith(".jar") || it.endsWith(".zip")}
        files.collect { servletContext.getResource(it) }
    }

    private JspTagLib attempLoadTagLibFromJAR(String uri, URL jarURL, ZipInputStream zipInput) {
        JspTagLib jspTagLib

        try {
            zipInput = new ZipInputStream(jarURL.openStream())
            ZipEntry entry = zipInput.getNextEntry()
            while (entry) {
                def name = entry.getName()

                if (name.startsWith("META-INF/") && name.endsWith(".tld")) {
                    def tagLibLocation = "jar:${jarURL.toExternalForm()}!$name"
                    def inputStreamReader = new BufferedReader(new InputStreamReader(new UncloseableInputStream(zipInput)))
                    inputStreamReader.mark 1024
                    XmlPullParser pullParser = new MXParser()
                    pullParser.setInput(inputStreamReader)

                    pullParser.nextTag()
                    if ("taglib".equals(pullParser.getName())) {
                        def tagLibURI
                        int token
                        while (true) {
                            token = pullParser.nextToken()
                            if (token == XmlPullParser.END_DOCUMENT) break
                            if (token == XmlPullParser.START_TAG && "uri".equals(pullParser.getName())) {
                                pullParser.next()
                                tagLibURI = pullParser.getText()?.trim()
                                break

                            }
                        }
                        if (tagLibURI) {

                            tagLibLocations[tagLibURI] = tagLibLocation
                            if (tagLibURI == uri) {
                                inputStreamReader.reset()
                                jspTagLib = loadJspTagLib(uri, tagLibLocation, inputStreamReader)
                            }
                        }
                    }
                }
                entry = zipInput.getNextEntry()
            }

        } finally {
            zipInput?.close()
        }
        return  jspTagLib
    }

    /**
     * Obtains a reference to the RootLoader instance
     */
    protected resolveRootLoader() {
         getClass().classLoader.rootLoader
    }

    private JspTagLib loadJspTagLib(String uri, String loc, Reader r) {

        def source = new InputSource(r)
        source.setSystemId loc

        SAXParserFactory factory = SAXParserFactory.newInstance()
        factory.namespaceAware = false
        factory.validating = false
        def reader = factory.newSAXParser().getXMLReader()
        TldReader tldReader = new TldReader()
        reader.setContentHandler tldReader
        reader.setEntityResolver new LocalEntityResolver()
        reader.parse source


        def taglib = new JspTagLibImpl(uri, tldReader.tags)        
        tagLibs[uri] = taglib
        return taglib
    }

}