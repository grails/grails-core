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

import spock.lang.Specification

class PdfPublisherSpec extends Specification {

    void "generate pdf from sample docs"() {
        given:
        String sampleDocsFolderPath = 'src/test/resources/docs'
        File sampleDocsFolder = new File(sampleDocsFolderPath)
        String pdfName = 'single.pdf'
        String child = "guide/single.html"

        expect:
        sampleDocsFolder.exists()
        !new File("${sampleDocsFolderPath}/guide/${pdfName}").exists()

        when:
        PdfPublisher.publishPdfFromHtml(sampleDocsFolder, child, pdfName)

        then:
        noExceptionThrown()
        new File("${sampleDocsFolderPath}/guide/${pdfName}").exists()

        cleanup:
        new File("${sampleDocsFolderPath}/guide/${pdfName}").delete()
    }
}
