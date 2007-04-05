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

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class ControllerArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Controller";
    private GrailsClass[] controllerClasses;


    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
            DefaultGrailsControllerClass.CONTROLLER,
            false);
    }

    public void initialize(ArtefactInfo artefacts) {
        controllerClasses = artefacts.getGrailsClasses();
    }

    public GrailsClass getArtefactForFeature(Object feature) {
        String uri = feature.toString();
        for (int i = 0; i < controllerClasses.length; i++) {
            if (((GrailsControllerClass)controllerClasses[i]).mapsToURI(uri)) {
                return controllerClasses[i];
            }
        }
        return null;
    }


}
