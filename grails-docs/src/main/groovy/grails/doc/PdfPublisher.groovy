/*
 * Copyright 2024 original authors
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

@CompileStatic
class PdfPublisher {

    static void publishPdfFromHtml(File outputDir, String child, String pdfName) {
        PdfBuilder pdfBuilder = new PdfBuilder()
        File currFile = new File(outputDir, child)
        String xml = pdfBuilder.createXml(currFile, outputDir.absolutePath)
        Document doc = pdfBuilder.createDocument(xml)
        File outputFile = new File(currFile.parentFile, pdfName)
        File urlBase = new File(outputDir, "guide/single.html")
        pdfBuilder.createPdfWithDocument(doc, outputFile, urlBase)
    }
}
