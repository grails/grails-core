/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.grails.plugins.converters

import grails.converters.JSON
import grails.converters.XML
import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import grails.util.GrailsUtil

import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.web.converters.configuration.ObjectMarshallerRegisterer
import org.grails.web.converters.marshaller.json.ValidationErrorsMarshaller as JsonErrorsMarshaller
import org.grails.web.converters.marshaller.xml.ValidationErrorsMarshaller as XmlErrorsMarshaller

/**
 * Allows the "obj as XML" and "obj as JSON" syntax.
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 *
 * @since 0.6
 */
class ConvertersGrailsPlugin implements GrailsApplicationAware {

    def version = GrailsUtil.getGrailsVersion()

    def author = "Siegfried Puchbauer"
    def title = "Provides JSON and XML Conversion for common Objects (Domain Classes, Lists, Maps, POJO)"
    def description = """
        The grails-converters plugin aims to give you the ability to convert your domain objects, maps and lists to JSON or XML very quickly, to ease development for AJAX based applications. The plugin leverages the the groovy "as" operator and extends the render method in grails controllers to directly send the result to the output stream. It also adds the Grails Codecs mechanism for XML and JSON.
    """
    def documentation = "http://grails.org/Converters+Plugin"
    def observe = ["controllers"]

    def dependsOn = [controllers: version, domainClass: version]

    GrailsApplication grailsApplication

    def doWithSpring = {
        jsonErrorsMarshaller(JsonErrorsMarshaller)

        xmlErrorsMarshaller(XmlErrorsMarshaller)

        convertersConfigurationInitializer(ConvertersConfigurationInitializer)

        errorsXmlMarshallerRegisterer(ObjectMarshallerRegisterer) {
            marshaller = { XmlErrorsMarshaller om -> }
            converterClass = XML
        }

        errorsJsonMarshallerRegisterer(ObjectMarshallerRegisterer) {
            marshaller = { JsonErrorsMarshaller om -> }
            converterClass = JSON
        }
    }
}
