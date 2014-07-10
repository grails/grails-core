package org.grails.web.converters

import grails.artefact.Artefact;
import grails.converters.XML
import grails.test.mixin.TestFor

import org.junit.Test

@TestFor(XmlController)
class ControllerWithXmlConvertersTests {

    @Test
    void testConvertArrayWithNullEments() {
        controller.convertArray()

        assert response.text == '<?xml version="1.0" encoding="UTF-8"?><array><string>tst0</string><string>tst1</string><null /><string>fail</string></array>'
    }
    @Test
    void testConvertListWithNullEments() {
        controller.convertList()

        assert response.text == '<?xml version="1.0" encoding="UTF-8"?><list><string>tst0</string><string>tst1</string><null /><string>fail</string></list>'
    }
}

@Artefact("Controller")
class XmlController {

     def convertArray() {
        def ar = []

        ar[0] = "tst0"
        ar[1] = "tst1"
        ar[3] = "fail"

        ar = ar as String[]

        render ar as XML
    }

    def convertList() {
        def ar = []

        ar[0] = "tst0"
        ar[1] = "tst1"
        ar[3] = "fail"

        render ar as XML
    }
}
