/* Copyright 2014 the original author or authors.
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
package grails.core;

import org.grails.config.PropertySourcesConfig;
import org.grails.core.AbstractGrailsApplication;
import groovy.util.ConfigObject;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * GrailsApplication implementation to be used in standalone applications 
 * 
 * @since 2.4.0
 */
public class StandaloneGrailsApplication extends AbstractGrailsApplication {
    public StandaloneGrailsApplication() {
        super();
        setConfig(new PropertySourcesConfig());
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        super.setMainContext(applicationContext);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getAllClasses() {
        return new Class[0];
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getAllArtefacts() {
        return new Class[0];
    }

    @Override
    public void refreshConstraints() {
        
    }

    @Override
    public void refresh() {
        
    }

    @Override
    public void rebuild() {
        
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Resource getResourceForClass(Class theClazz) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isArtefact(Class theClazz) {
        return false;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isArtefactOfType(String artefactType, Class theClazz) {
        return false;
    }

    @Override
    public boolean isArtefactOfType(String artefactType, String className) {
        return false;
    }

    @Override
    public GrailsClass getArtefact(String artefactType, String name) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ArtefactHandler getArtefactType(Class theClass) {
        return null;
    }

    @Override
    public ArtefactInfo getArtefactInfo(String artefactType) {
        return null;
    }

    @Override
    public GrailsClass[] getArtefacts(String artefactType) {
        return new GrailsClass[0];
    }

    @Override
    public GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public GrailsClass addArtefact(String artefactType, Class artefactClass) {
        return null;
    }

    @Override
    public GrailsClass addArtefact(String artefactType, GrailsClass artefactGrailsClass) {
        return null;
    }

    @Override
    public void registerArtefactHandler(ArtefactHandler handler) {
        
    }

    @Override
    public boolean hasArtefactHandler(String type) {
        return false;
    }

    @Override
    public ArtefactHandler[] getArtefactHandlers() {
        return new ArtefactHandler[0];
    }

    @Override
    public void initialise() {
        
    }

    @Override
    public boolean isInitialised() {
        return true;
    }

    @Override
    public GrailsClass getArtefactByLogicalPropertyName(String type, String logicalName) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void addArtefact(Class artefact) {
        
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void addOverridableArtefact(Class artefact) {
        
    }

    @Override
    public ArtefactHandler getArtefactHandler(String type) {
        return null;
    }
}
