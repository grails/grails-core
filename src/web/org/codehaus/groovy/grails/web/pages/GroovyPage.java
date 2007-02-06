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

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.metaclass.TagLibDynamicMethods;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.HtmlUtils;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Base class for a GroovyPage (at the moment there is nothing in here but could be useful for providing utility methods
 * etc.
 *
 * @author Troy Heninger
 * @author Graeme Rocher
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

    private GrailsApplication application;
    private GrailsApplicationAttributes grailsAttributes;


    /**
     * Convert from HTML to Unicode text.  This function converts many of the encoded HTML
     * characters to normal Unicode text.  Example: &amp;lt&semi; to &lt;.
     */
    public static String fromHtml(String text)
    {
        int ixz;
        if (text == null || (ixz = text.length()) == 0) return text;
        StringBuffer buf = new StringBuffer(ixz);
        String rep = null;
        for (int ix = 0; ix < ixz; ix++)
        {
            char c = text.charAt(ix);
            if (c == '&')
            {
                String sub = text.substring(ix + 1).toLowerCase();
                if (sub.startsWith("lt;"))
                {
                    c = '<';
                    ix += 3;
                }
                else
                if (sub.startsWith("gt;"))
                {
                    c = '>';
                    ix += 3;
                }
                else
                if (sub.startsWith("amp;"))
                {
                    c = '&';
                    ix += 4;
                }
                else
                if (sub.startsWith("nbsp;"))
                {
                    c = ' ';
                    ix += 5;
                }
                else
                if (sub.startsWith("semi;"))
                {
                    c = ';';
                    ix += 5;
                }
                else
                if (sub.startsWith("#"))
                {
                    char c2 = 0;
                    for (int iy = ix + 1; iy < ixz; iy++)
                    {
                        char c1 = text.charAt(iy);
                        if (c1 >= '0' && c1 <= '9')
                        {
                            c2 = (char)(c2 * 10 + c1);
                            continue;
                        }
                        if (c1 == ';')
                        {
                            c = c2;
                            ix = iy;
                        }
                        break;
                    }
                }
            }
            if (rep != null)
            {
                buf.append(rep);
                rep = null;
            }
            else buf.append(c);
        }
        return buf.toString();
    } // fromHtml()

    public Object getProperty(String property) {
        // in GSP we assume if a property doesn't exist that
        // it is null rather than throw an error this works nicely
        // with the Groovy Truth
        try {
            return super.getProperty(property);
        } catch (MissingPropertyException mpe) {
              if(LOG.isDebugEnabled()) {
                  LOG.debug("No property ["+property+"] found in GSP returning null");
              }
              return null;
        }
    }

    /**
     * Attempts to invokes a dynamic tag
     *
     * @param tagName The name of the tag
     * @param attrs The tags attributes
     * @param body  The body of the tag as a closure
     */
    public void invokeTag(String tagName, Map attrs, Closure body) {
        Binding binding = getBinding();

        final Writer out = (Writer)binding.getVariable(GroovyPage.OUT);

        if(this.application == null)
            initPageState();

        GroovyObject tagLib = getTagLib(tagName);

        if(tagLib != null) {
            Object tagLibProp;
            Map properties = DefaultGroovyMethods.getProperties(tagLib);
			if(properties.containsKey(tagName)) {
                tagLibProp = properties.get(tagName);
            } else {
                throw new GrailsTagException("Tag ["+tagName+"] does not exist in tag library ["+tagLib.getClass().getName()+"]");
            }
            if(tagLibProp instanceof Closure) {               
                Closure tag = setupTagClosure(out, tagLibProp);
                
                if(tag.getParameterTypes().length == 1) {
                    tag.call( new Object[]{ attrs });
                    if(body != null) {
                        body.call();
                    }
                }
                if(tag.getParameterTypes().length == 2) {
                    tag.call( new Object[] { attrs, body });
                }
            }else {
               throw new GrailsTagException("Tag ["+tagName+"] does not exist in tag library ["+tagLib.getClass().getName()+"]");
            }
        }
        else {
            throw new GrailsTagException("Tag ["+tagName+"] does not exist. No tag library found.");
        }
    }

    private void initPageState() {
        if(this.application == null) {
            ServletContext context = (ServletContext)getBinding().getVariable(SERVLET_CONTEXT);
            this.grailsAttributes = new DefaultGrailsApplicationAttributes(context);
            this.application = grailsAttributes.getGrailsApplication();
        }
    }

    private GroovyObject getTagLib(String tagName) {
        if(this.application == null)
            initPageState();
        Binding binding = getBinding();
        HttpServletRequest request = (HttpServletRequest)binding.getVariable(GroovyPage.REQUEST);
        HttpServletResponse response = (HttpServletResponse)binding.getVariable(GroovyPage.RESPONSE);

        return grailsAttributes.getTagLibraryForTag(request,response,tagName);
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

    	try {
    		return super.invokeMethod(methodName, args);
    	}
    	catch(MissingMethodException mme) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("No method ["+methodName+"] found");
			}
    		Map attrs = null;
    		Object body = null;
    		// retrieve tag lib and create wrapper writer
    		StringWriter capturedOut = new StringWriter();
    		final Writer out = new PrintWriter(capturedOut);
    		GroovyObject tagLib = getTagLib(methodName);

    		GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
    		Writer originalOut = webRequest.getOut();
    		try {
    			webRequest.setOut(out);
        		if(tagLib != null) {


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

        			// in a direct invocation the body is expected to return a string
        			// invoke the body closure and create a new closure that outputs
        			// to the response writer on each body invokation
        			final Object body1 = body;
        			Closure actualBody = createTagOutputCapturingClosure(methodName, out, body1);

        			
        			Object tagLibProp;
        			Map properties = DefaultGroovyMethods.getProperties(tagLib);
        			if(LOG.isDebugEnabled()) {
        				LOG.debug("Attempting to invoke ["+methodName+"] on tag library ["+tagLib.getClass()+"] with MetaClass ["+tagLib.getMetaClass()+"]");
        			}        			
        			if(properties.containsKey(methodName)) {
        				tagLibProp = properties.get(methodName);
        				if(tagLibProp instanceof Closure) {
        					Closure tag = setupTagClosure(out, tagLibProp);
        					
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
        					return capturedOut.toString();
        				}else {
        					throw new GrailsTagException("Tag ["+methodName+"] does not exist in tag library ["+tagLib.getClass().getName()+"]");
        				}
        			} else {
        				if(args instanceof Object[])
        					throw new MissingMethodException(methodName,tagLib.getClass(), (Object[])args);
        				else
        					throw new MissingMethodException(methodName,tagLib.getClass(), new Object[]{ args });
        			}

        		} else if(methodName.startsWith("encodeAs")) {
        			if(LOG.isDebugEnabled()) {
        				LOG.debug("Invoking as dynamic encoder");
        			}
        			final String codec = methodName.substring("encodeAs".length()).toLowerCase();
        			
        			// TODO temporary code here...
        			if("html".equals(codec)) {
        				return HtmlUtils.htmlEscape(((Object[])args)[0].toString());
        			} else {
            			if(LOG.isDebugEnabled()) {
            				LOG.debug("No encoder found for " + codec);
            			}
        			}
        		}
    			
    		}
    		finally {
    			webRequest.setOut(originalOut);
    		}
    		
       		throw new MissingMethodException(methodName,GroovyPage.class, new Object[]{ args });
    	}
    }

	private Closure setupTagClosure(final Writer out, Object tagLibProp) {
		Closure original = (Closure)tagLibProp;
		return (Closure)original.clone();
	}

	private Closure createTagOutputCapturingClosure(final String methodName, final Writer out, final Object body1) {
		return new Closure(this) {
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
						bodyResponse = ((Closure)body1).call();
					}
					else {
						bodyResponse = body1;
					}

					if(bodyResponse != null) {
						try {
							out.write(bodyResponse.toString());
						} catch (IOException e) {
							throw new GrailsTagException("I/O error invoking tag library closure ["+methodName+"] as method");
						}
					}
				}
				return null;
			}
		};
	}
} // GroovyPage

