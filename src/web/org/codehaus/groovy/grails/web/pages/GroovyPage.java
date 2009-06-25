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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaProperty;
import groovy.lang.Script;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.taglib.GroovyPageTagWriter;
import org.codehaus.groovy.grails.web.taglib.GroovyPageTagBody;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Base class for a GroovyPage (at the moment there is nothing in here but could be useful for providing utility methods
 * etc.
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 * @author Lari Hotari
 *
 * Date: Jan 10, 2004
 *
 */
public abstract class GroovyPage extends Script {
    private static final Log LOG = LogFactory.getLog(GroovyPage.class);

    public static final String REQUEST = "request";
    public static final String SERVLET_CONTEXT = "application";
    public static final String RESPONSE = "response";
    public static final String OUT = "out";
    public static final String ATTRIBUTES = "attributes";
    public static final String APPLICATION_CONTEXT = "applicationContext";
    public static final String SESSION = "session";
    public static final String PARAMS = "params";
    public static final String FLASH = "flash";
    public static final String PLUGIN_CONTEXT_PATH = "pluginContextPath";
    public static final String EXTENSION = ".gsp";
    public static final String WEB_REQUEST = "webRequest";
    public static final String DEFAULT_NAMESPACE = "g";
    public static final String LINK_NAMESPACE = "link";
    public static final String TEMPLATE_NAMESPACE = "tmpl";
    public static final String PAGE_SCOPE = "pageScope";
    public static final String CONTROLLER_NAME = "controllerName";
    public static final String SUFFIX = ".gsp";
    public static final String ACTION_NAME = "actionName";
    
    public static final Collection<String> RESERVED_NAMES = new ArrayList<String>() {{
        add(REQUEST);
        add(SERVLET_CONTEXT);
        add(RESPONSE);
        add(OUT);
        add(ATTRIBUTES);
        add(APPLICATION_CONTEXT);
        add(SESSION);
        add(PARAMS);
        add(FLASH);
        add(PLUGIN_CONTEXT_PATH);
        add(PAGE_SCOPE);

    }};
    private static final String BINDING = "binding";
    private static final String BLANK_STRING = "";
    private Map jspTags = Collections.EMPTY_MAP;
    private TagLibraryResolver jspTagLibraryResolver;
    private TagLibraryLookup gspTagLibraryLookup;
    private String[] htmlParts;
    
    private static final Closure EMPTY_BODY_CLOSURE = new Closure(null) {
		public Object doCall(Object obj) {
			return BLANK_STRING;
		}
		public Object doCall() {
			return BLANK_STRING;
		}
		public Object doCall(Object[] args) {
			return BLANK_STRING;
		}
		public Object call(Object[] args) {
			return BLANK_STRING;
		}
    };


    public GroovyPage() {
		super();
		init();
	}
    
    protected void init() {
    	
    }

	/**
     * Sets the JSP tag library resolver to use to resolve JSP tags
     * @param jspTagLibraryResolver The JSP tag resolve
     */
    public void setJspTagLibraryResolver(TagLibraryResolver jspTagLibraryResolver) {
        this.jspTagLibraryResolver = jspTagLibraryResolver;
    }

    /**
     * Sets the GSP tag library lookup class
     * @param gspTagLibraryLookup The class used to lookup a GSP tag library
     */
    public void setGspTagLibraryLookup(TagLibraryLookup gspTagLibraryLookup) {
        this.gspTagLibraryLookup = gspTagLibraryLookup;
    }

    /**
     * Obtains a reference to the JSP tag library resolver instance
     * @return The JSP TagLibraryResolver instance
     */
    TagLibraryResolver getTagLibraryResolver() {
        return this.jspTagLibraryResolver;
    }

    /**
     * In the development environment this method is used to evaluate expressions and improve error reporting
     *
     * @param exprText The expression text
     * @param lineNumber The line number
     * @param outerIt The other reference to the variable 'it'
     *
     * @param evaluator The expression evaluator
     * @return The result
     */
    public Object evaluate(String exprText, int lineNumber, Object outerIt, Closure evaluator)  {
        try {
            return evaluator.call(outerIt);
        }
        catch (Exception e) {
            throw new GroovyPagesException("Error evaluating expression ["+exprText+"] on line ["+lineNumber+"]: " + e.getMessage(), e, lineNumber, getGroovyPageFileName());
        }
    }

    public abstract String getGroovyPageFileName();


    public Object getProperty(String property) {
        // in GSP we assume if a property doesn't exist that
        // it is null rather than throw an error this works nicely
        // with the Groovy Truth
        if(BINDING.equals(property)) return getBinding();

        MetaProperty mp = getMetaClass().getMetaProperty(property);
        if(mp!= null)return mp.getProperty(this);

        Object value = getBinding().getVariables().get(property);
        if(value == null) {
            value = gspTagLibraryLookup!=null ? gspTagLibraryLookup.lookupNamespaceDispatcher(property) : null;
        }
        if(value == null && jspTags.containsKey(property)) {
            TagLibraryResolver tagResolver = getTagLibraryResolver();

            String uri = (String) jspTags.get(property);
            if(uri!=null)
                value = tagResolver.resolveTagLibrary(uri);
        }
        return value;
    }


    /**
     * Attempts to invokes a dynamic tag
     *
     * @param tagName The name of the tag
     * @param attrs The tags attributes
     * @param body  The body of the tag as a closure
     */
    public void invokeTag(String tagName, Map attrs, Closure body) {
    	invokeTag(tagName, GroovyPage.DEFAULT_NAMESPACE, attrs, body);
    }

    /**
     * Attempts to invokes a dynamic tag
     *
     * @param tagName The name of the tag
     * @param tagNamespace The taglib's namespace
     * @param attrs The tags attributes
     * @param body  The body of the tag as a closure
     */
    public void invokeTag(String tagName, String tagNamespace, Map attrs, Closure body) {
        invokeTag(tagName, tagNamespace,-1, attrs, body);
    }

    public void invokeTag(String tagName, String tagNamespace, int lineNumber, Map attrs, Closure body) {
        // TODO custom namespace stuff needs to be generalized and pluggable
        if(tagNamespace.equals(TEMPLATE_NAMESPACE)) {
            final String tmpTagName = tagName;
            final Map tmpAttrs = attrs;
            tagName = "render";
            tagNamespace = DEFAULT_NAMESPACE;
            attrs = new HashMap() {{
                put("model", tmpAttrs);
                put("template", tmpTagName);
            }};
        } else if(tagNamespace.equals(LINK_NAMESPACE)) {
            final String tmpTagName = tagName;
            final Map tmpAttrs = attrs;
            tagName = "link";
            tagNamespace = DEFAULT_NAMESPACE;
            attrs = new HashMap() {{
                if(tmpAttrs.size() > 0) {
                    put("params", tmpAttrs);
                }
                put("mapping", tmpTagName);
            }};
        }

        final GrailsWebRequest webRequest = (GrailsWebRequest)getBinding().getVariable(WEB_REQUEST);
        final Writer out = webRequest.getOut();

        try {

            if( gspTagLibraryLookup.hasNamespace(tagNamespace) ) {
                GroovyObject tagLib = getTagLib(tagName,tagNamespace);
                if(tagLib != null) {
                    Object tagLibProp = tagLib.getProperty(tagName);
                    if(tagLibProp instanceof Closure) {
                        Closure tag = (Closure) ((Closure)tagLibProp).clone();

                        switch(tag.getParameterTypes().length) {
                            case 1:

                                tag.call( new Object[]{ attrs });
                                if(body != null) {
                                    body.call();
                                }

                            break;

                            case 2:
                                if(tag.getParameterTypes().length == 2) {
                                    tag.call( new Object[] { attrs, (body!=null)?body:EMPTY_BODY_CLOSURE });
                                }
                            break;
                        }

                    }else {
                       throw new GrailsTagException("Tag ["+tagName+"] does not exist in tag library ["+tagLib.getClass().getName()+"]", getGroovyPageFileName(),lineNumber);
                    }
                }
                else {
                    throw new GrailsTagException("Tag ["+tagName+"] does not exist. No tag library found for namespace: " + tagNamespace, getGroovyPageFileName(),lineNumber);
                }
            } else {
                StringBuilder plainTag = new StringBuilder();
                String fullTagName = tagNamespace + ":" + tagName;
                plainTag.append("<").append(fullTagName);
                for (Object o : attrs.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    plainTag.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
                }
                plainTag.append(">");
                try {
                    out.write(plainTag.toString());
                    if(body != null) {
                        Object bodyOutput = body.call();
                        if(bodyOutput != null) out.write(bodyOutput.toString());
                    }
                    out.write("</" + fullTagName + ">");
                } catch(IOException e) {
                    throw new GrailsTagException("I/O error invoking tag library closure as method: " + e.getMessage(), getGroovyPageFileName(),0);
                }
            }
		}
        catch(Exception e) {
           Throwable cause = GrailsExceptionResolver.getRootCause(e);
           if(cause instanceof GrailsTagException) {
               // catch and rethrow with context
               throw new GrailsTagException(cause.getMessage(),getGroovyPageFileName(), lineNumber);
           }
           else {
               throw new GrailsTagException("Error executing tag <"+tagNamespace+":"+tagName+">: " + e.getMessage(),e, getGroovyPageFileName(),lineNumber);
           }
        }
		finally {
			getBinding().setVariable(OUT,out);
			webRequest.setOut(out);
		}
    }

    private GroovyObject getTagLib(String tagName) {
    	return getTagLib(tagName,DEFAULT_NAMESPACE);
    }

    private GroovyObject getTagLib(String tagName, String namespace) {
        return gspTagLibraryLookup != null ? gspTagLibraryLookup.lookupTagLibrary(namespace, tagName) : null;
    }

    /**
     * Allows invoking of taglibs as method calls with simple bodies. The bodies should only contain text
     *
     * @param methodName The methodName of the tag to call or the methodName of a method on GroovPage
     * @param args The Arguments
     *
     * @return The result of the invocation
     */
    public Object invokeMethod(final String methodName, Object args) {
        if(methodName.equals("invokeTag"))  return super.invokeMethod(methodName, args);

        Map attrs = null;
        Object body = null;
        GroovyObject tagLib = getTagLib(methodName);
        if(tagLib != null) {
            final GrailsWebRequest webRequest = (GrailsWebRequest)getBinding().getVariable(WEB_REQUEST);
            Writer originalOut = webRequest.getOut();
            try {
                // get attributes and body closure
                if (args instanceof Object[]) {
                    Object[] argArray = (Object[])args;
                    if(argArray.length > 0 && argArray[0] instanceof Map)
                        attrs = (Map)argArray[0];
                    if(argArray.length > 1) {
                        body = argArray[1];
                    }
                }
                else if(args instanceof Map) {
                    attrs = (Map)args;
                }

                if(attrs == null) {
                    attrs = new HashMap();
                }
                return captureTagOutput(tagLib,methodName, attrs, body, webRequest);

            }
            finally {
                getBinding().setVariable(OUT,originalOut);
                webRequest.setOut(originalOut);
            }

        }
        else {
            return super.invokeMethod(methodName, args);
        }

    }

    public static String captureTagOutput(GroovyObject tagLib, String methodName, Map attrs, Object body, GrailsWebRequest webRequest) {
        Object tagLibProp;// retrieve tag lib and create wrapper writer
		Writer originalOut = webRequest.getOut();
		try {
	        final GroovyPageTagWriter out = new GroovyPageTagWriter();
	        webRequest.setOut(out);

	        // in a direct invocation the body is expected to return a string
	        // invoke the body closure and create a new closure that outputs
	        // to the response writer on each body invokation
	        Closure actualBody = createTagOutputCapturingClosure(tagLib,methodName, out, body);

	        tagLibProp = tagLib.getProperty(methodName);
	        if(tagLibProp instanceof Closure) {
	            Closure tag = (Closure) ((Closure)tagLibProp).clone();

	            if(tag.getParameterTypes().length == 1) {
	                tag.call( new Object[]{ attrs });
	                if(actualBody != null) {
	                    actualBody.call();
	                }
	            }else if(tag.getParameterTypes().length == 2) {
	                tag.call( new Object[] { attrs, actualBody });
	            }else {
	                throw new GrailsTagException("Tag ["+methodName+"] does not specify expected number of params in tag library ["+tagLib.getClass().getName()+"]");
	            }
	            return out.getValue();
	        }else {
	            throw new GrailsTagException("Tag ["+methodName+"] does not exist in tag library ["+tagLib.getClass().getName()+"]");
	        }

		}
		finally {
			webRequest.setOut(originalOut);
		}
    }

	private static Closure createTagOutputCapturingClosure(Object wrappedInstance, final String methodName, final Writer out, final Object body1) {
		if(body1==null) {
			return EMPTY_BODY_CLOSURE;
		}
        else if(body1 instanceof GroovyPageTagBody) {
            return (Closure) body1;
        }
        else {
			return new Closure(wrappedInstance) {
				public Object doCall(Object obj) {
					return call(new Object[] {obj} );
				}
				public Object doCall() {
					return call(new Object[0]);
				}
				public Object doCall(Object[] args) {
					return call(args);
				}
				public Object call(Object[] args) {
					if(body1 != null) {
						Object bodyResponse;
						if(body1 instanceof Closure) {
	                           if(args!=null && args.length>0){
	                                   bodyResponse = ((Closure)body1).call(args);
	                           }
	                           else {
	                                   bodyResponse = ((Closure)body1).call();
	                           }
						}
						else {
							bodyResponse = body1;
						}
	
						if(bodyResponse != null && !(bodyResponse instanceof Writer) ){
							try {
	                            out.write(bodyResponse.toString());
							} catch (IOException e) {
								throw new GrailsTagException("I/O error invoking tag library closure ["+methodName+"] as method: " + e.getMessage(),e);
							}
						}
					}
					return BLANK_STRING;
				}
			};
		}
	}

    /**
     * Return whether the given name cannot be used within the binding of a GSP
     *
     * @param name True if it can't
     * @return A boolean true or false
     */
    public static boolean isReservedName(String name) {
        return RESERVED_NAMES.contains(name);
    }

    /**
     * Sets the JSP tags used by this GroovyPage instance
     *
     * @param jspTags The JSP tags used
     */
    public void setJspTags(Map jspTags) {
        this.jspTags = jspTags;
    }

	public String[] getHtmlParts() {
		return htmlParts;
	}

	public void setHtmlParts(String[] htmlParts) {
		this.htmlParts = htmlParts;
	}
} // GroovyPage

