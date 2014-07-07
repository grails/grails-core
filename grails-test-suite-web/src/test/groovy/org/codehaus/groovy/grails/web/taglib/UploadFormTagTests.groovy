package org.codehaus.groovy.grails.web.taglib

import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@TestFor(ApplicationTagLib)
class UploadFormTagTests {

    @Test
    void testUploadForm() {
        def template = '<g:uploadForm name="myForm"></g:uploadForm>'
        assertOutputEquals('<form action="/test" method="post" name="myForm" enctype="multipart/form-data" id="myForm" ></form>', template)
    }
}
