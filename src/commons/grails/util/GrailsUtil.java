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
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.support.MockResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.io.IOException;

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
    private static final String GRAILS_IMPLEMENTATION_TITLE = "Grails";
    private static final String GRAILS_VERSION;

    static {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] manifests = resolver.getResources("classpath*:META-INF/GRAILS-MANIFEST.MF");
            Manifest grailsManifest = null;
            for (int i = 0; i < manifests.length; i++) {
                Resource r = manifests[i];
                Manifest mf = new Manifest(r.getInputStream());
                String implTitle = mf.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                if(!StringUtils.isBlank(implTitle) && implTitle.equals(GRAILS_IMPLEMENTATION_TITLE))   {
                    grailsManifest = mf;
                    break;
                }
            }
            String version = null;
            if(grailsManifest != null) {
                version = grailsManifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }

            if(!StringUtils.isBlank(version)) {
                GRAILS_VERSION = version;
            }
            else {
                GRAILS_VERSION = null;
                throw new GrailsConfigurationException("Unable to read Grails version from MANIFEST.MF. Are you sure it the grails-core jar is on the classpath? " );
            }
        } catch (IOException e) {
            throw new GrailsConfigurationException("Unable to read Grails version from MANIFEST.MF. Are you sure it the grails-core jar is on the classpath? " + e.getMessage(), e);
        }


    }

    private static final String PRODUCTION_ENV_SHORT_NAME = "prod";
    private static final String DEVELOPMENT_ENVIRONMENT_SHORT_NAME = "dev";
    private static final String TEST_ENVIRONMENT_SHORT_NAME = "test";
    
    private static Map envNameMappings = new HashMap() {{
        put(DEVELOPMENT_ENVIRONMENT_SHORT_NAME, GrailsApplication.ENV_DEVELOPMENT);
        put(PRODUCTION_ENV_SHORT_NAME, GrailsApplication.ENV_PRODUCTION);
        put(TEST_ENVIRONMENT_SHORT_NAME, GrailsApplication.ENV_TEST);
    }};


    public static ApplicationContext bootstrapGrailsFromClassPath() {
		LOG.info("Loading Grails environment");
		ApplicationContext parent = new ClassPathXmlApplicationContext("applicationContext.xml");
		DefaultGrailsApplication application = (DefaultGrailsApplication)parent.getBean("grailsApplication", DefaultGrailsApplication.class);
		
		GrailsRuntimeConfigurator config = new GrailsRuntimeConfigurator(application,parent);
		MockServletContext servletContext = new MockServletContext(new MockResourceLoader());
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


    public static String getGrailsVersion() {
        return GRAILS_VERSION;
    }
    
    /**
     * Logs warning message about deprecation of specified property or method of some class.
     * 
     * @param clazz A class
     * @param methodOrPropName Name of deprecated property or method
     */
    public static void deprecated(Class clazz, String methodOrPropName ) {
    	deprecated(clazz, methodOrPropName, getGrailsVersion());
    }

    /**
     * Logs warning message about deprecation of specified property or method of some class.
     * 
     * @param clazz A class
     * @param methodOrPropName Name of deprecated property or method
     * @param version Version of Grails release in which property or method were deprecated
     */
    public static void deprecated(Class clazz, String methodOrPropName, String version ) {
    	deprecated("Property or method [" + methodOrPropName + "] of class [" + clazz.getName() + 
    			"] is deprecated in [" + getGrailsVersion() + 
    			"] and will be removed in future releases");
    }

    /**
     * Logs warning message about some deprecation and code style related hints.
     * 
     * @param message Message to display
     */
    public static void deprecated(String message) {
    	LOG.warn("[DEPRECATED] " + message);
    }


}
