package grails.doc.internal

import grails.doc.PdfBuilder
import spock.lang.Specification

class PublishPDFSpec extends Specification {

    void "generate pdf from docs"() {
        when:
        System.setProperty('grails.docs.clean.html','true')
        String pdfName = 'single.pdf'
        def outputDir = new File('/Users/sdelamo/git/grails/grails-doc/build/docs')
        def currFile = new File(outputDir, "guide/single.html")
        def pdfBuilder = new PdfBuilder()
        def xml = pdfBuilder.createXml(currFile, outputDir.absolutePath)
        pdfBuilder.createPdf(xml,
                new File(currFile.parentFile, pdfName),
                new File(outputDir, "guide/single.html"))

        then:
        noExceptionThrown()
    }

    void "remove CssLinks"() {
        given:
        String html = """
<head>
        <title>The Grails Framework 3.2.11</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <link rel="stylesheet" href="../css/main.css" type="text/css" media="screen, print" title="Style" charset="utf-8" />
        <link rel="stylesheet" href="../css/pdf.css" type="text/css" media="print" title="PDF" charset="utf-8" />
    <script type="text/javascript">
function addJsClass(el) {
    var classes = document.body.className.split(" ");
    classes.push("js");
    document.body.className = classes.join(" ");
}
    </script>
    </head>
    """
        String expected = """
<head>
        <title>The Grails Framework 3.2.11</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        
        
    <script type="text/javascript">
function addJsClass(el) {
    var classes = document.body.className.split(" ");
    classes.push("js");
    document.body.className = classes.join(" ");
}
    </script>
    </head>
    """
        when:
        String output = PdfBuilder.removeCssLinks(html)

        then:
        output == expected

    }
}
