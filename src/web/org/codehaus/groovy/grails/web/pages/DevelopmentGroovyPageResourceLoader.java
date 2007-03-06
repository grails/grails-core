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
package org.codehaus.groovy.grails.web.pages;

import org.codehaus.groovy.grails.support.StaticResourceLoader;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.springframework.core.io.Resource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A StaticResourceLoader that loads GSPs from a local grails-app folder instead of from WEB-INF in
 * development mode
 *
 * @see org.codehaus.groovy.grails.support.StaticResourceLoader
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 26, 2007
 *        Time: 4:25:08 PM
 */
public class DevelopmentGroovyPageResourceLoader extends StaticResourceLoader {
    private static final Log LOG = LogFactory.getLog(DevelopmentGroovyPageResourceLoader.class);
    /**
     * The id of the instance of this bean to be used in the Spring context
     */
    public static final String BEAN_ID = "groovyPageResourceLoader";

    public Resource getResource(String location) {
        if(StringUtils.isBlank(location)) throw new IllegalArgumentException("Argument [location] cannot be null or blank");
        if(!location.endsWith(GroovyPage.EXTENSION)) throw new IllegalArgumentException("Argument [location] is not a reference to a GSP. It does not end with .gsp");

        location = getRealLocationInProject(location);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Loading GSP from location ["+location+"]");
        }

        return super.getResource(location);
    }

    /**
     * Retrieves the real location of a GSP within a Grails project
     * @param location The location of the GSP at deployment time
     * @return The location of the GSP at development time
     */
    protected String getRealLocationInProject(String location) {

        if(location.startsWith(GrailsResourceUtils.WEB_INF)) {
            return location.substring(GrailsResourceUtils.WEB_INF.length());
        }
        else {
            return "/"+GrailsResourceUtils.WEB_APP_DIR+location;
        }
    }
}
