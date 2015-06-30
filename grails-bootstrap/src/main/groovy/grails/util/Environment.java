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

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;

import java.io.File;
import java.util.Locale;
import java.util.Map;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.grails.io.support.MainClassFinder;

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
     * The name of the GRAILS_HOME environment variable
     */
    public static String ENV_GRAILS_HOME = "GRAILS_HOME";
    /**
     * Specify whether reloading is enabled for this environment
     */
    public static String RELOAD_ENABLED = "grails.reload.enabled";

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

    private static final String PRODUCTION_ENV_SHORT_NAME = "prod";

    private static final String DEVELOPMENT_ENVIRONMENT_SHORT_NAME = "dev";
    private static final String TEST_ENVIRONMENT_SHORT_NAME = "test";

    @SuppressWarnings("unchecked")
    private static Map<String, String> envNameMappings = CollectionUtils.<String, String>newMap(
        DEVELOPMENT_ENVIRONMENT_SHORT_NAME, Environment.DEVELOPMENT.getName(),
        PRODUCTION_ENV_SHORT_NAME, Environment.PRODUCTION.getName(),
        TEST_ENVIRONMENT_SHORT_NAME, Environment.TEST.getName());
    private static Holder<Environment> cachedCurrentEnvironment = new Holder<Environment>("Environment");
    private static final boolean cachedHasGrailsHome = System.getProperty("grails.home") != null;
    private static boolean initializingState = false;
    public static Throwable currentReloadError = null;
    public static MultipleCompilationErrorsException currentCompilationError = null;
    private String name;

    Environment() {
        initialize();
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
        Environment current = cachedCurrentEnvironment.get();
        if (current != null) {
            return current;
        }

        return resolveCurrentEnvironment();
    }

    private static Environment resolveCurrentEnvironment() {
        String envName = System.getProperty(Environment.KEY);

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

    public static void cacheCurrentEnvironment() {
        cachedCurrentEnvironment.set(resolveCurrentEnvironment());
    }

    /**
     * @see #getCurrent()
     * @return the current environment
     */
    public static Environment getCurrentEnvironment() {
        return getCurrent();
    }

    /**
     * Returns true if the application is running in development mode (within grails run-app)
     * @return true if the application is running in development mode
     */
    public static boolean isDevelopmentMode() {
        return getCurrent() == DEVELOPMENT && Metadata.getCurrent().isDevelopmentEnvironmentAvailable() &&
                cachedHasGrailsHome;
    }

    /**
     * Check whether the application is deployed
     * @return true if is
     */
    public static boolean isWarDeployed() {
        return Metadata.getCurrent().isWarDeployed();
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
     * @return Return true if the environment has been set as a Systme property
     */
    public static boolean isSystemSet() {
        return System.getProperty(KEY) != null;
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

        final String reloadLocation = getReloadLocationInternal();
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
    public static boolean isReloadingAgentEnabled() {
        try {
            Class.forName("org.springsource.loaded.TypeRegistry");
            return Environment.getCurrent().isReloadEnabled();
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * @return Obtains the location to reload resources from
     */
    public String getReloadLocation() {
        String location = getReloadLocationInternal();
        if (hasLocation(location)) {
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
        String reloadingLocation = getReloadLocationInternal();
        return hasLocation(reloadingLocation);
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

                }
            }
        }
        return location;
    }
}
