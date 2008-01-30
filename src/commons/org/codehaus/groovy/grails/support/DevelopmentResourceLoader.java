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
package org.codehaus.groovy.grails.support;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 *
 * A ResourceLoader that allows files references like /WEB-INF/grails-app to be loaded from ./grails-app to support
 * the difference between wAR deployment and run-app
 *
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Jan 30, 2008
 */
public class DevelopmentResourceLoader extends DefaultResourceLoader{
    private GrailsApplication application;
    private String baseLocation = ".";


    public DevelopmentResourceLoader(GrailsApplication application) {
        super();
        Assert.notNull(application, "Argument [application] is required");
        this.application = application;
    }

    public DevelopmentResourceLoader(GrailsApplication application, String baseLocation) {
        this(application);
        this.baseLocation = baseLocation;
    }

    public Resource getResource(String location) {
        if(application.isWarDeployed()) {
            return super.getResource(location);
        }
        else {
            location = getRealLocationInProject(location);
            return super.getResource(location);
        }
    }

    /**
     * Retrieves the real location of a GSP within a Grails project
     * @param location The location of the GSP at deployment time
     * @return The location of the GSP at development time
     */
    protected String getRealLocationInProject(String location) {

        if(!location.startsWith("/")) location = "/"+location;
        
        if(location.startsWith(GrailsResourceUtils.WEB_INF)) {
            return "file:"+baseLocation+"/"+location.substring(GrailsResourceUtils.WEB_INF.length()+1);
        }
        else {
            return GrailsResourceUtils.WEB_APP_DIR+location;
        }
    }
}

