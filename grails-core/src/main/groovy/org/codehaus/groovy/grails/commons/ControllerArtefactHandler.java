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
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class ControllerArtefactHandler extends ArtefactHandlerAdapter {

	private static final GrailsClass NO_CONTROLLER = new AbstractGrailsClass(Object.class, "Controller") {};
	
    public static final String TYPE = "Controller";
    public static final String PLUGIN_NAME = "controllers";
    private ConcurrentHashMap<String, GrailsClass> uriToControllerClassCache;
    private LinkedBlockingQueue<String> uriToControllerClassKeys;
    private ArtefactInfo artefactInfo;

    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
                DefaultGrailsControllerClass.CONTROLLER, false);
    }

    @Override
    public void initialize(ArtefactInfo artefacts) {
        uriToControllerClassCache = new ConcurrentHashMap<String, GrailsClass>();
        uriToControllerClassKeys = new LinkedBlockingQueue<String>(10000);
        artefactInfo = artefacts;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public GrailsClass getArtefactForFeature(Object feature) {
        if (artefactInfo == null) {
            return null;
        }

        String uri = feature.toString();
        GrailsClass controllerClass = uriToControllerClassCache.get(uri);
        if (controllerClass == null) {
            final GrailsClass[] controllerClasses = artefactInfo.getGrailsClasses();
            // iterate in reverse in order to pick up application classes first
            for (int i = (controllerClasses.length-1); i >= 0; i--) {
                GrailsClass c = controllerClasses[i];
                if (((GrailsControllerClass) c).mapsToURI(uri)) {
                    controllerClass = c;
                    break;
                }
            }
            if (controllerClass == null) {
            	controllerClass = NO_CONTROLLER;
            }
            
            // don't cache for dev environment
            if (Environment.getCurrent() != Environment.DEVELOPMENT) {
            	
            	// implement a soft limit on the max number of elements in cache
            	// the actual count can be uriToControllerClassKeys' capacity + concurrent threads (which should be limited by the container)
                if (uriToControllerClassCache.putIfAbsent(uri, controllerClass) == null) {
                	while (!uriToControllerClassKeys.offer(uri)) {
                		String oldest = uriToControllerClassKeys.poll();
                		uriToControllerClassCache.remove(oldest);
                	}
                }
            }
        }
        
        if (controllerClass == NO_CONTROLLER) {
        	controllerClass = null;
        }
        return controllerClass;
    }
}
