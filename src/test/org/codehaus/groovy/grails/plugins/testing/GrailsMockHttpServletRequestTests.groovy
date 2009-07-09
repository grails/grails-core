/* Copyright 2009 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.testing

/**
 * Test case for {@link GrailsMockHttpServletRequest}.
 */
class GrailsMockHttpServletRequestTests extends GroovyTestCase {
    static final String TEST_XML_CONTENT = """\
<root>
  <item qty="10">
    <name>Orange</name>
    <type>Fruit</type>
  </item>
  <item qty="6">
    <name>Apple</name>
    <type>Fruit</type>
  </item>
  <item qty="2">
    <name>Chair</name>
    <type>Furniture</type>
  </item>
</root>
"""

    static final String TEST_JSON_CONTENT = """\
[{ qty: 10, name: "Orange", type: "Fruit" },
 { qty: 6, name: "Apple", type: "Fruit" },
 { qty: 2, name: "Chair", type: "Furniture" } ]
"""

    void testGetXML() {
        // Set up the test data.
        def request = new GrailsMockHttpServletRequest()
        request.contentType = "application/xml; charset=UTF-8"
        request.content = TEST_XML_CONTENT.getBytes("UTF-8")

        // Test the method.
        verifyXmlResult request.XML
    }

    void testGetXMLMultipleCalls() {
        def request = new GrailsMockHttpServletRequest()
        request.contentType = "application/xml; charset=UTF-8"
        request.content = TEST_XML_CONTENT.getBytes("UTF-8")

        // Test the method.
        verifyXmlResult request.XML

        // Try again.
        verifyXmlResult request.XML

        // And one more time.
        verifyXmlResult request.XML
    }

    void testGetXMLNoContent() {
        def request = new GrailsMockHttpServletRequest()
        shouldFail {
            request.XML
        }
    }

    void testGetXMLContentNotXml() {
        // Set up the test data.
        def content = """\
First line
Second line
"""
        def request = new GrailsMockHttpServletRequest()
        request.contentType = "text/plain; charset=UTF-8"
        request.content = content.getBytes("UTF-8")

        // Test the method.
        shouldFail {
            request.XML
        }
    }

    void testGetJSON() {
        // Set up the test data.
        def request = new GrailsMockHttpServletRequest()
        request.contentType = "text/json; charset=UTF-8"
        request.content = TEST_JSON_CONTENT.getBytes("UTF-8")

        // Test the method.
        verifyJsonResult request.JSON
    }

    void testGetJSONMultipleCalls() {
        // Set up the test data.
        def request = new GrailsMockHttpServletRequest()
        request.contentType = "text/json; charset=UTF-8"
        request.content = TEST_JSON_CONTENT.getBytes("UTF-8")

        // Test the method.
        verifyJsonResult request.JSON

        // Try again.
        verifyJsonResult request.JSON

        // And one more time.
        verifyJsonResult request.JSON
    }

    void testGetJSONNoContent() {
        def request = new GrailsMockHttpServletRequest()
        shouldFail {
            request.JSON
        }
    }

    void testGetJSONContentNotJson() {
        // Set up the test data.
        def content = """\
First line
Second line
"""
        def request = new GrailsMockHttpServletRequest()
        request.contentType = "text/plain; charset=UTF-8"
        request.content = content.getBytes("UTF-8")

        // Test the method.
        shouldFail {
            request.JSON
        }
    }

    private void verifyXmlResult(xml) {
        assertEquals 3, xml.item.size()
        assertEquals "Apple", xml.item[1].name.text()
        assertEquals "2", xml.item[2].@qty.text()
    }

    private void verifyJsonResult(json) {
        assertEquals 3, json.size()
        assertEquals "Apple", json[1].name
        assertEquals 2, json[2].qty
    }
}
