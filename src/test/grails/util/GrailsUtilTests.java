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
import junit.framework.TestCase;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Tests for the GrailsUtils class
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 22, 2007
 *        Time: 6:21:53 PM
 */
public class GrailsUtilTests extends TestCase {

    public void testEnvironment() {

        System.setProperty(Environment.KEY, "");
        assertEquals(Environment.DEVELOPMENT.getName(), GrailsUtil.getEnvironment());

        System.setProperty(Environment.KEY, "prod");
        assertEquals(Environment.PRODUCTION.getName(), GrailsUtil.getEnvironment());

        System.setProperty(Environment.KEY, "dev");
        assertEquals(Environment.DEVELOPMENT.getName(), GrailsUtil.getEnvironment());

        System.setProperty(Environment.KEY, "test");
        assertEquals(Environment.TEST.getName(), GrailsUtil.getEnvironment());

        System.setProperty(Environment.KEY, "myenvironment");
        assertEquals("myenvironment", GrailsUtil.getEnvironment());        
    }

    public void testGrailsVersion() {
        assertEquals("1.2.0.RC1", GrailsUtil.getGrailsVersion());
    }

    protected void tearDown() throws Exception {
        System.setProperty(Environment.KEY, "");
    }

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
