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

import org.grails.core.artefact.TagLibArtefactHandler;
import org.grails.web.api.CommonWebApi;
import org.grails.web.taglib.TagLibraryLookup;
import org.grails.web.taglib.TagOutput;
import org.springframework.context.ApplicationContext;

/**
 * API for Tag libraries in a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class TagLibraryApi extends CommonWebApi {

    private static final long serialVersionUID = 1;

    private transient TagLibraryLookup tagLibraryLookup;

    public TagLibraryApi() {
        super(null);
    }

    public TagLibraryApi(GrailsPluginManager pluginManager) {
        super(pluginManager);
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
