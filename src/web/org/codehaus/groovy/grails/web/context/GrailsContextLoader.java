/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.context;

import groovy.lang.ExpandoMetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

import grails.util.GrailsUtil;

/**
 * @author graemerocher
 *
 */
public class GrailsContextLoader extends ContextLoader {

	public static final Log LOG = LogFactory.getLog(GrailsContextLoader.class);
	
	protected WebApplicationContext createWebApplicationContext(ServletContext servletContext, ApplicationContext parent) throws BeansException {

        ExpandoMetaClass.enableGlobally();

        if(LOG.isDebugEnabled()) {
			LOG.debug("[GrailsContextLoader] Loading context. Creating parent application context");
		}
        WebApplicationContext  ctx = null;
        try {
            ctx = super.createWebApplicationContext(servletContext, parent);

            GrailsApplication application = (GrailsApplication) ctx.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
            ctx =  GrailsConfigUtils.configureWebApplicationContext(servletContext, ctx);
            GrailsConfigUtils.executeGrailsBootstraps(application, ctx, servletContext);
        } catch (BeansException e) {
            GrailsUtil.deepSanitize(e);
            throw e; // rethrow sanitized version, as we have nowhere above us on stack that catches
        }
        return ctx;
    }

}
