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
package org.codehaus.groovy.grails.commons;

import grails.util.Environment;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class ControllerArtefactHandler extends ArtefactHandlerAdapter {
    public static final String TYPE = "Controller";
    public static final String PLUGIN_NAME = "controllers";
    private ConcurrentHashMap<String, GrailsClass> uriToControllerClassCache;
    private ArtefactInfo artefactInfo;


    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
            DefaultGrailsControllerClass.CONTROLLER,
            false);
    }

    public void initialize(ArtefactInfo artefacts) {
        uriToControllerClassCache = new ConcurrentHashMap<String, GrailsClass>();
        this.artefactInfo = artefacts;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    public GrailsClass getArtefactForFeature(Object feature) {
        String uri = feature.toString();
        if(artefactInfo!=null) {
        	GrailsClass controllerClass = uriToControllerClassCache.get(uri);
        	if(controllerClass==null) {
                final GrailsClass[] controllerClasses = artefactInfo.getGrailsClasses();
                // iterate in reverse in order to pick up application classes first
                for (int i = (controllerClasses.length-1); i >= 0; i--) {
                    GrailsClass c = controllerClasses[i];
                    if (((GrailsControllerClass) c).mapsToURI(uri)) {
                        controllerClass = c;
                        break;
                    }

                }
	            if(controllerClass != null) {
                    // don't cache for dev environment
                    if(Environment.getCurrent() != Environment.DEVELOPMENT)
	            	    uriToControllerClassCache.putIfAbsent(uri, controllerClass);
	            }
        	}
        	return controllerClass;
        }
        return null;
    }


}
