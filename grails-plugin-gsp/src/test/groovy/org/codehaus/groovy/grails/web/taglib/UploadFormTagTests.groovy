package org.codehaus.groovy.grails.web.taglib

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class UploadFormTagTests extends AbstractGrailsTagTests {

    void testUploadForm() {
        def template = '<g:uploadForm name="myForm"></g:uploadForm>'
        assertOutputEquals('<form action="" method="post" name="myForm" enctype="multipart/form-data" id="myForm" ></form>', template)
    }
}
