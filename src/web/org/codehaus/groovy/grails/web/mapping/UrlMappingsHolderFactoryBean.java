/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.mapping;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.commons.*;

import java.util.List;
import java.util.ArrayList;

/**
 * A FactoryBean for constructing the UrlMappingsHolder from the registered UrlMappings class within a
 * GrailsApplication
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 * 
 *        <p/>
 *        Created: Mar 6, 2007
 *        Time: 6:48:57 PM
 */
public class UrlMappingsHolderFactoryBean implements FactoryBean, InitializingBean, GrailsApplicationAware {
    private GrailsApplication grailsApplication;
    private UrlMappingsHolder urlMappingsHolder;
    private UrlMappingEvaluator mappingEvaluator = new DefaultUrlMappingEvaluator();
    private UrlMappingParser urlParser = new DefaultUrlMappingParser();

    public Object getObject() throws Exception {
        return this.urlMappingsHolder;
    }

    public Class getObjectType() {
        return UrlMappingsHolder.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        if(grailsApplication == null) throw new IllegalStateException("Property [grailsApplication] must be set!");

        List urlMappings = new ArrayList();

        GrailsClass[] mappings = grailsApplication.getArtefacts(UrlMappingsArtefactHandler.TYPE);

        for (int i = 0; i < mappings.length; i++) {
            GrailsClass mappingClass = mappings[i];
            List grailsClassMappings = mappingEvaluator.evaluateMappings(mappingClass.getClazz());
            urlMappings.addAll(grailsClassMappings);
        }

        //mapDefaultControllerUris(urlMappings);

        this.urlMappingsHolder = new DefaultUrlMappingsHolder(urlMappings);

    }

    private void mapDefaultControllerUris(List urlMappings) {
        GrailsControllerClass[] controllers = (GrailsControllerClass[])grailsApplication.getArtefacts(ControllerArtefactHandler.TYPE);
        for (int i = 0; i < controllers.length; i++) {
            GrailsControllerClass controller = controllers[i];
            String[] uris = controller.getURIs();
            for (int j = 0; j < uris.length; j++) {
                String uri = uris[j];
                UrlMappingData data = urlParser.parse(uri);

                urlMappings.add(new RegexUrlMapping(    data,
                                                        controller.getPropertyName(),
                                                        controller.getClosurePropertyName(uri), null));
            }
        }
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
