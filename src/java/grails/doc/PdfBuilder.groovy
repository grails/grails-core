package grails.doc

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import org.w3c.dom.Document
import org.xhtmlrenderer.pdf.ITextRenderer

class PdfBuilder {

    private static final String LIVE_DOC_SITE = 'http://grails.org'

    static void build(String baseDir, String grailsHome) {
        baseDir = new File(baseDir).canonicalPath

        File htmlFile = new File("${baseDir}/docs/manual/guide/single.html")
        File outputFile = new File("${baseDir}/docs/manual/guide/single.pdf")
        String urlBase = "file://${grailsHome}/src/grails/docs/style"

        String xml = createXml(htmlFile)
        createPdf(xml, outputFile, urlBase)
    }

    private static String createXml(File htmlFile) {
        String xml = htmlFile.text

        // tweak main css so it doesn't get ignored
        xml = xml.replace('media="screen"', 'media="print"')

        // fix inner anchors
        xml = xml.replaceAll('<a href="../guide/single.html', '<a href="')

        // convert tabs to spaces otherwise they only take up one space
        xml = xml.replaceAll('\t', '    ')
        xml
    }

    private static void createPdf(String xml, File outputFile, String urlBase) {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()))

        ITextRenderer renderer = new ITextRenderer()
        renderer.setDocument(doc, urlBase + '/dummy')

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
