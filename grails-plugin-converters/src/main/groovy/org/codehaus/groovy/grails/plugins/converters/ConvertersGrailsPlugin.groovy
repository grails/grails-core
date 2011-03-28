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
package org.codehaus.groovy.grails.plugins.converters

import org.codehaus.groovy.grails.web.converters.marshaller.json.ValidationErrorsMarshaller as JsonErrorsMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.xml.ValidationErrorsMarshaller as XmlErrorsMarshaller

import grails.converters.JSON
import grails.converters.XML
import grails.util.GrailsUtil
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersApi
import org.codehaus.groovy.grails.plugins.converters.codecs.JSONCodec
import org.codehaus.groovy.grails.plugins.converters.codecs.XMLCodec
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.JSONParsingParameterCreationListener
import org.codehaus.groovy.grails.web.converters.XMLParsingParameterCreationListener
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.converters.configuration.ObjectMarshallerRegisterer
import org.springframework.validation.Errors
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersControllersApi

/**
 * A plug-in that allows the obj as XML syntax.
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 *
 * @since 0.6
 */
class ConvertersGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()

    def author = "Siegfried Puchbauer"
    def title = "Provides JSON and XML Conversion for common Objects (Domain Classes, Lists, Maps, POJO)"
    def description = """
        The grails-converters plugin aims to give you the ability to convert your domain objects, maps and lists to JSON or XML very quickly, to ease development for AJAX based applications. The plugin leverages the the groovy "as" operator and extends the render method in grails controllers to directly send the result to the output stream. It also adds the Grails Codecs mechanism for XML and JSON.
    """
    def documentation = "http://grails.org/Converters+Plugin"
    def providedArtefacts = [JSONCodec, XMLCodec]
    def observe = ["controllers"]

    def dependsOn = [
        controllers: GrailsUtil.getGrailsVersion(),
        domainClass: GrailsUtil.getGrailsVersion()
    ]

    def doWithSpring = {
        xmlParsingParameterCreationListener(XMLParsingParameterCreationListener)

        jsonParsingParameterCreationListener(JSONParsingParameterCreationListener)

        jsonErrorsMarshaller(JsonErrorsMarshaller)

        xmlErrorsMarshaller(XmlErrorsMarshaller)

        convertersConfigurationInitializer(ConvertersConfigurationInitializer)

        errorsXmlMarshallerRegisterer(ObjectMarshallerRegisterer) {
            marshaller = { XmlErrorsMarshaller om -> }
            converterClass = grails.converters.XML
        }

        errorsJsonMarshallerRegisterer(ObjectMarshallerRegisterer) {
            marshaller = { JsonErrorsMarshaller om -> }
            converterClass = grails.converters.JSON
        }

        instanceConvertersControllersApi(ConvertersControllersApi)
    }

    def doWithDynamicMethods = {applicationContext ->
        // TODO: Get rid of this evil static singleton code
        ConverterUtil.setGrailsApplication(application)

        applicationContext.convertersConfigurationInitializer.initialize(application)
        MetaClassEnhancer enhancer = new MetaClassEnhancer()
        enhancer.addApi(new ConvertersApi(applicationContext:applicationContext))

        // Override GDK asType for some common Interfaces and Classes
        enhancer.enhanceAll( [Errors, ArrayList, TreeSet, HashSet, List, Set, Collection, GroovyObject, Object, Enum].collect {
            GrailsMetaClassUtils.getExpandoMetaClass(it)
        } )

        // Methods for Reading JSON/XML from Requests
        def getXMLMethod = { -> XML.parse((HttpServletRequest) delegate) }
        def getJSONMethod = { -> JSON.parse((HttpServletRequest) delegate) }
        def requestMc = GroovySystem.metaClassRegistry.getMetaClass(HttpServletRequest)
        requestMc.getXML = getXMLMethod
        requestMc.getJSON = getJSONMethod

        log.debug "Converters Plugin configured successfully"
    }
}
