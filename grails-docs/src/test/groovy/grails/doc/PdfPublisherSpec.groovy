package grails.doc

import spock.lang.Specification

class PdfPublisherSpec extends Specification {

    void "generate pdf from sample docs"() {
        given: "sample docs folder, pdf name and html path"
        String sampleDocsFolderPath = 'src/test/resources/docs'
        File sampleDocsFolder = new File(sampleDocsFolderPath)
        String pdfName = 'guide/single.pdf'
        String singleHtmlPath = "guide/single.html"

        expect: "sample docs folder exists and pdf file does not exist"
        sampleDocsFolder.exists()
        !new File("${sampleDocsFolderPath}/${pdfName}").exists()

        when: "generate pdf from sample docs"
        PdfPublisher.publishPdfFromHtml(sampleDocsFolder, new File("build/manual-pdf"), singleHtmlPath, pdfName)

        then: "pdf file is generated successfully"
        noExceptionThrown()
        new File("build/manual-pdf/${pdfName}").exists()

        cleanup: "cleanup pdf file"
        new File("build/manual-pdf/${pdfName}").delete()
    }

    void "generate pdf from sample docs with default pdf name"() {
        given:
        String sampleDocsFolderPath = 'src/test/resources/docs'
        File sampleDocsFolder = new File(sampleDocsFolderPath)
        String pdfName = null
        String singleHtmlPath = "guide/single.html"

        expect:
        sampleDocsFolder.exists()
        !new File("${sampleDocsFolderPath}/guide/single.pdf").exists()

        when:
        PdfPublisher.publishPdfFromHtml(sampleDocsFolder, new File("build/manual-pdf"), singleHtmlPath, pdfName)

        then:
        noExceptionThrown()
        new File("build/manual-pdf/guide/single.pdf").exists()

        cleanup:
        new File("build/manual-pdf/guide/single.pdf").delete()
    }

}
