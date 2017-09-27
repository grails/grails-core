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

import grails.io.IOUtils;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.grails.io.support.Resource;
import org.grails.io.support.UrlResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Represents the current environment.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public enum Environment {

    /** The development environment */
    DEVELOPMENT,

    /** The production environment */
    PRODUCTION,

    /** The test environment */
    TEST,

    /**
     * For the application data source, primarly for backward compatability for those applications
     * that use ApplicationDataSource.groovy.
     */
    APPLICATION,

    /** A custom environment */
    CUSTOM;

    /**
     * Constant used to resolve the environment via System.getProperty(Environment.KEY)
     */
    public static String KEY = "grails.env";
    
    /**
     * Constant used to resolve the environment via System.getenv(Environment.ENV_KEY).
     */
    public static final String ENV_KEY = "GRAILS_ENV";

    /**
     * The name of the GRAILS_HOME environment variable
     */
    public static String ENV_GRAILS_HOME = "GRAILS_HOME";
    /**
     * Specify whether reloading is enabled for this environment
     */
    public static String RELOAD_ENABLED = "grails.reload.enabled";

    /**
     * Constant indicating whether run-app or test-app was executed
     */
    public static String RUN_ACTIVE = "grails.run.active";

    /**
     * Whether the display of full stack traces is needed
     */
    public static String FULL_STACKTRACE = "grails.full.stacktrace";

    /**
     * The location where to reload resources from
     */
    public static final String RELOAD_LOCATION = "grails.reload.location";

    /**
     * Whether interactive mode is enabled
     */
    public static final String INTERACTIVE_MODE_ENABLED = "grails.interactive.mode.enabled";

    /**
     * Constants that indicates whether this GrailsApplication is running in the default environment
     */
    public static final String DEFAULT = "grails.env.default";

    /**
     * Whether Grails is in the middle of bootstrapping or not
     */
    public static final String INITIALIZING = "grails.env.initializing";

    /**
     * Whether Grails has been executed standalone via the static void main method and not loaded in via the container
     */
    public static final String STANDALONE = "grails.env.standalone";

    private static final String PRODUCTION_ENV_SHORT_NAME = "prod";

    private static final String DEVELOPMENT_ENVIRONMENT_SHORT_NAME = "dev";
    private static final String TEST_ENVIRONMENT_SHORT_NAME = "test";

    @SuppressWarnings("unchecked")
    private static Map<String, String> envNameMappings = CollectionUtils.<String, String>newMap(
        DEVELOPMENT_ENVIRONMENT_SHORT_NAME, Environment.DEVELOPMENT.getName(),
        PRODUCTION_ENV_SHORT_NAME, Environment.PRODUCTION.getName(),
        TEST_ENVIRONMENT_SHORT_NAME, Environment.TEST.getName());
    private static Holder<Environment> cachedCurrentEnvironment = new Holder<>("Environment");
    private static final boolean DEVELOPMENT_MODE = getCurrent() == DEVELOPMENT && BuildSettings.GRAILS_APP_DIR_PRESENT;
    private static boolean initializingState = false;

    private static final String GRAILS_IMPLEMENTATION_TITLE = "Grails";
    private static final String GRAILS_VERSION;
    private static final boolean STANDALONE_DEPLOYED;
    private static final boolean WAR_DEPLOYED;

    static {
        Package p = Environment.class.getPackage();
        String version = p != null ? p.getImplementationVersion() : null;
        if (version == null || isBlank(version)) {
            try {
                URL manifestURL = IOUtils.findResourceRelativeToClass(Environment.class, "/META-INF/MANIFEST.MF");
                Manifest grailsManifest = null;
                if(manifestURL != null) {
                    Resource r = new UrlResource(manifestURL);
                    if(r.exists()) {
                        InputStream inputStream = null;
                        Manifest mf = null;
                        try {
                            inputStream = r.getInputStream();
                            mf = new Manifest(inputStream);
                        } finally {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                        String implTitle = mf.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                        if (!isBlank(implTitle) && implTitle.equals(GRAILS_IMPLEMENTATION_TITLE)) {
                            grailsManifest = mf;
                        }
                    }
                }

                if (grailsManifest != null) {
                    version = grailsManifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                }

                if (isBlank(version)) {
                    version = "Unknown";
                }
            }
            catch (Exception e) {
                version = "Unknown";
            }
        }
        GRAILS_VERSION = version;

        URL url = Environment.class.getResource("");
        if(url != null) {

            String protocol = url.getProtocol();
            if(protocol.equals("jar")) {
                String fullPath = url.toString();
                if(fullPath.contains(IOUtils.RESOURCE_WAR_PREFIX)) {
                    STANDALONE_DEPLOYED = true;
                }
                else {
                    int i = fullPath.indexOf(IOUtils.RESOURCE_JAR_PREFIX);
                    if(i > -1) {
                        fullPath = fullPath.substring(i + IOUtils.RESOURCE_JAR_PREFIX.length());
                        STANDALONE_DEPLOYED = fullPath.contains(IOUtils.RESOURCE_JAR_PREFIX);

                    }
                    else {
                        STANDALONE_DEPLOYED = false;
                    }

                }
            }
            else {
                STANDALONE_DEPLOYED = false;
            }
        }
        else {
            STANDALONE_DEPLOYED = false;
        }

        URL loadedLocation = Environment.class.getClassLoader().getResource(Metadata.FILE);
        if(loadedLocation != null ) {
            String path = loadedLocation.getPath();
            WAR_DEPLOYED = isWebPath(path);
        }
        else {

            loadedLocation = Thread.currentThread().getContextClassLoader().getResource(Metadata.FILE);
            if(loadedLocation != null ) {
                String path = loadedLocation.getPath();
                WAR_DEPLOYED = isWebPath(path);
            }
            else {
                WAR_DEPLOYED = false;
            }
        }
    }

    public static Throwable currentReloadError = null;
    public static MultipleCompilationErrorsException currentCompilationError = null;
    private String name;
    private String reloadLocation;

    Environment() {
        initialize();
    }

    /**
     * @return The current Grails version
     */
    public static String getGrailsVersion() {
        return GRAILS_VERSION;
    }
    public static void setCurrentReloadError(Throwable currentReloadError) {
        Environment.currentReloadError = currentReloadError;
    }

    public static MultipleCompilationErrorsException getCurrentCompilationError() {
        return currentCompilationError;
    }

    public static Throwable getCurrentReloadError() {
        return currentReloadError;
    }

    public static boolean isReloadInProgress() {
        return Boolean.getBoolean("grails.reloading.in.progress");
    }

    private void initialize() {
        name = toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Returns the current environment which is typcally either DEVELOPMENT, PRODUCTION or TEST.
     * For custom environments CUSTOM type is returned.
     *
     * @return The current environment.
     */
    public static Environment getCurrent() {
        String envName = getEnvironment();

        Environment env;
        if(!isBlank(envName)) {
            env = getEnvironment(envName);
            if(env != null) {
                return env;
            }
        }


        Environment current = cachedCurrentEnvironment.get();
        if (current != null) {
            return current;
        }
        return cacheCurrentEnvironment();
    }

    private static Environment resolveCurrentEnvironment() {
        String envName = getEnvironment();

        if (isBlank(envName)) {
            Metadata metadata = Metadata.getCurrent();
            if (metadata != null) {
                envName = metadata.getEnvironment();
            }
            if (isBlank(envName)) {
                return DEVELOPMENT;
            }
        }

        Environment env = getEnvironment(envName);
        if (env == null) {
            try {
                env = Environment.valueOf(envName.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                // ignore
            }
        }
        if (env == null) {
            env = Environment.CUSTOM;
            env.setName(envName);
        }
        return env;
    }

    private static Environment cacheCurrentEnvironment() {
        Environment env = resolveCurrentEnvironment();
        cachedCurrentEnvironment.set(env);
        return env;
    }

    /**
     * @see #getCurrent()
     * @return the current environment
     */
    public static Environment getCurrentEnvironment() {
        return getCurrent();
    }

    /**
     * Reset the current environment
     */
    public static void reset() {
        cachedCurrentEnvironment.set(null);
        Metadata.reset();
    }

    /**
     * Returns true if the application is running in development mode (within grails run-app)
     *
     * @return true if the application is running in development mode
     */

    public static boolean isDevelopmentMode() {
        return DEVELOPMENT_MODE;
    }

    /**
     * This method will return true if the 'grails-app' directory was found, regardless of whether reloading is active or not
     *
     * @return True if the development sources are present
     */
    public static boolean isDevelopmentEnvironmentAvailable() {
        return BuildSettings.GRAILS_APP_DIR_PRESENT && !isStandaloneDeployed() && !isWarDeployed();
    }

    /**
     * This method will return true the application is run
     *
     * @return True if the development sources are present
     */
    public static boolean isDevelopmentRun() {
        Environment env = Environment.getCurrent();
        return isDevelopmentEnvironmentAvailable() && Boolean.getBoolean(RUN_ACTIVE) && (env == Environment.DEVELOPMENT);
    }
    
    /**
     * Check whether the application is deployed
     * @return true if is
     */
    public static boolean isWarDeployed() {
        if(!isStandalone()) {
            return WAR_DEPLOYED;
        }
        return false;
    }

    private static boolean isWebPath(String path) {
        // Workaround for weblogic who repacks files from 'classes' into a new jar under lib/
        return path.contains("/WEB-INF/classes") || path.contains("_wl_cls_gen.jar!/");
    }

    /**
     * Whether the application has been executed standalone via static void main.
     *
     * This method will return true when the application is executed via `java -jar` or
     * if the application is run directly via the main method within an IDE
     *
     * @return True if it is running standalone outside of a servlet container
     */
    public static boolean isStandalone() {
        return Boolean.getBoolean(STANDALONE);
    }

    /**
     * Whether the application is running standalone within a JAR
     *
     * This method will return true only if the the application is executed via `java -jar`
     * and not if it is run via the main method within an IDE
     *
     * @return True if it is running standalone outside a servlet container from within a JAR or WAR file
     */
    public static boolean isStandaloneDeployed() {
        return isStandalone() && STANDALONE_DEPLOYED;
    }

    /**
     * Whether this is a fork of the Grails command line environment
     * @return True if it is a fork
     */
    public static boolean isFork() {
        return Boolean.getBoolean("grails.fork.active");
    }

    /**
     * Returns whether the environment is running within the Grails shell (executed via the 'grails' command line in a terminal window)
     * @return true if is
     */
    public static boolean isWithinShell() {
        return DefaultGroovyMethods.getRootLoader(Environment.class.getClassLoader()) != null;
    }

    /**
     * @return Return true if the environment has been set as a System property
     */
    public static boolean isSystemSet() {
        return getEnvironment() != null;
    }

    /**
     * Returns the environment for the given short name
     * @param shortName The short name
     * @return The Environment or null if not known
     */
    public static Environment getEnvironment(String shortName) {
        final String envName = envNameMappings.get(shortName);
        if (envName != null) {
            return Environment.valueOf(envName.toUpperCase());
        }
        else {
            try {
                return Environment.valueOf(shortName.toUpperCase());
            } catch ( IllegalArgumentException ise ) {
                return null;
            }
        }
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And returns the closure that relates to the current environment
     *
     * @param closure The top level closure
     * @return The environment specific block or null if non exists
     */
    public static Closure<?> getEnvironmentSpecificBlock(Closure<?> closure) {
        final Environment env = getCurrent();
        return getEnvironmentSpecificBlock(env, closure);
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And returns the closure that relates to the specified
     *
     * @param env The environment to use
     * @param closure The top level closure
     * @return The environment specific block or null if non exists
     */
    public static Closure<?> getEnvironmentSpecificBlock(Environment env, Closure<?> closure) {
        if (closure == null) {
            return null;
        }

        final EnvironmentBlockEvaluator evaluator = evaluateEnvironmentSpecificBlock(env, closure);
        return evaluator.getCallable();
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And executes the closure that relates to the current environment
     *
     * @param closure The top level closure
     * @return The result of the closure execution
     */
    public static Object executeForCurrentEnvironment(Closure<?> closure) {
        final Environment env = getCurrent();
        return executeForEnvironment(env, closure);
    }

    /**
     * Takes an environment specific DSL block like:
     *
     * <code>
     * environments {
     *      development {}
     *      production {}
     * }
     * </code>
     *
     * And executes the closure that relates to the specified environment
     *
     * @param env The environment to use
     * @param closure The top level closure
     * @return The result of the closure execution
     */
    public static Object executeForEnvironment(Environment env, Closure<?> closure) {
        if (closure == null) {
            return null;
        }

        final EnvironmentBlockEvaluator evaluator = evaluateEnvironmentSpecificBlock(env, closure);
        return evaluator.execute();
    }

    private static EnvironmentBlockEvaluator evaluateEnvironmentSpecificBlock(Environment environment, Closure<?> closure) {
        final EnvironmentBlockEvaluator evaluator = new EnvironmentBlockEvaluator(environment);
        closure.setDelegate(evaluator);
        closure.call();
        return evaluator;
    }

    private static class EnvironmentBlockEvaluator extends GroovyObjectSupport {
        private Environment current;
        private Closure<?> callable;

        public Closure<?> getCallable() {
            return callable;
        }

        Object execute() {
            return callable == null ? null : callable.call();
        }

        private EnvironmentBlockEvaluator(Environment e) {
            current = e;
        }

        @SuppressWarnings("unused")
        public void environments(Closure<?> c) {
            if (c != null) {
                c.setDelegate(this);
                c.call();
            }
        }
        @SuppressWarnings("unused")
        public void production(Closure<?> c) {
            if (current == Environment.PRODUCTION) {
                callable = c;
            }
        }
        @SuppressWarnings("unused")
        public void development(Closure<?> c) {
            if (current == Environment.DEVELOPMENT) {
                callable = c;
            }
        }
        @SuppressWarnings("unused")
        public void test(Closure<?> c) {
            if (current == Environment.TEST) {
                callable = c;
            }
        }

        @SuppressWarnings("unused")
        public Object methodMissing(String name, Object args) {
            Object[] argsArray = (Object[])args;
            if (args != null && argsArray.length > 0 && (argsArray[0] instanceof Closure)) {
                if (current == Environment.CUSTOM && current.getName().equals(name)) {
                    callable = (Closure<?>) argsArray[0];
                }
                return null;
            }
            throw new MissingMethodException(name, Environment.class, argsArray);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /**
     * @return  the name of the environment
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     * @param name  the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns whether reload is enabled for the environment
     */
    public boolean isReloadEnabled() {
        final boolean reloadOverride = Boolean.getBoolean(RELOAD_ENABLED);
        getReloadLocation();
        final boolean reloadLocationSpecified = hasLocation(reloadLocation);
        return this == DEVELOPMENT && reloadLocationSpecified ||
                reloadOverride && reloadLocationSpecified;
    }

    /**
     *
     * @return Whether interactive mode is enabled
     */
    public static boolean isInteractiveMode() {
        return Boolean.getBoolean(INTERACTIVE_MODE_ENABLED);
    }

    /**
     *
     * @return Whether interactive mode is enabled
     */
    public static boolean isInitializing() {
        return initializingState;
    }

    public static void setInitializing(boolean initializing) {
        initializingState = initializing;
        System.setProperty(INITIALIZING, String.valueOf(initializing));
    }

    /**
     * @return true if the reloading agent is active
     */
    private static Boolean reloadingAgentEnabled = null;
    public static boolean isReloadingAgentEnabled() {
        if(reloadingAgentEnabled != null) {
            return reloadingAgentEnabled;
        }
        try {
            Class.forName("org.springsource.loaded.TypeRegistry");
            reloadingAgentEnabled = Environment.getCurrent().isReloadEnabled();
        }
        catch (ClassNotFoundException e) {
            reloadingAgentEnabled = false;
        }
        return reloadingAgentEnabled;
    }

    /**
     * @return Obtains the location to reload resources from
     */
    public String getReloadLocation() {
        if(this.reloadLocation != null) {
            return this.reloadLocation;
        }
        String location = getReloadLocationInternal();
        if (hasLocation(location)) {
            reloadLocation = location;
            return location;
        }
        return "."; // default to the current directory
    }

    private boolean hasLocation(String location) {
        return location != null && location.length() > 0;
    }

    /**
     * @return Whether a reload location is specified
     */
    public boolean hasReloadLocation() {
        getReloadLocation();
        return hasLocation(reloadLocation);
    }

    private String getReloadLocationInternal() {
        String location = System.getProperty(RELOAD_LOCATION);
        if(!hasLocation(location)) {
            location = System.getProperty(BuildSettings.APP_BASE_DIR);
        }
        if(!hasLocation(location)) {
            File current = new File(".", "grails-app");
            if(current.exists()) {
                location = current.getParentFile().getAbsolutePath();
            }
            else {
                current = new File(".", "settings.gradle");
                if(current.exists()) {
                    // multi-project build
                    location = IOUtils.findApplicationDirectory();
                }
            }
        }
        return location;
    }

    private static String getEnvironment() {
        String envName = System.getProperty(Environment.KEY);
        return isBlank(envName) ? System.getenv(Environment.ENV_KEY) : envName;
    }

}
