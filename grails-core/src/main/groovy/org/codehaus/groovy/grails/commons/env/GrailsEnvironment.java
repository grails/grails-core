/*
 * Copyright 2011 SpringSource
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

package org.codehaus.groovy.grails.commons.env;

import grails.util.Environment;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.support.DefaultWebEnvironment;

import java.util.Set;

/**
 * Bridges Grails' existing environment API with the new Spring 3.1 environment profiles API
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsEnvironment extends DefaultWebEnvironment {

    GrailsApplication grailsApplication;

    public GrailsEnvironment(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
        getPropertySources().addFirst(new GrailsConfigPropertySource(grailsApplication));
    }

    @Override
    protected Set<String> doGetActiveProfiles() {
        Set<String> activeProfiles = super.doGetActiveProfiles();
        activeProfiles.add(Environment.getCurrent().getName());
        return activeProfiles;
    }


    private class GrailsConfigPropertySource extends PropertySource<GrailsApplication> {

        public GrailsConfigPropertySource(GrailsApplication grailsApplication) {
            super(grailsApplication.getMetadata().getApplicationName(), grailsApplication);
        }

        @Override
        public Object getProperty(String key) {
            return grailsApplication.getFlatConfig().get(key);
        }
    }
}
