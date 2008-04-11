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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.codehaus.groovy.grails.support.MockResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
    private static final Log STACK_LOG  = LogFactory.getLog("StackTrace");
    private static final String GRAILS_IMPLEMENTATION_TITLE = "Grails";
    private static final String GRAILS_VERSION;
    private static final String[] GRAILS_PACKAGES = new String[] {
            "org.codehaus.groovy.grails.",
            "org.codehaus.groovy.runtime.",
            "org.codehaus.groovy.reflection.",
            "org.codehaus.gant.",
            "grails.",
            "groovy.",
            "org.mortbay.",
            "sun.",
            "java.lang.reflect.",
            "org.springframework.",
            "com.opensymphony.",
            "org.hibernate.",
            "javax.servlet."
    };

    static {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String version = null;
        try {
            Resource[] manifests = resolver.getResources("classpath*:META-INF/MANIFEST.MF");
            Manifest grailsManifest = null;
            for (int i = 0; i < manifests.length; i++) {
                Resource r = manifests[i];
                Manifest mf = new Manifest(r.getInputStream());
                String implTitle = mf.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                if(!isBlank(implTitle) && implTitle.equals(GRAILS_IMPLEMENTATION_TITLE))   {
                    grailsManifest = mf;
                    break;
                }
            }

            if(grailsManifest != null) {
                version = grailsManifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }

            if(isBlank(version)) {
                LOG.error("Unable to read Grails version from MANIFEST.MF. Are you sure the grails-core jar is on the classpath? " );
                version = "Unknown";
            }
        } catch (IOException e) {
            version = "Unknown";
            LOG.error("Unable to read Grails version from MANIFEST.MF. Are you sure it the grails-core jar is on the classpath? " + e.getMessage(), e);
        }

        GRAILS_VERSION = version;
    }

    private static final String PRODUCTION_ENV_SHORT_NAME = "prod";
    private static final String DEVELOPMENT_ENVIRONMENT_SHORT_NAME = "dev";
    private static final String TEST_ENVIRONMENT_SHORT_NAME = "test";

    private static Map envNameMappings = new HashMap() {{
        put(DEVELOPMENT_ENVIRONMENT_SHORT_NAME, GrailsApplication.ENV_DEVELOPMENT);
        put(PRODUCTION_ENV_SHORT_NAME, GrailsApplication.ENV_PRODUCTION);
        put(TEST_ENVIRONMENT_SHORT_NAME, GrailsApplication.ENV_TEST);
    }};


    /**
     * <p>Bootstraps a Grails application from the current classpath. The method will look for an applicationContext.xml file in the classpath
     * that must contain a bean of type GrailsApplication and id grailsApplication
     *
     * <p>The method will then bootstrap Grails with the GrailsApplication and load all Grails plug-ins found in the path
     *
     * @return The Grails ApplicationContext instance 
     */
    public static ApplicationContext bootstrapGrailsFromClassPath() {
		LOG.info("Loading Grails environment");
		ApplicationContext parent = new ClassPathXmlApplicationContext("applicationContext.xml");
		DefaultGrailsApplication application = (DefaultGrailsApplication)parent.getBean("grailsApplication", DefaultGrailsApplication.class);

        return createGrailsApplicationContext(parent, application);
	}

    private static ApplicationContext createGrailsApplicationContext(ApplicationContext parent, GrailsApplication application) {
        GrailsRuntimeConfigurator config = new GrailsRuntimeConfigurator(application,parent);
        MockServletContext servletContext = new MockServletContext(new MockResourceLoader());
        ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)config.configure(servletContext);
        servletContext.setAttribute( ApplicationAttributes.APPLICATION_CONTEXT, appCtx);
        Assert.notNull(appCtx);
        return appCtx;
    }

    /**
     * Bootstraps Grails with the given GrailsApplication instance
     *
     * @param application The GrailsApplication instance
     * @return A Grails ApplicationContext
     */
    public static ApplicationContext bootstrapGrailsFromApplication(GrailsApplication application) {
        MockApplicationContext parent = new MockApplicationContext();
        parent.registerMockBean(GrailsApplication.APPLICATION_ID, application);

        return createGrailsApplicationContext(parent, application);
    }      
    
	/**
	 * Bootstraps Grails from the given parent ApplicationContext which should contain a bean definition called "grailsApplication"
	 * of type GrailsApplication
	 */
	public static ApplicationContext bootstrapGrailsFromParentContext(ApplicationContext parent) {
		DefaultGrailsApplication application = (DefaultGrailsApplication)parent.getBean("grailsApplication", DefaultGrailsApplication.class);

        return createGrailsApplicationContext(parent, application);		
	}


    /**
     * Retrieves the current execution environment
     *
     * @return The environment Grails is executing under
     */
    public static String getEnvironment() {
        GrailsApplication app = ApplicationHolder.getApplication();


        String envName = null;
        if(app!=null) {
            Map metadata = app.getMetadata();
            if(metadata!=null)
                envName = (String)metadata.get(GrailsApplication.ENVIRONMENT);
        }
        if(isBlank(envName))
            envName = System.getProperty(GrailsApplication.ENVIRONMENT);

        if(isBlank(envName)) {
            return GrailsApplication.ENV_DEVELOPMENT;
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

    private static boolean isBlank(String value) {
        return value == null || value.length() == 0;
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

    /**
     * Logs warning message to grails.util.GrailsUtil logger which is turned on in development mode.
     *
     * @param message Message to display
     */
    public static void warn(String message) {
    	LOG.warn("[WARNING] " + message);
    }


    /**
     * <p>Remove all apparently Grails-internal trace entries from the exception instance<p>
     * <p>This modifies the original instance and returns it, it does not clone</p>
     * @param t
     * @return The exception passed in, after cleaning the stack trace
     */
    public static Throwable sanitize(Throwable t) {
        // Note that this getProperty access may well be synced...
        if (!Boolean.valueOf(System.getProperty("grails.full.stacktrace")).booleanValue()) {
            StackTraceElement[] trace = t.getStackTrace();
            List newTrace = new ArrayList();
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement stackTraceElement = trace[i];
                if (isApplicationClass(stackTraceElement.getClassName())) {
                    newTrace.add( stackTraceElement);
                }
            }

            // Only trim the trace if there was some application trace on the stack
            // if not we will just skip sanitizing and leave it as is
            if (newTrace.size() > 0) {
                // We don't want to lose anything, so log it
                STACK_LOG.error("Sanitizing stacktrace:", t);
                StackTraceElement[] clean = new StackTraceElement[newTrace.size()];
                newTrace.toArray(clean);
                t.setStackTrace(clean);
            }
        }
        return t;
    }

    public static void printSanitizedStackTrace(Throwable t, PrintWriter p) {
        t = GrailsUtil.sanitize(t);

        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement stackTraceElement = trace[i];
            p.println(  "at "+stackTraceElement.getClassName()
                        +"("+stackTraceElement.getMethodName()
                        +":"+stackTraceElement.getLineNumber()+")");
        }
    }

    public static void printSanitizedStackTrace(Throwable t) {
        printSanitizedStackTrace(t, new PrintWriter(System.err));
    }

    public static boolean isApplicationClass(String className) {
        for (int i = 0; i < GRAILS_PACKAGES.length; i++) {
            String grailsPackage = GRAILS_PACKAGES[i];
            if (className.startsWith(grailsPackage)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Extracts the root cause of the exception, no matter how nested it is</p>
     * @param t
     * @return The deepest cause of the exception that can be found
     */
    public static Throwable extractRootCause(Throwable t) {
        Throwable result = t;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    /**
     * <p>Get the root cause of an exception and sanitize it for display to the user</p>
     * <p>This will MODIFY the stacktrace of the root cause exception object and return it</p>
     * @param t
     * @return The root cause exception instance, with its stace trace modified to filter out grails runtime classes
     */
    public static Throwable sanitizeRootCause(Throwable t) {
        return GrailsUtil.sanitize(GrailsUtil.extractRootCause(t));
    }

    /**
     * <p>Sanitize the exception and ALL nested causes</p>
     * <p>This will MODIFY the stacktrace of the exception instance and all its causes irreversibly</p>
     * @param t
     * @return The root cause exception instances, with stack trace modified to filter out grails runtime classes
     */
    public static Throwable deepSanitize(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) {
            current = GrailsUtil.sanitize(current.getCause());
        }
        return GrailsUtil.sanitize(t);
    }
}
