/* Copyright 2008 the original author or authors.
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
package grails.test

import groovy.xml.StreamingMarkupBuilder
import grails.util.GrailsNameUtils

/**
 * Support class for writing unit tests for controllers. Its main job
 * is to mock the various properties and methods that Grails injects
 * into controllers. By default it determines what controller to mock
 * based on the name of the test, but this can be overridden by one
 * of the constructors.
 * 
 * @author Graeme Rocher
 * @author Peter Ledbrook
 */
class ControllerUnitTestCase extends MvcUnitTestCase {
    protected controller

    /**
     * Creates a new test case for the controller that is in the same
     * package as the test case and has the same prefix before "Controller"
     * in its name. For example, if the class name of the test were
     * <code>org.example.TestControllerTests</code>, this constructor
     * would mock <code>org.example.TestController</code>.
     */
    ControllerUnitTestCase() {
        super("Controller")
    }

    /**
     * Creates a new test case for the given controller class.
     */
    ControllerUnitTestCase(Class controllerClass) {
        super(controllerClass)
    }

    protected void setUp() {
        super.setUp()
        mockController(this.testClass)
        this.controller = newInstance()
    }

    Class getControllerClass() {
        return this.testClass
    }

    /**
     * Mocks a command object class, providing a "validate()" method
     * and access to the "errors" property.
     */
    protected mockCommandObject(Class clazz) {
        registerMetaClass(clazz)
        MockUtils.mockCommandObject(clazz, errorsMap)
    }

    /**
     * Sets an XML string as the body of the mock HTTP request. The
     * string is converted to bytes based on a UTF-8 encoding.
     */
    protected void setXmlRequestContent(String content) {
        setXmlRequestContent("UTF-8", content)
    }

    /**
     * Sets an XML string as the body of the mock HTTP request. The
     * given string is converted to bytes according to the given encoding.
     */
    protected void setXmlRequestContent(String encoding, String content) {
        mockRequest.contentType = "application/xml; charset=$encoding"
        mockRequest.content = content.getBytes(encoding)
    }

    /**
     * Sets the body of the mock HTTP request to some XML. The XML is
     * provided as an XmlBuilder-compliant closure. The XML is encoded
     * to bytes using UTF-8.
     */
    protected void setXmlRequestContent(Closure c) {
        setXmlRequestContent("UTF-8", c)
    }

    /**
     * Sets the body of the mock HTTP request to some XML. The XML is
     * provided as an XmlBuilder-compliant closure. The XML is encoded
     * to bytes using the specified encoding.
     */
    protected void setXmlRequestContent(String encoding, Closure c) {
        def xml = new StreamingMarkupBuilder(encoding: encoding).bind(c)
        def out = new ByteArrayOutputStream()
        out << xml

        mockRequest.contentType = "application/xml; charset=$encoding"
        mockRequest.content = out.toByteArray()
    }

    protected Object newInstance() {
        def instance = super.newInstance();
        webRequest.controllerName = GrailsNameUtils.getLogicalPropertyName(instance.class.name, "Controller")

        return instance
    }


}
