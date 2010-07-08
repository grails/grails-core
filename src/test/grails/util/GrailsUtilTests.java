/* Copyright 2004-2005 Graeme Rocher
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
package grails.util;

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * Tests for the GrailsUtils class.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsUtilTests extends TestCase {

    public void testGrailsVersion() {
        assertEquals("1.3.4.BUILD-SNAPSHOT", GrailsUtil.getGrailsVersion());
    }

    @Override
    protected void tearDown() throws Exception {
        System.setProperty(Environment.KEY, "");
    }

    @SuppressWarnings("deprecation")
    public void testWriteSlurperResult() throws SAXException, ParserConfigurationException, IOException {
        String testXml = "<root><books><book isbn=\"45734957\">" +
                         "<title>Misery</title><author>Stephen King</author>" +
                         "</book></books></root>";
        GPathResult result = new XmlSlurper().parseText(testXml);

        StringWriter output = new StringWriter(testXml.length() + 20);
        GrailsUtil.writeSlurperResult(result, output);

        testXml = testXml.replaceAll("<root>", "<root xmlns='http://java.sun.com/xml/ns/j2ee'>");
        testXml = testXml.replace('"', '\'');
        assertEquals(testXml, output.toString());
    }
}
