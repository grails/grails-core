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
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Grails utility methods for command line and GUI applications
 *
 * @author Graeme Rocher
 * @since 0.2
 * 
 * @version $Revision$
 * First Created: 02-Jun-2006
 * Last Updated: $Date$
 *
 */
public class GrailsUtil {

	private static final Log LOG  = LogFactory.getLog(GrailsUtil.class);


    private static Map envNameMappings = new HashMap() {{
        put("dev", GrailsApplication.ENV_DEVELOPMENT);
        put("prod", GrailsApplication.ENV_PRODUCTION);
        put("test", GrailsApplication.ENV_TEST);
    }};

    public static ApplicationContext bootstrapGrailsFromClassPath() {
		LOG.info("Loading Grails environment");
		ApplicationContext parent = new ClassPathXmlApplicationContext("applicationContext.xml");
		DefaultGrailsApplication application = (DefaultGrailsApplication)parent.getBean("grailsApplication", DefaultGrailsApplication.class);
		
		GrailsRuntimeConfigurator config = new GrailsRuntimeConfigurator(application,parent);
		MockServletContext servletContext = new MockServletContext();
		ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)config.configure(servletContext);
		servletContext.setAttribute( ApplicationAttributes.APPLICATION_CONTEXT, appCtx);
		Assert.notNull(appCtx);
		return appCtx;
	}

    /**
     * Retrieves the current execution environment
     *
     * @return The environment Grails is executing under
     */
    public static String getEnvironment() {
        String envName = System.getProperty(GrailsApplication.ENVIRONMENT);
        if(StringUtils.isBlank(envName)) {
            // for now if no environment specified default to production
            return GrailsApplication.ENV_PRODUCTION;                
        }
        else {
            if(envNameMappings.containsKey(envName)) {
                return (String)envNameMappings.get(envName);
            }
            else {
                return envName;
            }
        }
    }

    /**
     * Retrieves whether the current execution environment is the development one
     *
     * @return True if it is the development environment
     */
    public static boolean isDevelopmentEnv() {
        return GrailsApplication.ENV_DEVELOPMENT.equals(GrailsUtil.getEnvironment());
    }
}
