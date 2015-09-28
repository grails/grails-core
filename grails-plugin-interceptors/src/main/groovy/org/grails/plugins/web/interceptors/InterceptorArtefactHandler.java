/*
 * Copyright 2015 original authors
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
package org.grails.plugins.web.interceptors;

import grails.artefact.Interceptor;
import grails.core.ArtefactHandlerAdapter;
import grails.core.DefaultGrailsClass;
import grails.core.GrailsClass;

/**
 * {@link grails.core.ArtefactHandler} for {@link grails.artefact.Interceptor} instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class InterceptorArtefactHandler extends ArtefactHandlerAdapter {

    public static final String MATCH_SUFFIX = ".INTERCEPTOR_MATCHED";

    public static final String TYPE = Interceptor.class.getSimpleName();
    public static final String PLUGIN_NAME = "interceptors";

    public InterceptorArtefactHandler() {
        super(TYPE, GrailsClass.class, DefaultGrailsClass.class, TYPE);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }
}
