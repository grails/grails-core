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
    @Property pageProperty = { attrs ->
            if(!attrs.name) {
	            throwTagError("Tag [pageProperty] is missing required attribute [name]")
            }    
            
            def htmlPage = getPage()

            String propertyValue = htmlPage.getProperty(attrs.name)

            if (!propertyValue)
                propertyValue = attrs.'default';

            if (propertyValue != null) {
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
     * Used in layouts to render the page title from the SiteMesh page
     *
     * <g:layoutTitle default="The Default title" />
     */
	@Property layoutTitle = { attrs ->
        String title = page.title
        if (!title) title = attrs.defaultTitle
        if (title) out << title;		
	}
	
    /**
     * Used in layouts to render the body of a SiteMesh layout
     *
     * <g:layoutBody />
     */	
	@Property layoutBody = { attrs ->
		getPage().writeBody(out)
	}
	
    /**
     * Used in layouts to render the head of a SiteMesh layout
     *
     * <g:layoutHead />
     */		
	@Property layoutHead = { attrs ->
		getPage().writeHead(out)	
	}

    /**
     *  allows rendering of templates inside views for collections, models and beans. Examples:
     *
     *  <g:render template="atemplate" collection="${users}" />
     *  <g:render template="atemplate" model="[user:user,company:company]" />
     *  <g:render template="atemplate" bean="${user}" />
     */
    @Property render = { attrs, body ->
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
    }

    /**
     * Attempts to render input for a property value based by attempting to choose a rendering component
     * to use based on the property type
     */
    @Property renderInput = { attrs, body ->
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