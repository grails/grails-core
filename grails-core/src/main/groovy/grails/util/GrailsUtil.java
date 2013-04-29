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
package grails.util;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Writable;
import groovy.util.slurpersupport.GPathResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer;
import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
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
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Grails utility methods for command line and GUI applications.
 *
 * @author Graeme Rocher
 * @since 0.2
 */
public class GrailsUtil {

    private static final Log LOG = LogFactory.getLog(GrailsUtil.class);
    private static final String GRAILS_IMPLEMENTATION_TITLE = "Grails";
    private static final String GRAILS_VERSION;
    private static final StackTraceFilterer stackFilterer = new DefaultStackTraceFilterer();
    private static final boolean LOG_DEPRECATED = Boolean.valueOf(System.getProperty("grails.log.deprecated", String.valueOf(Environment.isDevelopmentMode())));

    static {
        Package p = GrailsUtil.class.getPackage();
        String version = p != null ? p.getImplementationVersion() : null;
        if (version == null || isBlank(version)) {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            try {
                Resource[] manifests = resolver.getResources("classpath*:META-INF/MANIFEST.MF");
                Manifest grailsManifest = null;
                for (int i = 0; i < manifests.length; i++) {
                    Resource r = manifests[i];
                    InputStream inputStream = null;
                    Manifest mf = null;
                    try {
                        inputStream = r.getInputStream();
                        mf = new Manifest(inputStream);
                    }
                    finally {
                        IOUtils.closeQuietly(inputStream);
                    }
                    String implTitle = mf.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                    if (!isBlank(implTitle) && implTitle.equals(GRAILS_IMPLEMENTATION_TITLE)) {
                        grailsManifest = mf;
                        break;
                    }
                }

                if (grailsManifest != null) {
                    version = grailsManifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                }

                if (isBlank(version)) {
                    LOG.error("Unable to read Grails version from MANIFEST.MF. Are you sure the grails-core jar is on the classpath? ");
                    version = "Unknown";
                }
            }
            catch (Exception e) {
                version = "Unknown";
                LOG.error("Unable to read Grails version from MANIFEST.MF. Are you sure it the grails-core jar is on the classpath? " + e.getMessage(), e);
            }
        }
        GRAILS_VERSION = version;
    }

    /**
     * <p>Bootstraps a Grails application from the current classpath. The method will look for an
     * applicationContext.xml file in the classpath that must contain a bean of type
     * GrailsApplication and id grailsApplication.
     *
     * <p>The method will then bootstrap Grails with the GrailsApplication and load all Grails plug-ins found in the path
     *
     * @return The Grails ApplicationContext instance
     */
    public static ApplicationContext bootstrapGrailsFromClassPath() {
        LOG.info("Loading Grails environment");
        ApplicationContext parent = new ClassPathXmlApplicationContext("applicationContext.xml");
        DefaultGrailsApplication application = parent.getBean("grailsApplication", DefaultGrailsApplication.class);

        return createGrailsApplicationContext(parent, application);
    }

    private static ApplicationContext createGrailsApplicationContext(ApplicationContext parent, GrailsApplication application) {
        GrailsRuntimeConfigurator config = new GrailsRuntimeConfigurator(application,parent);
        MockServletContext servletContext = new MockServletContext(new MockResourceLoader());
        ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)config.configure(servletContext);
        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, appCtx);
        Assert.notNull(appCtx);
        return appCtx;
    }

    /**
     * Bootstraps Grails with the given GrailsApplication instance.
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
     * of type GrailsApplication.
     */
    public static ApplicationContext bootstrapGrailsFromParentContext(ApplicationContext parent) {
        DefaultGrailsApplication application = parent.getBean("grailsApplication", DefaultGrailsApplication.class);
        return createGrailsApplicationContext(parent, application);
    }

    /**
     * Retrieves the current execution environment.
     *
     * @return The environment Grails is executing under
     * @deprecated Use Environment.getCurrent() method instead
     */
    @Deprecated
    public static String getEnvironment() {
        return Environment.getCurrent().getName();
    }

    /**
     * Retrieves whether the current execution environment is the development one.
     *
     * @return true if it is the development environment
     */
    public static boolean isDevelopmentEnv() {
        return Environment.getCurrent().equals(Environment.DEVELOPMENT);
    }

    public static String getGrailsVersion() {
        return GRAILS_VERSION;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /**
     * Logs warning message about deprecation of specified property or method of some class.
     *
     * @param clazz A class
     * @param methodOrPropName Name of deprecated property or method
     */
    public static void deprecated(Class<?> clazz, String methodOrPropName) {
        deprecated(clazz, methodOrPropName, getGrailsVersion());
    }

    /**
     * Logs warning message about deprecation of specified property or method of some class.
     *
     * @param clazz A class
     * @param methodOrPropName Name of deprecated property or method
     * @param version Version of Grails release in which property or method were deprecated
     */
    public static void deprecated(Class<?> clazz, String methodOrPropName, String version) {
        if (LOG_DEPRECATED) {
            deprecated("Property or method [" + methodOrPropName + "] of class [" + clazz.getName() +
                    "] is deprecated in [" + version +
                    "] and will be removed in future releases");
        }
    }

    /**
     * Logs warning message about some deprecation and code style related hints.
     *
     * @param message Message to display
     */
    public static void deprecated(String message) {
        if (LOG_DEPRECATED && LOG.isWarnEnabled()) {
            LOG.warn("[DEPRECATED] " + message);
        }
    }

    /**
     * Logs warning message to grails.util.GrailsUtil logger which is turned on in development mode.
     *
     * @param message Message to display
     */
    public static void warn(String message) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("[WARNING] " + message);
        }
    }

    /**
     * <p>Remove all apparently Grails-internal trace entries from the exception instance<p>
     * <p>This modifies the original instance and returns it, it does not clone</p>
     * @param t The exception
     * @return The exception passed in, after cleaning the stack trace
     *
     * @deprecated Use {@link StackTraceFilterer} instead
     */
    @Deprecated
    public static Throwable sanitize(Throwable t) {
        return stackFilterer.filter(t);
    }

    public static void printSanitizedStackTrace(Throwable t, PrintWriter p) {
        printSanitizedStackTrace(t, p, stackFilterer);
    }

    public static void printSanitizedStackTrace(Throwable t, PrintWriter p, StackTraceFilterer stackTraceFilterer) {
        t = stackTraceFilterer.filter(t);

        StackTraceElement[] trace = t.getStackTrace();
        for (StackTraceElement stackTraceElement : trace) {
            p.println("at " + stackTraceElement.getClassName() +
                      "(" + stackTraceElement.getMethodName() +
                      ":" + stackTraceElement.getLineNumber() + ")");
        }
    }

    public static void printSanitizedStackTrace(Throwable t) {
        printSanitizedStackTrace(t, new PrintWriter(System.err));
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
        return stackFilterer.filter(extractRootCause(t));
    }

    /**
     * <p>Sanitize the exception and ALL nested causes</p>
     * <p>This will MODIFY the stacktrace of the exception instance and all its causes irreversibly</p>
     * @param t
     * @return The root cause exception instances, with stack trace modified to filter out grails runtime classes
     */
    public static Throwable deepSanitize(Throwable t) {
        return stackFilterer.filter(t, true);
    }

    /**
     * Writes out a GPathResult (i.e. the result of parsing XML using
     * XmlSlurper) to the given writer.
     * @param result The root node of the XML to write out.
     * @param output Where to write the XML to.
     * @throws IOException If the writing fails due to a closed stream or unwritable file.
     * @deprecated Will be removed in a future release
     */
    @Deprecated
    public static void writeSlurperResult(GPathResult result, Writer output) throws IOException {
        Binding b = new Binding();
        b.setVariable("node", result);
        // this code takes the XML parsed by XmlSlurper and writes it out using StreamingMarkupBuilder
        // don't ask me how it works, refer to John Wilson ;-)
        Writable w = (Writable)new GroovyShell(b).evaluate(
                "new groovy.xml.StreamingMarkupBuilder().bind {" +
                " mkp.declareNamespace(\"\":  \"http://java.sun.com/xml/ns/j2ee\");" +
                " mkp.yield node}");
        w.writeTo(output);
    }
}
