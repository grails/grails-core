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
 * An tag library that contains tags to help rendering of views and layouts
 *
 * @author Graeme Rocher
 * @since 17-Jan-2006
 */
import org.springframework.validation.Errors;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.servlet.support.RequestContextUtils as RCU;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;

class RenderTagLib implements com.opensymphony.module.sitemesh.RequestConstants {

    protected getPage() {
    	return request[PAGE]
    }
    
    /**
     * Used to retrieve a property of the decorated page
     *
     * <g:pageProperty default="defaultValue" name="body.onload" />
     */    
    def pageProperty = { attrs ->
            if(!attrs.name) {
	            throwTagError("Tag [pageProperty] is missing required attribute [name]")
            }    
            
            def htmlPage = getPage()

            String propertyValue = htmlPage.getProperty(attrs.name)

            if (!propertyValue)
                propertyValue = attrs.'default';

            if (propertyValue) {
                if (attrs.writeEntireProperty) {
                    out << ' '
                    out << propertyName.substring(propertyName.lastIndexOf('.') + 1)
                    out << "=\"${propertyValue}\""
                }
                else {
                    out << propertyValue
                }
            }    
    }
	
	/**
	 * Invokes the body of this tag if the page property exists:
	 * 
	 * <g:ifPageProperty name="meta.index">body to invoke</g:ifPageProperty>
	 *
	 * of it equals a certain value:
	 *
	 *<g:ifPageProperty name="meta.index" equals="blah">body to invoke</g:ifPageProperty>
	 */ 
	def ifPageProperty = { attrs, body ->
		if(attrs.name) {
			def htmlPage = getPage()
			def names = ((attrs.name instanceof List) ? attrs.name : [attrs.name])
			
			
			def invokeBody = true
			for(i in 0..<names.size()) {
				String propertyValue = htmlPage.getProperty(names[i])
				if(propertyValue) {
					if(attrs.equals instanceof List) {
						invokeBody = (attrs.equals[i]==propertyValue)
					}
					else if(attrs.equals instanceof String) {
						invokeBody = (attrs.equals == propertyValue)	
					}
				}
				else {
					invokeBody = false
					break	
				}
			}
			if(invokeBody) {
				body();	
			}
		}
	}
    /**
     * Used in layouts to render the page title from the SiteMesh page
     *
     * <g:layoutTitle default="The Default title" />
     */
	def layoutTitle = { attrs ->
        String title = page.title
        if (!title && attrs.'default') title = attrs.'default'
        if (title) out << title;		
	}
	
    /**
     * Used in layouts to render the body of a SiteMesh layout
     *
     * <g:layoutBody />
     */	
	def layoutBody = { attrs ->
		getPage().writeBody(out)
	}
	
    /**
     * Used in layouts to render the head of a SiteMesh layout
     *
     * <g:layoutHead />
     */		
	def layoutHead = { attrs ->
		getPage().writeHead(out)	
	}
	
	
	/**
	 * Creates next/previous links to support pagination for the current controller
	 *
	 * <g:paginate total="${Account.count()}" />
	 */
	def paginate = { attrs ->
        if(attrs.total == null)
            throwTagError("Tag [paginate] is missing required attribute [total]")
		
		def mkp = new groovy.xml.MarkupBuilder(out)
		def total = attrs.total.toInteger()
		def max = params.max?.toInteger()
		def offset = params.offset?.toInteger() 
		def action = (attrs.action? attrs.action : 'list')
		def breadcrumb = true
		if(attrs.breadcrumb) breadcrumb = Boolean.valueOf(attrs.breadcrumb)
			
		if(!max) max = (attrs.max ? attrs.max.toInteger() : 10)
		if(!offset) offset = (attrs.offset ? attrs.offset.toInteger() : 0)
		
		def linkParams = [offset:offset-10,max:max]
		def linkTagAttrs = ['class':'prevLink',action:action]
		if(attrs.controller) {
			linkTagAttrs.controller = attrs.controller	
		}
		if(attrs.id) {
			linkTagAttrs.id = attrs.id	
		}
		if(attrs.params)linkParams.putAll(attrs.params)
		linkTagAttrs.params = linkParams
	
		def combined = max + offset
		if(offset > 0) {			
			link(linkTagAttrs.clone(),{out<< (attrs.prev? attrs.prev : 'Previous' ) })
		}
		
		if(total > max) {
			linkTagAttrs.'class' = 'step'
			if(breadcrumb) {
				def j = 0
				0.step(total,max) { i ->
					if(offset == i) {
						mkp.a('class':'step',"${++j}")	
					}
					else {
						linkParams.offset=i
						link(linkTagAttrs.clone(),{out<<++j})	
					}
				}			
			}			
		}
		linkParams.offset = offset+10
		if(combined < total) {	
			linkTagAttrs.'class'='nextLink'			
			link(linkTagAttrs,{out<< (attrs.'next'? attrs.'next' : 'Next' )})			
		}

	}

    /**
     *  allows rendering of templates inside views for collections, models and beans. Examples:
     *
     *  <g:render template="atemplate" collection="${users}" />
     *  <g:render template="atemplate" model="[user:user,company:company]" />
     *  <g:render template="atemplate" bean="${user}" />
     */
    def render = { attrs, body ->
        if(!attrs.template)
            throwTagError("Tag [render] is missing required attribute [template]")

        def engine = grailsAttributes.getPagesTemplateEngine()
        def uri = grailsAttributes.getTemplateUri(attrs.template,request)
        def var = attrs['var']

        def url = servletContext.getResource(uri)
        if(!url)
            throwTagError("No template found for name [${attrs.template}] in tag [render]")

        def t = engine.createTemplate(  uri,
                                        servletContext,
                                        request,
                                        response)

        if(attrs.model instanceof Map) {
            t.make( attrs.model ).writeTo(out)
        }
        else if(attrs.collection) {
            attrs.collection.each {
            	if(var) {
            		def b = [:]
            		b.put(var, it)
            		t.make(b).writeTo(out)
            	}
            	else {
	                t.make( ['it': it] ).writeTo(out)
	            }
            }
        }
        else if(attrs.bean) {
        	if(var) {
        		def b = [:]
        		b.put(var, attrs.bean)
        		t.make(b).writeTo(out)
        	}
        	else {        
	            t.make( [ 'it' : attrs.bean ] ).writeTo(out)
	        }
        }
		else if(attrs.template && attrs.size() == 1) {
			t.make().writeTo(out)			
		}
    }

	/**
	 * Used to include templates
	 */
	def include = { attrs ->
		if(attrs.template) {
	        def engine = grailsAttributes.getPagesTemplateEngine()
	        def uri = grailsAttributes.getTemplateUri(attrs.template,request)

	        def url = servletContext.getResource(uri)
	        if(!url)
	            throwTagError("No template found for name [${attrs.template}] in tag [include]")

	        def t = engine.createTemplate(  uri,
	                                        servletContext,
	                                        request,
	                                        response)
			
			t.make().writeTo(out)
		}
	}

    /**
     * Attempts to render input for a property value based by attempting to choose a rendering component
     * to use based on the property type
     */
    def renderInput = { attrs, body ->
        def bean = attrs['bean']
        if(!bean) {
            throwTagError("Tag [renderInput] is missing required attribute [bean]")
        }
        if(!attrs['property']) {
            throwTagError("Tag [renderInput] is missing required attribute [property]")
        }

       def app = grailsAttributes.getGrailsApplication()
       def dc = app.getGrailsDomainClass(bean.class.name)
       def pv = bean.metaPropertyValues.find {
            it.name == attrs['property']
       }
       if(!pv) {
          throwTagError("Property [${attrs['property']}] does not exist in tag [renderInput] for bean [${bean}]")
       }
       def engine = grailsAttributes.getPagesTemplateEngine()
       def uri = findUriForType(pv.type)

       if(!uri)
            throwTagError("Type [${pv.type}] is unsupported by tag [renderInput]. No template found.")

       def t = engine.createTemplate(   uri,
                                        servletContext,
                                        request,
                                        response)
       if(!t) {
            throwTagError("Type [${pv.type}] is unsupported by tag [renderInput]. No template found.")
       }

       def binding = [ name:pv.name,value:pv.value]
       binding['constraints'] = (dc ? dc.constrainedProperties : null)

       t.make(binding).writeTo(out)
    }

    private String findUriForType(type) {
        if(type == Object.class)
            return null;
        def uri = "/WEB-INF/internal/render/${type.name}.gsp";
        def url = servletContext.getResource(uri)

        if(url != null) {
            return uri
        }
        else {
            return findUriForType(type.superClass)
        }
    }	
}