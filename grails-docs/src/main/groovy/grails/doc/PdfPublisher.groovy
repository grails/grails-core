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

import groovy.transform.CompileStatic
import org.w3c.dom.Document

/**
 * Publishes PDF files from HTML files using the {@link PdfBuilder} class.
 *
 * @author Sergio del Amo
 * @since 3.3.0
 */
@CompileStatic
class PdfPublisher {

    /**
     * Publishes a PDF from a single HTML file in the same input and output directory. (Deprecated)
     *
     * @param outputDir The output directory for the PDF file or the same directory as the HTML file
     * @param child The relative path to the HTML file within the output directory
     * @param pdfName The PDF file name (optional)
     *
     * @deprecated Use {@link #publishPdfFromHtml(File, File, String, String)} instead
     */
    @Deprecated
    static void publishPdfFromHtml(File outputDir, String child, String pdfName) {
        PdfBuilder pdfBuilder = new PdfBuilder()
        File currFile = new File(outputDir, child)
        String xml = pdfBuilder.createXml(currFile, outputDir.absolutePath)
        Document doc = pdfBuilder.createDocument(xml)
        File outputFile = new File(currFile.parentFile, pdfName)
        File urlBase = new File(outputDir, "guide/single.html")
        pdfBuilder.createPdfWithDocument(doc, outputFile, urlBase)
    }

    /**
     * Publishes a PDF from a single HTML file
     *
     * @param inputDir The input directory for the HTML file
     * @param outputDir The output directory for the PDF file
     * @param singleHtmlPath The path to the single HTML file
     * @param pdfName The relative path to the PDF file within the output directory (optional)
     */
    static void publishPdfFromHtml(File inputDir, File outputDir, String singleHtmlPath, String pdfName) {
        if (pdfName == null) {
            pdfName = singleHtmlPath.replace(".html", ".pdf")
        }
        final PdfBuilder pdfBuilder = new PdfBuilder()
        File singleHtmlFile = new File(inputDir, singleHtmlPath)
        final String xml = pdfBuilder.createXml(singleHtmlFile, outputDir.absolutePath)
        Document doc = pdfBuilder.createDocument(xml)
        File outputFile = new File(outputDir, pdfName)
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        pdfBuilder.createPdfWithDocument(doc, outputFile, singleHtmlFile)
    }
}
