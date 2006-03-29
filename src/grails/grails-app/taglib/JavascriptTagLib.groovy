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
class JavascriptTagLib extends ApplicationTagLib  {

	/**
	 * Mappings to the relevant files to be included for each library
	 */
	static final INCLUDED_LIBRARIES = "org.codehaus.grails.INCLUDED_JS_LIBRARIES"
	static final INCLUDED_JS = "org.codehaus.grails.INCLUDED_JS"	
	static final LIBRARY_MAPPINGS = [
										prototype : ['prototype'],
										yahoo : [ 'YAHOO','connect', 'dom','event','animation']										
									]
										
	static {
		LIBRARY_MAPPINGS.scriptaculous = LIBRARY_MAPPINGS.prototype + ['scriptaculous','builder','controls','effects','slider','draganddrop']		
		LIBRARY_MAPPINGS.rico = LIBRARY_MAPPINGS.prototype + ['rico']				
	}									
	/**
	 * Includes a javascript src file, library or inline script
	 **/
	@Property javascript = { attrs, body ->
		if(!request[INCLUDED_JS]) request[INCLUDED_JS] = []
		if(!request[INCLUDED_LIBRARIES]) request[INCLUDED_LIBRARIES] = []
		
		if(attrs.src) {
			out << '<script type="text/javascript" src="'
			out << grailsAttributes.getApplicationUri(request)
			out << "/js/${attrs.src}"
			out.println '"></script>'		
		}
		else if(attrs.library) {

			if(LIBRARY_MAPPINGS.containsKey(attrs.library)) {
				if(!request[INCLUDED_LIBRARIES].contains(attrs.library)) {
					LIBRARY_MAPPINGS[attrs.library].each {
						if(!request[INCLUDED_JS].contains(it)) {
								request[INCLUDED_JS] << it
								out << '<script type="text/javascript" src="'
								out << grailsAttributes.getApplicationUri(request)
								out << "/js/${it}.js"							
								out.println '"></script>'
						}					
					}
					request[INCLUDED_LIBRARIES] << attrs.library					
				}				
			}
			else {
				if(!request[INCLUDED_LIBRARIES].contains(attrs.library)) {
					out << '<script type="text/javascript" src="'
					out << grailsAttributes.getApplicationUri(request)
					out << "/js/${attrs.library}.js"						
					out.println '"></script>'
					request[INCLUDED_LIBRARIES] << attrs.library
					request[INCLUDED_JS] << attrs.library					
				}
			}
		}
		else {
			out.println '<script type="text/javascript">'
				body()
			out.println '</script>'
		}
	}
    /**
     *  Creates a remote function call using the prototype library
     */
    @Property remoteFunction = { attrs  ->    
		def isPrototype = request[JavascriptTagLib.INCLUDED_LIBRARIES].contains('prototype');
		def isYahoo = request[JavascriptTagLib.INCLUDED_LIBRARIES].contains('yahoo');
		
		if(!request[JavascriptTagLib.INCLUDED_LIBRARIES] || (!isYahoo && !isPrototype)) {
			out << 'function() { alert("Grails Message: The remoteFunction tag requires the \'prototype\' or \'yahoo\' libraries to be included with the <g:javascript library=\'prototype\' /> tag"); }'		
		}    
		else {
	        // before remote function
	        def after = ''
	        if(attrs["before"])
	            out << "${attrs.remove('before')};"
	        if(attrs["after"])
	            after = "${attrs.remove('after')};"
	
			// if the prototype library is included use it
			if(isPrototype) {
				out << 'new Ajax.'
				if(attrs["update"]) {
					out << 'Updater('
					if(attrs["update"] instanceof Map) {
						out << "{"
						def update = []
						if(attrs["update"]["success"]) {
							update << "success:'${attrs['update']['success']}'"
						}
						if(attrs["update"]["failure"]) {
							update << "failure:'${attrs['update']['failure']}'"
						}
						out << update.join(',')
						out << "},"
					}
					else {
						out << "'" << attrs["update"] << "',"
					}
					attrs.remove("update")
				}
				else {
					out << "Request("
				}
				out << "'"				
			}
			// otherwise try yahoo
			else if(isYahoo) {
				out.println 'function() {'
				buildYahooCallback(attrs)							
				def method = (attrs.method ? attrs.method : 'POST' )
				out << "YAHOO.util.Connect.asyncRequest('${method}','"
			}
	

	        if(attrs['url']) {
	            createLink(attrs['url'])
	        }
	        else {
	            createLink(attrs)
	        }
	
			if(isPrototype) {
				out << "',"
		
				// process options
				out << getAjaxOptions(attrs)
				// close
				out << ');'
			}
			else if(isYahoo) {
				out << "',callback,null); }"
			}
	
	        // after remote function
	        if(after)
	           out <<  after		
		}

    }

	// helper method to create yahoo callback object
	def buildYahooCallback(attrs) {
		out.println ''' var callback = {
			success: function(o) {'''
			if(attrs.update) {
				if(attrs.update instanceof Map) {
					if(attrs.update.sucess) {
						out.println "document.getElementById('${attrs.update.success}').innerHTML = o.responseText;"									
					}								
				}
				else {
					out.println "document.getElementById('${attrs.update}').innerHTML = o.responseText;"
				}
			}
			if(attrs.onSuccess) {
				out.println "${attrs.onSuccess}(o);"
			}							

			out << '''},
			failure: function(o) {'''
			if(attrs.update && attrs.update.failure) {
				out.println "document.getElementById('${attrs.update.failure}').innerHTML = o.responseText;"							
			}
			if(attrs.onFailure) {
				out.println "${attrs.onFailure}(o);"
			}											
			out.println '}'
			if(attrs.params) {
					// todo add arguments
			}
			out.println '}'		
	}
    // helper function to build ajax options
    def getAjaxOptions(options) {
        def ajaxOptions = []

        if(options) {
            // process callbacks
            def callbacks = options.findAll { k,v ->
                k ==~ /on(\p{Upper}|\d){1}\w+/
            }
            callbacks.each { k,v ->
                ajaxOptions << "${k}:function(e){${v}}"
                options.remove(k)
            }

            // necessary defaults
            if(options['asynchronous'])
                ajaxOptions << "asynchronous:${options.remove('asynchronous')}"
            else
                ajaxOptions << "asynchronous:true"


            if(options['evalScripts'])
                ajaxOptions << "evalScripts:${options.remove('evalScripts')}"
            else
                ajaxOptions << "evalScripts:true"

            if(options['params']) {
                ajaxOptions << "parameters:${options.remove('parameters')}"
            }
            // remaining options
            options.each { k, v ->
                 switch(v) {
                    case 'true': ajaxOptions << "${k}:${v}"; break;
                    case 'false': ajaxOptions << "${k}:${v}"; break;
                    case ~/\s*function(\w*)\s*/: ajaxOptions << "${k}:${v}"; break;
                    default:ajaxOptions << "${k}:'${v}'"; break;
                 }
            }
        }
        // set defaults
        else {
             ajaxOptions << "asynchronous:true"
             ajaxOptions << "evalScripts:true"
        }

        return "{${ajaxOptions.join(',')}}"
    }

    /**
     * A link to a remote uri that used the prototype library to invoke the link via ajax
     */
    @Property remoteLink = { attrs, body ->
       out << "<a href=\"#\" onclick=\""
        // create remote function
        remoteFunction(attrs)
        out << 'return false;" '
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
     * A form which used prototype to serialize its parameters and submit via an asynchronous ajax call
     */
    @Property formRemote = { attrs, body ->
		def isPrototype = request[JavascriptTagLib.INCLUDED_LIBRARIES].contains('prototype');
		def isYahoo = request[JavascriptTagLib.INCLUDED_LIBRARIES].contains('yahoo');
		
		if(!request[JavascriptTagLib.INCLUDED_LIBRARIES] || (!isYahoo && !isPrototype)) {
			out << 'function() { alert("Grails Message: The remoteFunction tag requires the \'prototype\' or \'yahoo\' libraries to be included with the <g:javascript library=\'prototype\' /> tag"); }'		
		}    
		else {	
			if(isPrototype) {				
			   attrs['parameters'] = "Form.serialize(this)"
			   out << '<form onsubmit="' << remoteFunction(attrs) << ';return false;" '
			   out << 'method="' <<  (attrs['method'] ? attrs['method'] : 'post') << '" '
			   out << 'action="' <<  (attrs['action'] ? attrs['action'] : createLink(attrs['url'])) << '">'
		
				// output body
				   body()
				// close tag
			   out << '</form>'
		    }
			else if(isYahoo) {		
				def url = outToString(createLink,attrs)
				def onsubmit = []
				if(attrs.before) {
					onsubmit << attrs.before	
				}
				onsubmit << "${attrs.name}RemoteFunction()"
				if(attrs.after) {
					onsubmit << attrs.after	
				}
				onsubmit << "return false;"
				
				withTag(name:'form',
						attrs: [ 	name: attrs.name,
									onsubmit: onsubmit.join(';'),
									method: (attrs.method ? attrs.method : 'post'),
									action: (attrs.action ? attrs.action : url)
								] ) {
					body()
				}
				withTag(name:'script',attrs:[type:'text/javascript']) {
					out << """
					function ${attrs.name}RemoteFunction() {						
						YAHOO.util.Connect.setForm('${attrs.name}');"""
					buildYahooCallback(attrs)
					out <<	"var cObj = YAHOO.util.Connect.asyncRequest('POST', '$url', callback);}"					
				}				
			}
		}
    }

	/**
	 * Helper method for outputting to a string instead of the the output write
	 */
	def outToString(tag,attrs) {
		def saveOut = out
		def sw = new StringWriter()
		def result = null
		try {
			this.out = new PrintWriter(sw)
			tag(attrs)
		}
		finally {
			out = saveOut;
		}
		return sw.toString()
	}
	/**
	 * Helper method for creating tags
	 */
	def withTag = { params, body ->
		out << "<${params.name}"
		if(params.attrs) {
			params.attrs.each{ k,v ->
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
		out << '>'
		body()
		out << "</${params.name}>"			
	}
    /**
     *  Creates a form submit button that submits the current form to a remote ajax call
     */
    @Property submitToRemote = { attrs, body ->
        attrs['parameters'] = "Form.serialize(this.form)"
        out << "<input type='button' name='${attrs.remove('name')}' value='${attrs.remove('value')}' "
        out << 'onclick="'
        remoteFunction(attrs)
        out << ';return false;" />'
    }

}