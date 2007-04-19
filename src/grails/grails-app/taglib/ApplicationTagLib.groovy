/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /**
 * The base application tag library for Grails many of which take inspiration from Rails helpers (thanks guys! :)
 * This tag library tends to get extended by others as tags within here can be re-used in said libraries
 *
 * @author Graeme Rocher
 * @since 17-Jan-2006
 */
import org.springframework.validation.Errors;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.servlet.support.RequestContextUtils as RCU;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;

class ApplicationTagLib { 
	
	def grailsUrlMappingsHolder
	
    /**
     * Creates a link to a resource, generally used as a method rather than a tag.
     *
     * eg. <link type="text/css" href="${createLinkTo(dir:'css',file:'main.css')}" />
     */
    def createLinkTo = { attrs ->
         out << grailsAttributes.getApplicationUri(request)
         if(attrs['dir']) {
            out << "/${attrs['dir']}"
         }
         if(attrs['file']) {
            out << "/${attrs['file']}"
         }
    }

    /**
     *  General linking to controllers, actions etc. Examples:
     *
     *  <g:link action="myaction">link 1</gr:link>
     *  <g:link controller="myctrl" action="myaction">link 2</gr:link>
     */
    def link = { attrs, body ->
        out << "<a href=\""
        // create the link
        out << createLink(attrs)

        out << '\" '
        // process remaining attributes
        attrs.each { k,v ->
            out << k << "=\"" << v << "\" "
        }
        out << ">"
        // output the body
        body()

        // close tag
        out << "</a>"
    }


    /**
     * Creates a grails application link from a set of attributes. This
     * link can then be included in links, ajax calls etc. Generally used as a method call
     * rather than a tag eg.
     *
     *  <a href="${createLink(action:'list')}">List</a>
     */
    def createLink = { attrs ->     
        out << grailsAttributes.getApplicationUri(request)
        // prefer a URL attribute
        if(attrs['url']) {
             attrs = attrs.remove('url')
        }
                                                     
		def controller = attrs.remove("controller")
		def action = attrs.remove("action")
		def params = attrs.remove("params")
		def mapping = grailsUrlMappingsHolder?.getReverseMapping(controller, 
																 action, 
																 params)
        def url = mapping?.createURL(params)
		if(url) {
			out << url
		}             
		else {
	        // if the current attribute null set the controller uri to the current controller
	        if(controller) {
	            out << '/' << controller
	        }
	        else {
	           out << grailsAttributes.getControllerUri(request)
	        }
	        if(action) {
	            out << '/' << action
	        }
	        if(attrs["id"]) {
	            out << '/' << attrs.remove("id")
	        }
	        if(params) {
	            out << '?'
	            def i = 0
	            params.each { k,v ->
	                out << "${k.encodeAsURL()}=${v?.encodeAsURL()}"
	                if(++i < params.size())
	                   out << '&'
	            }
	        }			
		}
															
    }

	/**
	 * Helper method for creating tags called like:
	 *
	 * withTag(name:'script',attrs:[type:'text/javascript']) {
	 *
	 * }
	 */
	def withTag = { attrs, body ->
		out << "<${attrs.name}"
		if(attrs.attrs) {
			attrs.attrs.each{ k,v ->
				if(v) {
					if(v instanceof Closure) {
						out << " $k=\""
					    v()
						out << '"'
					}
					else {
						out << " $k=\"$v\""
					}					
				} 				
			}
		}
		out << '>'
		body()
		out << "</${attrs.name}>"			
	}	
}
