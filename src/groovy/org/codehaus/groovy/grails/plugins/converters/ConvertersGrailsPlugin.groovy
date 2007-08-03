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

import grails.converters.*
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.plugins.converters.codecs.JSONCodec
import org.codehaus.groovy.grails.web.converters.Converter
import org.codehaus.groovy.grails.web.converters.ConverterUtil

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
	def providedArtefacts = [ JSONCodec]
	def observe = ["controllers"]

	def dependsOn = [
	    controllers: GrailsUtil.getGrailsVersion(),
	    domainClass: GrailsUtil.getGrailsVersion()
	]

    def renderMethod = { Converter converter ->
                converter.render(delegate.response);
    }
    def headerMethod = { String key, def value ->
            if(value) delegate.response?.setHeader(key, value.toString())
    }
    def jsonHeaderMethod = { def value ->
            if(value) delegate.response?.setHeader("X-JSON", value.toString())
    }

    def onChange = { event ->
        def mc = event.source.metaClass
        mc.render = renderMethod
        mc.header = headerMethod
        mc.jsonHeader = jsonHeaderMethod
    }

	def doWithDynamicMethods = { applicationContext ->
        ConverterUtil.setGrailsApplication(application);

		def controllerClasses = application.controllerClasses
        for(controller in controllerClasses) {
            def mc = controller.metaClass
			mc.render = renderMethod
			mc.header = headerMethod
			mc.jsonHeader = jsonHeaderMethod
		}

        def asTypeMethod = { Class clazz ->
                if(ConverterUtil.isConverterClass(clazz)) {
                    return ConverterUtil.createConverter(clazz, delegate)
                } else {
                    return ConverterUtil.invokeOriginalAsTypeMethod(delegate, clazz)
                }
        }

        for(dc in application.domainClasses) {
            def mc = dc.metaClass
            mc.asType = asTypeMethod
            XML.addAlias(dc.propertyName, dc.clazz);
            log.debug "Adding XStream alias ${dc.propertyName} for class ${dc.clazz.getName()}"
        }

        [Collection,List,Set,ArrayList,HashSet,TreeSet, Object].each { collectionClass ->
			collectionClass.metaClass.asType = { Class clazz ->
				if(ConverterUtil.isConverterClass(clazz)) {
					return ConverterUtil.createConverter(clazz, delegate);
				} else {
					return ConverterUtil.invokeOriginalAsTypeMethod(delegate, clazz)
				}
			}
		}
		log.debug "Converters Plugin configured successfully"
	}
}