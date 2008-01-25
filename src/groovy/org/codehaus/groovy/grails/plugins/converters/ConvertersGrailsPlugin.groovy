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

import grails.converters.JSON
import grails.converters.XML
import grails.util.GrailsUtil
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.converters.Converter
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.converters.XMLParsingParameterCreationListener
import org.codehaus.groovy.grails.web.converters.JSONParsingParameterCreationListener
import org.springframework.core.JdkVersion

/**
 * A plug-in that allows the obj as XML syntax
 *
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 *
 * @since 0.6
 *
 */
class ConvertersGrailsPlugin {
    def version = GrailsUtil.getGrailsVersion()

    def author = "Siegfried Puchbauer"
    def title = "Provides JSON and XML Conversion for common Objects (Domain Classes, Lists, Maps, POJO)"
    def description = """
		The grails-converters plugin aims to give you the ability to convert your domain objects, maps and lists to JSON or XML very quickly, to ease development for AJAX based applications. The plugin leverages the the groovy "as" operator and extends the render method in grails controllers to directly send the result to the output stream. It also adds the Grails Codecs mechanism for XML and JSON.
	"""
    def documentation = "http://grails.org/Converters+Plugin"
    def providedArtefacts = [org.codehaus.groovy.grails.plugins.converters.codecs.JSONCodec, org.codehaus.groovy.grails.plugins.converters.codecs.XMLCodec]
    def observe = ["controllers"]

    def dependsOn = [
            controllers: GrailsUtil.getGrailsVersion(),
            domainClass: GrailsUtil.getGrailsVersion()
            ]


    def doWithSpring = {
        xmlParsingParameterCreationListener(XMLParsingParameterCreationListener)
        jsonParsingParameterCreationListener(JSONParsingParameterCreationListener)
    }

    def renderMethod = {Converter converter ->
        converter.render(delegate.response);
    }
    def headerMethod = {String key, def value ->
        if (value) delegate.response?.setHeader(key, value.toString())
    }
    def jsonHeaderMethod = {def value ->
        def json = (value instanceof JSON || value instanceof JSONObject || value instanceof JSONArray) ? value : (new JSON(value));
        if (value) delegate.response?.setHeader("X-JSON", value.toString())
    }

    def onChange = {event ->
        def mc = event.source.metaClass
        mc.render = renderMethod
        mc.header = headerMethod
        mc.jsonHeader = jsonHeaderMethod
    }

    def doWithDynamicMethods = {applicationContext ->
        try {
            ConverterUtil.setGrailsApplication(application);

            log.debug "Applying new header and render methods to all Controllers..."
            def controllerClasses = application.controllerClasses
            for (controller in controllerClasses) {
                def mc = controller.metaClass
                mc.render = renderMethod
                mc.header = headerMethod
                mc.jsonHeader = jsonHeaderMethod
            }

            def asTypeMethod = {java.lang.Class clazz ->
                if (ConverterUtil.isConverterClass(clazz)) {
                    return ConverterUtil.createConverter(clazz, delegate)
                } else {
                    return ConverterUtil.invokeOriginalAsTypeMethod(delegate, clazz)
                }
            }

            for (dc in application.domainClasses) {
                def mc = dc.metaClass
                mc.asType = asTypeMethod
                ConverterUtil.addAlias(dc.propertyName, dc.clazz);
                log.debug "Adding XStream alias ${dc.propertyName} for class ${dc.clazz.getName()}"
            }


            // Override GDK asType for some common Interfaces and Classes
            List targetClasses = [java.util.ArrayList, java.util.TreeSet, java.util.HashSet, java.util.List, java.util.Set, java.util.Collection, groovy.lang.GroovyObject, java.lang.Object]
            if(JdkVersion.getMajorJavaVersion() >= JdkVersion.JAVA_15) {
                targetClasses << java.lang.Enum
            }
            targetClasses.each {Class clazz ->
                def mc = GroovySystem.metaClassRegistry.getMetaClass(clazz)
                if (!mc instanceof ExpandoMetaClass) {
                    log.warn "Unable to add Converter Functionality to Class ${className}"
                    return;
                }
                log.debug "Adding Converter asType Method to Class ${clazz} [${clazz.class}] -> [${mc.class}]"
                mc.asType = asTypeMethod
                mc.initialize()
            }

            // Methods for Reading JSON/XML from Requests
            def getXMLMethod = {->
                return XML.parse((HttpServletRequest) delegate)
            }
            def getJSONMethod = {->
                return JSON.parse((HttpServletRequest) delegate)
            }
            def requestMc = GroovySystem.metaClassRegistry.getMetaClass(HttpServletRequest)
            requestMc.getXML = getXMLMethod
            requestMc.getJSON = getJSONMethod
            requestMc.initialize()

            // TODO:
            // add asType Method to XmlSlurper to unmarshalling
            // of XML Content and implement unmarshalling in JSONObject/Array...

            log.debug "Converters Plugin configured successfully"
        } catch (Exception e) {
            log.error(e)
        }
    }
}