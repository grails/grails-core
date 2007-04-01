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
	def out // to facilitate testing

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
		
		def messageSource = grailsAttributes.getApplicationContext().getBean("messageSource")
		def locale = RCU.getLocale(request) 
		
		def total = attrs.total.toInteger()
		def action = (attrs.action? attrs.action : 'list')
		def offset = params.offset?.toInteger()
		def max = params.max?.toInteger()
		def maxsteps = (attrs.maxsteps ? attrs.maxsteps.toInteger() : 10)

        if(attrs.breadcrumb) {
			log.warn("Tag [paginate] includes the [breadcrumb] attribute. This attribute is deprecated and will be removed in the future. Please update your code to use the [maxsteps] attribute instead.")
		}

		if(!offset) offset = (attrs.offset ? attrs.offset.toInteger() : 0)			
		if(!max) max = (attrs.max ? attrs.max.toInteger() : 10)
		
		def linkParams = [offset:offset - max, max:max]
		if(params.sort) linkParams.sort = params.sort
		if(params.order) linkParams.order = params.order
		if(attrs.params) linkParams.putAll(attrs.params)
		
		def linkTagAttrs = [action:action]
		if(attrs.controller) {
			linkTagAttrs.controller = attrs.controller	
		}
		if(attrs.id) {
			linkTagAttrs.id = attrs.id	
		}
		linkTagAttrs.params = linkParams
		
		// determine paging variables
		def steps = maxsteps > 0
		int currentstep = (offset / max) + 1
		int firststep = 1
		int laststep = Math.round(Math.ceil(total / max))
			
		// display previous link when not on firststep
		if(currentstep > firststep) {
			linkTagAttrs.class = 'prevLink'
			link(linkTagAttrs.clone(), {out << (attrs.prev ? attrs.prev : messageSource.getMessage('default.paginate.prev', null, 'Previous', locale))})
		}
		
		// display steps when steps are enabled and laststep is not firststep
		if(steps && laststep > firststep) {
			linkTagAttrs.class = 'step'

			// determine begin and endstep paging variables
			int beginstep = currentstep - Math.round(maxsteps / 2) + (maxsteps % 2)
			int endstep = currentstep + Math.round(maxsteps / 2) - 1
			
			if(beginstep < firststep) {
				beginstep = firststep
				endstep = maxsteps
			}
			if(endstep > laststep) {
				beginstep = laststep - maxsteps + 1
				if(beginstep < firststep) {
					beginstep = firststep
				}
				endstep = laststep
			}

			// display firststep link when beginstep is not firststep
			if(beginstep > firststep) {
				linkParams.offset = 0
				link(linkTagAttrs.clone(), {out << firststep})
				out << '<span class="step">..</span>'
			}

			// display paginate steps
			(beginstep..endstep).each { i ->
				if(currentstep == i) {
					out << "<span class=\"currentStep\">${i}</span>"
				}
				else {
					linkParams.offset = (i - 1) * max
					link(linkTagAttrs.clone(), {out << i})
				}
			}	
			
			// display laststep link when endstep is not laststep
			if(endstep < laststep) {
				out << '<span class="step">..</span>'
				linkParams.offset = (laststep -1) * max
				link(linkTagAttrs.clone(), {out << laststep})
			}		
		}
		
		// display next link when not on laststep
		if(currentstep < laststep) {	
			linkTagAttrs.class = 'nextLink'			
			linkParams.offset = offset + max
			link(linkTagAttrs.clone(), {out << (attrs.next ? attrs.next : messageSource.getMessage('default.paginate.next', null, 'Next', locale))})			
		}

	}

	/**
	 * Renders a sortable column to support sorting in list views
	 *
	 * Attributes:
	 *
	 * property - name of the property relating to the field, including dotted notation for nested properties
	 * defaultOrder (optional) - default order for the property; choose between asc (default if not provided) and desc
	 * title (optional*) - title caption for the column
	 * titleKey (optional*) - title key to use for the column, resolved against the message source
	 * params (optional) - a map containing request parameters
	 *
	 * Attribute title or titleKey is required. When both attributes are specified then titleKey takes precedence,
	 * resulting in the title caption to be resolved against the message source. In case when the message could
	 * not be resolved, the title will be used as title caption. 
	 *
	 * Examples:
	 *
	 * <g:sortableColumn property="title" title="Title" />
	 * <g:sortableColumn property="title" title="Title" style="width: 200px" />
	 * <g:sortableColumn property="title" titleKey="book.title" />	 
	 * <g:sortableColumn property="releaseDate" defaultOrder="desc" title="Release Date" />
	 * <g:sortableColumn property="releaseDate" defaultOrder="desc" title="Release Date" titleKey="book.releaseDate" />
	 */
	def sortableColumn = { attrs ->

		if(!attrs.property)
			throwTagError("Tag [sortableColumn] is missing required attribute [property]") 
		
		if(!attrs.title && !attrs.titleKey)
			throwTagError("Tag [sortableColumn] is missing required attribute [title] or [titleKey]")

		def property = attrs.remove("property")
		def action = attrs.action ? attrs.remove("action") : "list"
		
		def defaultOrder = attrs.remove("defaultOrder")
		if(defaultOrder != "desc") defaultOrder = "asc"

		// current sorting property and order
		def sort = params.sort
		def order = params.order

		// add sorting property and params to link params
		def linkParams = [sort:property]
		if(attrs.params) linkParams.putAll(attrs.params)
		
		// determine and add sorting order for this column to link params
		attrs.class = "sortable"
		if(property == sort) {
			attrs.class = attrs.class + " sorted " + order
			if(order == "asc") {
				linkParams.order = "desc"
			}
			else {
				linkParams.order = "asc"
			}
		}
		else {
			linkParams.order = defaultOrder
		}

		// determine column title
		def title = attrs.remove("title")
		def titleKey = attrs.remove("titleKey")
		if(titleKey) {
			if(!title) title = titleKey
			def messageSource = grailsAttributes.getApplicationContext().getBean("messageSource")
			def locale = RCU.getLocale(request)
			title = messageSource.getMessage(titleKey, null, title, locale)
		}

		out << "<th "
		// process remaining attributes
		attrs.each { k, v ->
			out << k << "=\"" << v.encodeAsHTML() << "\" "
		}
		out << ">"

		// output link
		link([action:action, params:linkParams], { out << title })

		out << "</th>"
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

        def t = engine.createTemplate( uri )

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

	        def t = engine.createTemplate(  uri )
			
			t.make().writeTo(out)
		}
	}	
}