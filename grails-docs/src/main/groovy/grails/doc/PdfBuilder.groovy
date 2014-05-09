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

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import org.jsoup.Jsoup
import org.w3c.dom.Document
import org.xhtmlrenderer.pdf.ITextRenderer

class PdfBuilder {

    private static final String LIVE_DOC_SITE = 'http://grails.org'

    static void build(String baseDir, String styleDir = null) {
        build basedir: baseDir
    }

    /**
     * Builds a PDF file from the manual's single.html file.<p>
     * The following directories are assumed to exist:<ul>
     * <li> $basedir/guide/single.html</li>
     * <li> $basedir/guide/css/</li>
     * <li> $basedir/guide/img/</li>
     * </ul>
     *
     * The {@code options} map should have the following key/value pairs<ul>
     * <li>basedir = points to the root directory that contains the generated manual <b>required</b></li>
     * </ul>
     */
    static void build(Map options) {
        File baseDir = new File(options.basedir).canonicalFile

        File guideDir = new File(baseDir, "guide")
        File htmlFile = new File(guideDir, "single.html")
        File outputFile = new File(guideDir, "single.pdf")

        String xml = createXml(htmlFile, baseDir.absolutePath)
        createPdf xml, outputFile, guideDir
    }

    static String createXml(File htmlFile, String base) {
        String xml = htmlFile.getText("UTF-8")

        // fix inner anchors
        xml = xml.replaceAll('<a href="\\.\\./guide/single\\.html', '<a href="')
        // fix image refs to absolute paths
        xml = xml.replaceAll('src="\\.\\./img/', "src=\"file://${base}/img/")

        // convert tabs to spaces otherwise they only take up one space
        xml = xml.replaceAll('\t', '    ')
        cleanupHtml(htmlFile, xml)
    }

    static boolean cleanHtml = Boolean.getBoolean("grails.docs.clean.html")
    static boolean debugPdf = Boolean.getBoolean("grails.docs.debug.pdf")
    
    private static cleanupHtml(File htmlFile, String xml) {
        def result = cleanHtml ? Jsoup.parse(xml).outerHtml() : xml
        if(debugPdf) {
            File before = new File(htmlFile.absolutePath + '.before.xml')
            before.setText(xml, 'UTF-8')
            if(result != xml) {
                File after = new File(htmlFile.absolutePath + '.after.xml')
                after.setText(result, 'UTF-8')
            }
        }
        result
    }

    static void createPdf(String xml, File outputFile, File urlBase) {
        def dbf = DocumentBuilderFactory.newInstance()
        dbf.validating = false
        dbf.setFeature "http://apache.org/xml/features/nonvalidating/load-external-dtd", false
        dbf.setFeature "http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false
        
        DocumentBuilder builder = dbf.newDocumentBuilder()
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))

        ITextRenderer renderer = new ITextRenderer()
        renderer.setDocument(doc, urlBase.toURI().toString())

        OutputStream outputStream
        try {
            outputStream = new FileOutputStream(outputFile)
            renderer.layout()
            renderer.createPDF(outputStream)
        }
        finally {
            outputStream?.close()
        }
    }
}
