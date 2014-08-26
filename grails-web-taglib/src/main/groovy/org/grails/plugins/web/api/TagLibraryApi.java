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
package org.grails.plugins.web.api;

import grails.core.GrailsApplication;
import grails.core.GrailsTagLibClass;
import grails.plugins.GrailsPluginManager;
import grails.util.Environment;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.grails.core.artefact.TagLibArtefactHandler;
import org.grails.web.api.CommonWebApi;
import org.grails.web.taglib.TagLibraryLookup;
import org.grails.web.taglib.TagOutput;
import org.grails.web.taglib.util.TagLibraryMetaUtils;
import org.springframework.context.ApplicationContext;

import java.util.List;

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
                tagLibrary = lookup.lookupTagLibrary(TagOutput.DEFAULT_NAMESPACE, methodName);
                usednamespace = TagOutput.DEFAULT_NAMESPACE;
            }

            if (tagLibrary != null && !developmentMode) {
                TagLibraryMetaUtils.registerMethodMissingForTags(mc, lookup, usednamespace, methodName);
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
    
    private String getNamespace(Object instance) {
        GrailsApplication grailsApplication = getGrailsApplication(null);
        if (grailsApplication != null) {
            GrailsTagLibClass taglibrary = (GrailsTagLibClass) grailsApplication.getArtefact(TagLibArtefactHandler.TYPE, instance.getClass().getName());
            if (taglibrary != null) {
                return taglibrary.getNamespace();
            }
        }
        return TagOutput.DEFAULT_NAMESPACE;
    }
    public TagLibraryLookup getTagLibraryLookup() {
        if (tagLibraryLookup == null) {
            ApplicationContext applicationContext = getApplicationContext(null);
            if (applicationContext != null && applicationContext.containsBean("gspTagLibraryLookup")) {
                tagLibraryLookup = applicationContext.getBean("gspTagLibraryLookup", TagLibraryLookup.class);
            }
        }
        return tagLibraryLookup;
    }
}
