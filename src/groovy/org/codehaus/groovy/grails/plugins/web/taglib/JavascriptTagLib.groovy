package org.codehaus.groovy.grails.plugins.web.taglib

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
import org.springframework.validation.Errors;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.servlet.support.RequestContextUtils as RCU;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;

 /**
 *  A  tag lib that provides tags for developing javascript and ajax applications
 *
 * @author Graeme Rocher
 * @since 17-Jan-2006
 */
class JavascriptTagLib  {

	/**
	 * Mappings to the relevant files to be included for each library
	 */
	static final INCLUDED_LIBRARIES = "org.codehaus.grails.INCLUDED_JS_LIBRARIES"
	static final INCLUDED_JS = "org.codehaus.grails.INCLUDED_JS"
    static final CONTROLLER = "org.codehaus.groovy.grails.CONTROLLER"
	static final LIBRARY_MAPPINGS = [
										prototype : ['prototype/prototype']
									]
										
	static {
		LIBRARY_MAPPINGS.scriptaculous = LIBRARY_MAPPINGS.prototype + ['prototype/scriptaculous','prototype/builder','prototype/controls','prototype/effects','prototype/slider','prototype/dragdrop']		
		LIBRARY_MAPPINGS.rico = LIBRARY_MAPPINGS.prototype + ['prototype/rico']				
	}
	
	static final PROVIDER_MAPPINGS = [
	                                     prototype: PrototypeProvider.class,
	                                     scriptaculous: PrototypeProvider.class,
	                                     rico: PrototypeProvider.class
                                  	 ]

	/**
	 * Includes a javascript src file, library or inline script
	 * if the tag has no 'src' or 'library' attributes its assumed to be an inline script:
	 *
	 * <g:javascript>alert('hello')</g:javascript>
	 *
	 * The 'library' attribute will attempt to use the library mappings defined above to import the 
	 * right js files and not duplicate imports eg.
	 *
	 * <g:javascript library="scripaculous" /> // imports all the necessary js for the scriptaculous library
	 *
	 * The 'src' attribute will merely import the js file but within the right context (ie inside the /js/ directory of 
	 * the Grails application:
	 *
	 * <g:javascript src="myscript.js" /> // actually imports '/app/js/myscript.js'
	 **/
	def javascript = { attrs, body ->
		setUpRequestAttributes();
        def requestPluginContext = request[CONTROLLER]?.pluginContextPath
		if(attrs.src) {
            javascriptInclude(attrs)
		}
		else if(attrs.library) {

			if(LIBRARY_MAPPINGS.containsKey(attrs.library)) {
				if(!request[INCLUDED_LIBRARIES].contains(attrs.library)) {
					LIBRARY_MAPPINGS[attrs.library].each {
						if(!request[INCLUDED_JS].contains(it)) {
							request[INCLUDED_JS] << it
							def newattrs = [:] + attrs
							newattrs.src = it+'.js'
							javascriptInclude(newattrs)
					    }
					}
					request[INCLUDED_LIBRARIES] << attrs.library
				}
			}
			else {
				if(!request[INCLUDED_LIBRARIES].contains(attrs.library)) {
					def newattrs = [:] + attrs
					newattrs.src = newattrs.remove('library')+'.js'
					javascriptInclude(newattrs)
					request[INCLUDED_LIBRARIES] << attrs.library
					request[INCLUDED_JS] << attrs.library					
				}
			}
		}
		else {
			out.println '<script type="text/javascript">'
			out.println body()
			out.println '</script>'
		}
	}

	private javascriptInclude(attrs) {
    	def requestPluginContext = request[CONTROLLER]?.pluginContextPath
		out << '<script type="text/javascript" src="'
		if (!attrs.base) {
            def baseUri = grailsAttributes.getApplicationUri(request)
            out << baseUri
            out << (baseUri.endsWith('/') ? '' : '/')
			if (requestPluginContext) {
			  out << (requestPluginContext.startsWith("/") ? requestPluginContext.substring(1) : requestPluginContext)
			  out << "/"
			} 
			out << 'js/'
		} else {
			out << attrs.base
		}
		out << attrs.src
 		out.println '"></script>'
	}
		
    /**
     *  Creates a remote function call using the prototype library
     */
    def remoteFunction = { attrs  ->    
		// before remote function
		def after = ''
		if(attrs["before"])
			out << "${attrs.remove('before')};"
		if(attrs["after"])
			after = "${attrs.remove('after')};"

		def p = getProvider()
		p.doRemoteFunction(owner,attrs,out)
		attrs.remove('update')
		// after remote function
		if(after)
		   out <<  after			           		   
    }

    private setUpRequestAttributes(){
        if(!request[INCLUDED_JS]) request[INCLUDED_JS] = []
		if(!request[INCLUDED_LIBRARIES]) request[INCLUDED_LIBRARIES] = []
    }
    /**
     * Normal map implementation does a shallow clone. This implements a deep clone for maps
     * using recursion
     */
    private deepClone(Map map) {
	    def cloned = [:]
	    map?.each { k,v ->
		    if(v instanceof Map) {
			   cloned[k] = deepClone(v)
   	        }
            else {
		     cloned[k] = v  		    
		    }
		}                
		return cloned
    }
    /**
     * A link to a remote uri that used the prototype library to invoke the link via ajax
     */
    def remoteLink = { attrs, body ->  
       out << "<a href=\""    

       def cloned = deepClone(attrs)
	   out << createLink(cloned)               

	   out << "\" onclick=\""
        // create remote function
        out << remoteFunction(attrs)   
		attrs.remove('url')
        out << "return false;\""
        // process remaining attributes
        attrs.each { k,v ->
            out << ' ' << k << "=\"" << v << "\""
        }
        out << ">"
        // output the body
        out << body()

        // close tag
        out << "</a>"
    }

	/**
	 * A field that sends its value to a remote link
	 */
	def remoteField = { attrs, body ->
		def paramName = attrs.paramName ? attrs.remove('paramName') : 'value'
		def value = attrs.remove('value') 
		if(!value) value = ''
		
		out << "<input type=\"text\" name=\"${attrs.remove('name')}\" value=\"${value}\" onkeyup=\""
        
        if(attrs.params) {
			if(attrs.params instanceof Map) {
				attrs.params.put(paramName, new JavascriptValue('this.value'))
			}
			else {
				attrs.params += "+'${paramName}='+this.value"	
			}
		}
		else {
    		attrs.params = "'${paramName}='+this.value"
		}
		out << remoteFunction(attrs)
		attrs.remove('params')
		out << "\""   
		attrs.remove('url')
		attrs.each { k,v->
			out << " $k=\"$v\""
		}
		out <<" />"
	}

    /**
     * A form which used prototype to serialize its parameters and submit via an asynchronous ajax call
     */
    def formRemote = { attrs, body ->
        if(!attrs.name) {
            throwTagError("Tag [formRemote] is missing required attribute [name]")
        }        
        if(!attrs.url) {
            throwTagError("Tag [formRemote] is missing required attribute [url]")
        }

        // 'formRemote' does not support the 'params' attribute.
        if(attrs.params != null) {
            throwTagError("""\
Tag [formRemote] does not support the [params] attribute - add\
a 'params' key to the [url] attribute instead.""")
        }
        
        // get javascript provider
 		def p = getProvider()				
		def url = deepClone(attrs.url)
 		
		// prepare form settings
		p.prepareAjaxForm(attrs)
        
        def params = [  onsubmit:remoteFunction(attrs) + 'return false',
					    method: (attrs.method? attrs.method : 'POST' ),
					    action: (attrs.action? attrs.action : createLink(url))		                 
		             ]
		attrs.remove('url')		             
	    params.putAll(attrs)
		if(params.name && !params.id)
			params.id = params.name
	    out << withTag(name:'form',attrs:params) {
			out << body()   
	    }		
    }

    /**
     *  Creates a form submit button that submits the current form to a remote ajax call
     */
    def submitToRemote = { attrs, body ->
    	// get javascript provider
		def p = getProvider()    
		// prepare form settings 
		attrs.forSubmitTag = ".form"
		p.prepareAjaxForm(attrs)    
        def params = [  onclick:remoteFunction(attrs) + 'return false',
					    type: 'button',
					    name: attrs.remove('name'),
					    value: attrs.remove('value'), 
					    id: attrs.remove('id'),
					    'class':attrs.remove('class')
		             ]
		             
		out << withTag(name:'input', attrs:params) {
			out << body()	
		}
    }
	
	/**
	 * Escapes a javasacript string replacing single/double quotes and new lines
	 *
	 * <g:escapeJavascript>This is some "text" to be escaped</g:escapeJavascript>
	 */
	def escapeJavascript = { attrs,body ->
		def js = ''
		if(body instanceof Closure) {
			def tmp = out
			def sw = new StringWriter()
			out = new PrintWriter(out)
			// invoke body
			out << body()
			// restore out
			out = tmp
			js = sw.toString()
			
		}
		else if(body instanceof String) {
			js = body
		}
		else if(attrs instanceof String) {
			js = attrs	
		}
		out << 	js.replaceAll(/\r\n|\n|\r/, '\\\\n')
					.replaceAll('"','\\\\"')
					  .replaceAll("'","\\\\'")
	}
	 
	def setProvider = { attrs, body ->
		if (request[JavascriptTagLib.INCLUDED_LIBRARIES] == null) {
		    request[JavascriptTagLib.INCLUDED_LIBRARIES] = []
		}
		request[JavascriptTagLib.INCLUDED_LIBRARIES] << attrs.library
	}

	/**
	 * Returns the provider of the necessary function calls to perform Javascript functions
	 *
	 **/
	private JavascriptProvider getProvider() {
        setUpRequestAttributes()
        def providerClass = PROVIDER_MAPPINGS.find { request[JavascriptTagLib.INCLUDED_LIBRARIES]?.contains(it.key) }?.value
        if (providerClass == null) {
            providerClass = PrototypeProvider.class
        }
        return providerClass.newInstance()
	}
}
/**
 * Interface defining methods that a JavaScript provider should implement
 *
 * @author Graeme Rocher
 **/
interface JavascriptProvider {
	/**
	 * Creates a remote function call
	 *
	 * @param The attributes to use
	 * @param The output to write to
	 */
	def doRemoteFunction(taglib,attrs, out)
	
	def prepareAjaxForm(attrs)
}

class JavascriptValue {
    def value

    public JavascriptValue(value) {
        this.value = value
    }

    public String toString() {
        return "'+$value+'"
    }


}
/**
 * Prototype implementation of JavaScript provider
 *
 * @author Graeme Rocher
 */
class PrototypeProvider implements JavascriptProvider {
	def doRemoteFunction(taglib,attrs, out) {
		out << 'new Ajax.'
		if(attrs.update) {
			out << 'Updater('
			if(attrs.update instanceof Map) {
				out << "{"
				def update = []
				if(attrs.update?.success) {
					update << "success:'${attrs['update']['success']}'"
				}
				if(attrs.update?.failure) {
					update << "failure:'${attrs['update']['failure']}'"
				}
				out << update.join(',')
				out << "},"
			}
			else {
				out << "'" << attrs.update << "',"
			}
			attrs.remove("update")
		}
		else {
			out << "Request("
		}
		out << "'"						
		
		//def pms = attrs.remove('params')
        def url
        def jsParams = attrs.params?.findAll { it.value instanceof JavascriptValue }

        jsParams?.each { attrs.params?.remove(it.key) }



        if(attrs.url) {
			url = taglib.createLink(attrs.url)
		}
		else {
			url = taglib.createLink(attrs)
		}

        if(!attrs.params) attrs.params = [:]
        jsParams?.each { attrs.params[it.key] = it.value }

        
        def i = url?.indexOf('?')

        if(i >-1) {
            if(attrs.params instanceof String) {
                attrs.params += "+'&${url[i+1..-1].encodeAsJavaScript()}'"                
            }
            else if(attrs.params instanceof Map) {
                def params = createQueryString(attrs.params)
                attrs.params = "'${params}${params ? '&' : ''}${url[i+1..-1].encodeAsJavaScript()}'"
            }
            else {
                attrs.params = "'${url[i+1..-1].encodeAsJavaScript()}'"
            }
            out << url[0..i-1]
        }
        else {
            out << url
        }
        out << "',"
        /* We have removed these currently and are using full URLs to prevent duplication of parameters
            as per GRAILS-2045
        if(pms)
		    attrs.params = pms
		*/
		// process options
		out << getAjaxOptions(attrs)
		// close
		out << ');'
        attrs.remove('params')
    }

    private String createQueryString(params) {
        def allParams = []
        for (entry in params) {
            def value = entry.value
            def key = entry.key
            if (value instanceof JavascriptValue) {
                allParams << "${key.encodeAsURL()}='+${value.value}+'"
            }
            else {
                allParams << "${key.encodeAsURL()}=${value.encodeAsURL()}".encodeAsJavaScript()
            }
        }
        if(allParams.size() == 1) {
            return allParams[0]
        }
        else {
            return allParams.join('&')
        }
    }

    // helper function to build ajax options
    def getAjaxOptions(options) {
        def ajaxOptions = []

       // necessary defaults
       def optionsAttr = options.remove('options')
       def async = optionsAttr?.remove('asynchronous')
       if( async != null)
           ajaxOptions << "asynchronous:${async}"
       else
           ajaxOptions << "asynchronous:true"

       def eval = optionsAttr?.remove('evalScripts')
       if(eval != null)
           ajaxOptions << "evalScripts:${eval}"
       else
           ajaxOptions << "evalScripts:true" 
           
        if(options) {
            // process callbacks
            def callbacks = options.findAll { k,v ->
                k ==~ /on(\p{Upper}|\d){1}\w+/
            }
            callbacks.each { k,v ->
                ajaxOptions << "${k}:function(e){${v}}"
                options.remove(k)
            }
            if(options.params) {
				def params = options.remove('params')
				if (params instanceof Map) {
                    params = createQueryString(params)
				}
                ajaxOptions << "parameters:${params}"
            }
        }
        // remaining options
        optionsAttr?.each { k, v ->
            if(k!='url') {
                switch(v) {
                    case 'true': ajaxOptions << "${k}:${v}"; break;
                    case 'false': ajaxOptions << "${k}:${v}"; break;
                    case ~/\s*function(\w*)\s*/: ajaxOptions << "${k}:${v}"; break;
                    case ~/Insertion\..*/: ajaxOptions << "${k}:${v}"; break;
                    default:ajaxOptions << "${k}:'${v}'"; break;
                }
            }
        }

        return "{${ajaxOptions.join(',')}}"
    }
	
    def prepareAjaxForm(attrs) {
		if(!attrs.forSubmitTag) attrs.forSubmitTag = ""
        
        attrs.params = "Form.serialize(this${attrs.remove('forSubmitTag')})".toString()
    }
}


