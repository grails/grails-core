/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.mapping;


import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource

/**
* A plug-in that handles the configuration of URL mappings for Grails
*
* @author Graeme Rocher
* @since 0.4
*/
class UrlMappingsGrailsPlugin {

	def watchedResources = ["file:./grails-app/conf/*UrlMappings.groovy"]

	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [core:version]
                                
	def doWithSpring = {
		grailsUrlMappingsHolderBean(UrlMappingsHolderFactoryBean) {
            grailsApplication = ref("grailsApplication", true)
        }
        urlMappingsTargetSource(org.springframework.aop.target.HotSwappableTargetSource, grailsUrlMappingsHolderBean)
        grailsUrlMappingsHolder(ProxyFactoryBean) {
            targetSource = urlMappingsTargetSource
            proxyInterfaces = [org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder]
        }
	}

    def doWithApplicationContext = { ApplicationContext ctx ->
        def beans = beans(doWithSpring)
        beans.registerBeans(ctx)        	
	}

	def doWithWebDescriptor = { webXml ->
        def filters = webXml.filter
        def lastFilter = filters[filters.size()-1]
        lastFilter + {
            filter {
                'filter-name'('urlMapping')
                'filter-class'(org.codehaus.groovy.grails.web.mapping.filter.UrlMappingsFilter.getName())
            }            
        }
        // here we augment web.xml with all the error codes contained within the UrlMapping definitions
        def servlets = webXml.servlet
        def lastServlet = servlets[servlets.size()-1]

        lastServlet + {
            'servlet' {
                'servlet-name'("grails-errorhandler")
                'servlet-class'(org.codehaus.groovy.grails.web.servlet.ErrorHandlingServlet.getName())
            }
        }

        def servletMappings = webXml.'servlet-mapping'
        def lastMapping = servletMappings[servletMappings.size()-1]
        lastMapping + {
            'servlet-mapping' {
                'servlet-name'("grails-errorhandler")
                'url-pattern'("/grails-errorhandler")
            }
        }
        def welcomeFileList = webXml.'welcome-file-list'
        def errorPages = {
                 for(Resource r in watchedResources) {
                        r.file.eachLine { line ->
                           def matcher = line =~ /\s*"(\d+?)"\(.+?\)/
                           if(matcher) {
                              def errorCode = matcher[0][1]
                               'error-page' {
                                   'error-code'(errorCode)
                                   'location'("/grails-errorhandler")
                               }

                           }
                        }
                 }
            }
        if(welcomeFileList.size() > 0) {
            welcomeFileList = welcomeFileList[welcomeFileList.size()-1]
            welcomeFileList + errorPages
        }
        else {
            lastMapping +  errorPages
        }

        def filterMappings = webXml.'filter-mapping'
        def lastFilterMapping = filterMappings[filterMappings.size() - 1]

        lastFilterMapping + {
            'filter-mapping' {
                'filter-name'('urlMapping')
                'url-pattern'("/*")
            }
        }
    }
	
	def onChange = { event ->
	    if(application.isUrlMappingsClass(event.source)) {
	        application.addArtefact( UrlMappingsArtefactHandler.TYPE, event.source )	        

			def factory = new UrlMappingsHolderFactoryBean(grailsApplication:application)
			factory.afterPropertiesSet()
			def mappings = factory.getObject()

			HotSwappableTargetSource ts = event.ctx.getBean("urlMappingsTargetSource")
			ts.swap mappings			
        }
	}
}