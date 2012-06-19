/*
 * Copyright 2010 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.api;

import grails.util.Environment;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.io.Writer;
import java.util.List;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack;
import org.codehaus.groovy.grails.web.pages.GroovyPageRequestBinding;
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup;
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestAttributes;

/**
 * API for Tag libraries in a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class TagLibraryApi extends CommonWebApi {

    private static final long serialVersionUID = 1;

    private transient TagLibraryLookup tagLibraryLookup;
    private boolean developmentMode = Environment.isDevelopmentMode();

    public TagLibraryApi() {
        super(null);
    }

    public TagLibraryApi(GrailsPluginManager pluginManager) {
        super(pluginManager);
    }

    /**
     * Throws a GrailsTagException
     *
     * @param instance The tag library instance
     * @param message The error message
     */
    public void throwTagError(@SuppressWarnings("unused") Object instance, String message) {
        throw new GrailsTagException(message);
    }

    /**
     * Obtains the page scope instance
     *
     * @param instance The tag library
     * @return  The page scope instance
     */
    public GroovyPageBinding getPageScope(Object instance) {
        GrailsWebRequest webRequest = getWebRequest(instance);
        GroovyPageBinding binding = (GroovyPageBinding) webRequest.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE, RequestAttributes.SCOPE_REQUEST);
        if (binding == null) {
            binding = new GroovyPageBinding(new GroovyPageRequestBinding(webRequest));
            binding.setRoot(true);
            webRequest.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, binding, RequestAttributes.SCOPE_REQUEST);
        }
        return binding;
    }

    /**
     * Obtains the currently output writer

     * @param instance The tag library instance
     * @return The writer to use
     */
    public Writer getOut(@SuppressWarnings("unused") Object instance) {
        return GroovyPageOutputStack.currentWriter();
    }

    /**
     * Sets the current output writer
     * @param instance The tag library instance
     * @param newOut The new output writer
     */
    public void setOut(@SuppressWarnings("unused") Object instance, Writer newOut) {
        GroovyPageOutputStack.currentStack().push(newOut,true);
    }

    /**
     * Method missing implementation that handles tag invocation by method name
     *
     * @param instance The instance
     * @param methodName The method name
     * @param argsObject The arguments
     * @return The result
     */
    public Object methodMissing(Object instance, String methodName, Object argsObject) {
        Object[] args = argsObject instanceof Object[] ? (Object[])argsObject : new Object[]{argsObject};
        MetaClass mc = InvokerHelper.getMetaClass(instance);
        String usednamespace = getNamespace(instance);
        TagLibraryLookup lookup = getTagLibraryLookup();
        if (lookup != null) {

            GroovyObject tagLibrary = lookup.lookupTagLibrary(usednamespace, methodName);
            if (tagLibrary == null) {
                tagLibrary = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, methodName);
                usednamespace = GroovyPage.DEFAULT_NAMESPACE;
            }

            if (tagLibrary != null && !developmentMode) {
                WebMetaUtils.registerMethodMissingForTags(mc, lookup, usednamespace, methodName);
            }

            if (tagLibrary != null) {
                List<MetaMethod> respondsTo = tagLibrary.getMetaClass().respondsTo(tagLibrary, methodName, args);
                if (respondsTo.size()>0) {
                    return respondsTo.get(0).invoke(tagLibrary, args);
                }
            }
        }

        throw new MissingMethodException(methodName, instance.getClass(), args);
    }


    /**
     * Prpoerty missing implementation that looks up tag library namespaces or tags in the default namespace
     *
     * @param instance The tag library instance
     * @param name The property name
     * @return A tag namespace or a tag in the default namespace
     *
     * @throws MissingPropertyException When no tag namespace or tag is found
     */
    public Object propertyMissing(Object instance, String name) {
        TagLibraryLookup gspTagLibraryLookup = getTagLibraryLookup();
        if (gspTagLibraryLookup != null) {

            Object result = gspTagLibraryLookup.lookupNamespaceDispatcher(name);
            if (result == null) {
                String namespace = getNamespace(instance);
                GroovyObject tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name);
                if (tagLibrary == null) {
                    tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name);
                }

                if (tagLibrary != null) {
                    Object tagProperty = tagLibrary.getProperty(name);
                    if (tagProperty instanceof Closure) {
                        result = ((Closure<?>)tagProperty).clone();
                    }
                }
            }

            if (result != null && !developmentMode) {
                MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(instance.getClass());
                WebMetaUtils.registerPropertyMissingForTag(mc, name, result);
            }

            if (result != null) {
                return result;
            }
        }

        throw new MissingPropertyException(name, instance.getClass());
    }

    private String getNamespace(Object instance) {
        GrailsApplication grailsApplication = getGrailsApplication(null);
        if (grailsApplication != null) {
            GrailsTagLibClass taglibrary = (GrailsTagLibClass) grailsApplication.getArtefact(TagLibArtefactHandler.TYPE, instance.getClass().getName());
            if (taglibrary != null) {
                return taglibrary.getNamespace();
            }
        }
        return GroovyPage.DEFAULT_NAMESPACE;
    }

    @Autowired
    public void setGspTagLibraryLookup(TagLibraryLookup gspTagLibraryLookup) {
        this.tagLibraryLookup = gspTagLibraryLookup;
    }

    public void setTagLibraryLookup(TagLibraryLookup tagLibraryLookup) {
        this.tagLibraryLookup = tagLibraryLookup;
    }

    public TagLibraryLookup getTagLibraryLookup() {
        if (this.tagLibraryLookup == null) {
            ApplicationContext applicationContext = getApplicationContext(null);
            if (applicationContext != null && applicationContext.containsBean("gspTagLibraryLookup")) {
                tagLibraryLookup = applicationContext.getBean("gspTagLibraryLookup", TagLibraryLookup.class);
            }
        }
        return tagLibraryLookup;
    }
}
