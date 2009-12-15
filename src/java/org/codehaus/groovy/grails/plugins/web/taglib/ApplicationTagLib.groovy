package org.codehaus.groovy.grails.plugins.web.taglib

import grails.util.Environment
import grails.util.GrailsUtil
import grails.util.Metadata
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

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
class ApplicationTagLib implements ApplicationContextAware, InitializingBean {

    ApplicationContext applicationContext
    GrailsPluginManager pluginManager
    
    def grailsUrlMappingsHolder


	static final SCOPES = [page:'pageScope',
						   application:'servletContext',
						   request:'request',
						   session:'session',
						   flash:'flash']


    boolean useJsessionId = false

    public void afterPropertiesSet() {
      def config = applicationContext.getBean(GrailsApplication.APPLICATION_ID).config
      if(config.grails.views.enable.jsessionid instanceof Boolean) {
         useJsessionId = config.grails.views.enable.jsessionid
      }
    }

    /**
     * Obtains the value of a cookie
     */
    def cookie = { attrs ->
        def cke = request.cookies.find {it.name == attrs['name']}
        if(cke)
            out << cke.value
    }

    def header = { attrs ->
        if(attrs.name) {
            def hdr = request.getHeader(attrs.name)
            if(hdr) out << hdr
        }
    }

    /**
     * Sets a variable in the pageContext or the specified scope
     */
	def set = { attrs, body ->
		def scope = attrs.scope ? SCOPES[attrs.scope] : 'pageScope'
		def var = attrs.var
		def value = attrs.value
		def containsValue = attrs.containsKey('value')
		if(!scope) throw new IllegalArgumentException("Invalid [scope] attribute for tag <g:set>!")
		if(!var) throw new IllegalArgumentException("[var] attribute must be specified to for <g:set>!")

		if(!containsValue && body) value = body()

		this."$scope"."$var" = value
		null
	}

    /**
     * Get the declared URL of the server from config, or guess at localhost for non-production
     */
    String makeServerURL() {
        def u = ConfigurationHolder.config.grails.serverURL
        if (!u) {
            // Leave it null if we're in production so we can throw
            if (Environment.current != Environment.PRODUCTION) {
                u = "http://localhost:" +(System.getProperty('server.port') ? System.getProperty('server.port') : "8080")
            }
        }
        return u
    }

    /**
     * Check for "absolute" attribute and render server URL if available from Config or deducible in non-production
     */
    private handleAbsolute(attrs) {
        def base = attrs.remove('base')
        if(base) {
            return base
        }
        else {
            def abs = attrs.remove("absolute")
            if (Boolean.valueOf(abs)) {
                def u = makeServerURL()
                if (u) {
                    return u
                } else {
                    throwTagError("Attribute absolute='true' specified but no grails.serverURL set in Config")
                }
            }
            else {
               return GrailsWebRequest.lookup(request).contextPath
            }
        }
    }

    /**
     * Creates a link to a resource, generally used as a method rather than a tag.
     *
     * eg. <link type="text/css" href="${createLinkTo(dir:'css',file:'main.css')}" />
     */
    def createLinkTo = { attrs ->
        GrailsUtil.deprecated "Tag [createLinkTo] is deprecated please use [resource] instead"
        out << resource(attrs)
    }

    /**
     * Creates a link to a resource, generally used as a method rather than a tag.
     *
     * eg. <link type="text/css" href="${resource(dir:'css',file:'main.css')}" />
     */
    def resource = { attrs ->
		def writer = out
		writer << handleAbsolute(attrs)
        def dir = attrs['dir']
		if(attrs.plugin) {
			writer << pluginManager.getPluginPath(attrs.plugin) ?: ''
		}
        else {
            if(attrs.contextPath != null) {
                writer << attrs.contextPath.toString() 
            }
            else {
                def pluginContextPath = pageScope.pluginContextPath
                if(dir != pluginContextPath)
                    writer << pluginContextPath ?: ''
            }
        }
        if(dir) {
           writer << (dir.startsWith("/") ?  dir : "/${dir}")
        }
        def file = attrs['file']
        if(file) {
           writer << (file.startsWith("/") || dir?.endsWith('/') ?  file : "/${file}")
        }
    }

    /**
     *  General linking to controllers, actions etc. Examples:
     *
     *  <g:link action="myaction">link 1</gr:link>
     *  <g:link controller="myctrl" action="myaction">link 2</gr:link>
     */
    def link = { attrs, body ->

        def writer = getOut()
        def elementId = attrs.remove('elementId')
        writer <<  "<a href=\"${createLink(attrs).encodeAsHTML()}\""
        if(elementId) {
           writer << " id=\"${elementId}\""
        }

        writer << "${attrs.collect {k, v -> " $k=\"$v\"" }.join('')}>"
        writer << "${body()}</a>"
    }


    /**
     * Creates a grails application link from a set of attributes. This
     * link can then be included in links, ajax calls etc. Generally used as a method call
     * rather than a tag eg.
     *
     *  <a href="${createLink(action:'list')}">List</a>
     */
    def createLink = { attrs ->
    	def writer = getOut()

    	// prefer URI attribute
		if(attrs['uri']) {
			writer << handleAbsolute(attrs)
			writer << attrs.uri.toString()
		}
		else {
			// prefer a URL attribute
			def urlAttrs = attrs
			if(attrs['url'] instanceof Map) {
				urlAttrs = attrs.remove('url').clone()
			}
			else if(attrs['url']) {
				urlAttrs = attrs.remove('url').toString()
			}
			
			if(urlAttrs instanceof String) {
				if(useJsessionId)
				writer << response.encodeURL(urlAttrs)
				else
				writer << urlAttrs
			}
			else {
				def controller = urlAttrs.containsKey("controller") ? urlAttrs.remove("controller")?.toString() : controllerName
				def action = urlAttrs.remove("action")?.toString()
				if(controller && !action) {
					GrailsControllerClass controllerClass = grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, controller)
					String defaultAction = controllerClass?.getDefaultAction()
					if(controllerClass?.hasProperty(defaultAction))
					action = defaultAction
				}
				def id = urlAttrs.remove("id")
				def frag = urlAttrs.remove('fragment')?.toString()
				def params = urlAttrs.params && urlAttrs.params instanceof Map ? urlAttrs.remove('params') : [:]
				params.mappingName = urlAttrs.remove('mapping')
				if(request['flowExecutionKey']) {
					params."execution" = request['flowExecutionKey']
				}
				
				if(urlAttrs.event) {
					params."_eventId" = urlAttrs.remove('event')
				}
				def url
				if(id != null) params.id = id
				def urlMappings = applicationContext.getBean("grailsUrlMappingsHolder")
				UrlCreator mapping = urlMappings.getReverseMapping(controller,action,params)
				
				// cannot use jsessionid with absolute links
				if(useJsessionId && !attrs.absolute) {
					url = mapping.createURL(controller, action, params, request.characterEncoding, frag)
					def base = attrs.remove('base')
					if(base) writer << base
					writer << response.encodeURL(url)
				}
				else {
					url = mapping.createRelativeURL(controller, action, params, request.characterEncoding, frag)
					out << handleAbsolute(attrs)
					writer << url
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
		def writer = out
		writer << "<${attrs.name}"
		if(attrs.attrs) {
			attrs.attrs.each{ k,v ->
				if(v) {
					if(v instanceof Closure) {
						writer << " $k=\""
					    v()
						writer << '"'
					}
					else {
						writer << " $k=\"$v\""
					}
				}
			}
		}
		writer << '>'
		writer << body()
		writer << "</${attrs.name}>"
	}

	/**
	 * Output application metadata that is loaded from application.properties
	 */
    def meta = { attrs ->
        out << Metadata.current[attrs.name]
    }

}
