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
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.taglib.GroovyPageTagWriter;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.springframework.beans.BeanWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.StringWriter;
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
    public static final String PAGE_SCOPE = "pageScope";
    public static final Collection RESERVED_NAMES = new ArrayList() {{
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
    private GrailsApplication application;
    private static final String BLANK_STRING = "";
    private ApplicationContext applicationContext;


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
        if(BINDING.equals(property)) return getBinding();

        MetaProperty mp = getMetaClass().getMetaProperty(property);
        if(mp!= null)return mp.getProperty(this);
        
        Object value = getBinding().getVariables().get(property);
        if(value == null) {
            value = getTagLibForNamespace(property);
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

        final GrailsWebRequest webRequest = (GrailsWebRequest)getBinding().getVariable(WEB_REQUEST);
        final Writer out = webRequest.getOut();

        try {
	        if(this.application == null)
	            initPageState();


            if( getTagLibForNamespace(tagNamespace) != null ) {
                GroovyObject tagLib = getTagLib(tagName,tagNamespace);
                if(tagLib != null) {
                    Object tagLibProp;
                    BeanWrapper bean = getTagLibraryBean(tagLib, webRequest);
                    if(bean.isReadableProperty(tagName)) {
                        tagLibProp = tagLib.getProperty(tagName);
                    } else {
                        throw new GrailsTagException("Tag ["+tagName+"] does not exist in tag library ["+tagLib.getClass().getName()+"]");
                    }
                    if(tagLibProp instanceof Closure) {
                        Closure tag = setupTagClosure(tagLibProp);

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
                    throw new GrailsTagException("Tag ["+tagName+"] does not exist. No tag library found for namespace: " + tagNamespace);
                }
            } else {
                StringBuffer plainTag = new StringBuffer();
                String fullTagName = tagNamespace + ":" + tagName;
                plainTag.append("<").append(fullTagName);
                for(Iterator iterator = attrs.entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry entry = (Map.Entry) iterator.next();
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
                    throw new GrailsTagException("I/O error invoking tag library closure as method");
                }
            }
		}
		finally {       
			getBinding().setVariable(OUT,out);
			webRequest.setOut(out);
		}		
    }

    private void initPageState() {
        if(this.application == null) {
            ServletContext context = (ServletContext)getBinding().getVariable(SERVLET_CONTEXT);
            this.applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
            this.application = (GrailsApplication)applicationContext.getBean(GrailsApplication.APPLICATION_ID);

        }
    }

    private GroovyObject getTagLibForNamespace(String namespace) {
        if(this.application == null)
            initPageState();
        

        GrailsClass tagLibClass = application.getArtefactForFeature(TagLibArtefactHandler.TYPE, namespace);
        if(tagLibClass != null) {
            return (GroovyObject) applicationContext.getBean(tagLibClass.getFullName());
        }
        return null;
    }

    private GroovyObject getTagLib(String tagName) {
    	return getTagLib(tagName,DEFAULT_NAMESPACE);
    }
    
    private GroovyObject getTagLib(String tagName, String namespace) {
        if(this.application == null)
            initPageState();

        GrailsClass tagLibClass = application.getArtefactForFeature(TagLibArtefactHandler.TYPE, namespace+':'+tagName);
        if(tagLibClass != null) {
            return (GroovyObject) applicationContext.getBean(tagLibClass.getFullName());
        }
        return null;
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
        if(methodName.equals("fromHtml"))  return super.invokeMethod(methodName, args);

        Map attrs = null;
    		Object body = null;
    		GroovyObject tagLib = getTagLib(methodName);

            final GrailsWebRequest webRequest = (GrailsWebRequest)getBinding().getVariable(WEB_REQUEST);
            Writer originalOut = webRequest.getOut();
    		try {
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


                    BeanWrapper bean = getTagLibraryBean(tagLib, webRequest);

                    if(LOG.isDebugEnabled()) {
        				LOG.debug("Attempting to invoke ["+methodName+"] on tag library ["+tagLib.getClass()+"] with MetaClass ["+tagLib.getMetaClass()+"]");
        			}
        			if(bean.isReadableProperty(methodName)) {
                        return captureTagOutput(tagLib,methodName, attrs, body, webRequest, bean);
                    } else {
        				if(args instanceof Object[])
                            return super.invokeMethod(methodName, args);
        				else
        					return super.invokeMethod(methodName, args);
        			}

        		}
    		}
    		finally { 
				getBinding().setVariable(OUT,originalOut);	
    			webRequest.setOut(originalOut);
    		}
            return super.invokeMethod(methodName, args);
    }

    private BeanWrapper getTagLibraryBean(GroovyObject tagLib, GrailsWebRequest webRequest) {
        GrailsApplication application = webRequest.getAttributes().getGrailsApplication();
        GrailsClass grailsClass = application.getArtefact(TagLibArtefactHandler.TYPE, tagLib.getClass().getName());
        return grailsClass.getReference();
    }

    public static String captureTagOutput(GroovyObject tagLib, String methodName, Map attrs, Object body, GrailsWebRequest webRequest, BeanWrapper bean) {
        Object tagLibProp;// retrieve tag lib and create wrapper writer
		Writer originalOut = webRequest.getOut();
		try {
	        final GroovyPageTagWriter out = new GroovyPageTagWriter(new StringWriter());
	        webRequest.setOut(out);

	        // in a direct invocation the body is expected to return a string
	        // invoke the body closure and create a new closure that outputs
	        // to the response writer on each body invokation
	        Closure actualBody = createTagOutputCapturingClosure(tagLib,methodName, out, body);

	        tagLibProp = tagLib.getProperty(methodName);
	        if(tagLibProp instanceof Closure) {
	            Closure tag = setupTagClosure(tagLibProp);

	            if(tag.getParameterTypes().length == 1) {
	                tag.call( new Object[]{ attrs });
	                if(actualBody != null) {
	                    actualBody.call();
	                }
	            }else if(tag.getParameterTypes().length == 2) {
	                tag.call( new Object[] { attrs, actualBody });
	            }else {
	                throw new GrailsTagException("Tag ["+methodName+"] does not specify expected number of params in tag library ["+bean.getWrappedClass().getName()+"]");
	            }
	            return out.getValue();
	        }else {
	            throw new GrailsTagException("Tag ["+methodName+"] does not exist in tag library ["+bean.getWrappedClass().getName()+"]");
	        }
			
		}   
		finally {
			webRequest.setOut(originalOut);
		}
    }

    private static Closure setupTagClosure(final Object tagLibProp) {
		Closure original = (Closure)tagLibProp;
		return (Closure)original.clone();
	}

	private static Closure createTagOutputCapturingClosure(Object wrappedInstance, final String methodName, final Writer out, final Object body1) {
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
							throw new GrailsTagException("I/O error invoking tag library closure ["+methodName+"] as method");
						}
					}
				}
				return BLANK_STRING;
			}
		};
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
} // GroovyPage

