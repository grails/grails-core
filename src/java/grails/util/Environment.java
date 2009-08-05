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


import java.util.HashMap;
import java.util.Locale;


/**
 * An enum that represents the current environment
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Dec 12, 2008
 */
public enum Environment {
    DEVELOPMENT, PRODUCTION, TEST, APPLICATION, CUSTOM;
    /**
     * Constant used to resolve the environment via System.getProperty(Environment.KEY)
     */
    public static String KEY = "grails.env";

    /**
     * Specify whether reloading is enabled for this environment
     */
    public static String RELOAD_ENABLED = "grails.reload.enabled";

    /**
     * The location where to reload resources from
     */
    public static final String RELOAD_LOCATION = "grails.reload.location";


    /**
     * Constants that indicates whether this GrailsApplication is running in the default environment
     */
    public static final String DEFAULT = "grails.env.default";
    private static final String PRODUCTION_ENV_SHORT_NAME = "prod";
    private static final String DEVELOPMENT_ENVIRONMENT_SHORT_NAME = "dev";

    private static final String TEST_ENVIRONMENT_SHORT_NAME = "test";
    private static HashMap<String, String> envNameMappings = new HashMap<String, String>() {{
        put(DEVELOPMENT_ENVIRONMENT_SHORT_NAME, Environment.DEVELOPMENT.getName());
        put(PRODUCTION_ENV_SHORT_NAME, Environment.PRODUCTION.getName());
        put(TEST_ENVIRONMENT_SHORT_NAME, Environment.TEST.getName());
    }};

    /**
     * Returns the current environment which is typcally either DEVELOPMENT, PRODUCTION or TEST.
     * For custom environments CUSTOM type is returned.
     *
     * @return The current environment.
     */
    public static Environment getCurrent() {

        String envName = System.getProperty(Environment.KEY);
        Metadata metadata = Metadata.getCurrent();
        if(metadata!=null && isBlank(envName)) {
            envName = metadata.getEnvironment();
        }

        if(isBlank(envName)) {
            return DEVELOPMENT;
        }
        else {
            Environment env = getEnvironment(envName);
            if(env == null) {
                try {
                    env = Environment.valueOf(envName.toUpperCase());
                }
                catch (IllegalArgumentException e) {
                    // ignore
                }
            }
            if(env == null) {
                env = Environment.CUSTOM;
                env.setName(envName);
            }
            return env;
        }

    }

    /**
     * @see #getCurrent()
     */
    public static Environment getCurrentEnvironment() {
        return getCurrent();        
    }


    /**
     * @return Return true if the environment has been set as a Systme property
     */
    public static boolean isSystemSet() {
        return System.getProperty(KEY) !=null;
    }

    /**
     * Returns the environment for the given short name
     * @param shortName The short name
     * @return The Environment or null if not known
     */
    public static Environment getEnvironment(String shortName) {
        final String envName = envNameMappings.get(shortName);
        if(envName !=null) {
            return Environment.valueOf(envName.toUpperCase());
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }


    private String name;

    /**
     * @return The name of the environment 
     */
    public String getName() {
        if(name == null) {
            return this.toString().toLowerCase(Locale.getDefault());
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns whether reload is enabled for the environment
     */
    public boolean isReloadEnabled() {
        final boolean reloadOverride = Boolean.getBoolean(RELOAD_ENABLED);

        final String reloadLocation = getReloadLocationInternal();
        final boolean reloadLocationSpecified = isNotBlank(reloadLocation);
        return this == DEVELOPMENT && reloadLocationSpecified && !Metadata.getCurrent().isWarDeployed() || reloadOverride && reloadLocationSpecified;
    }

    private boolean isNotBlank(String reloadLocation) {
        return (reloadLocation !=null && reloadLocation.length()>0 );
    }

    /**
     * @return Obtains the location to reload resources from
     */
    public String getReloadLocation() {
        String location = getReloadLocationInternal();
        if(isNotBlank(location)) {
            return location;
        }
        else {
            throw new IllegalStateException("Cannot check reload enabled, ["+RELOAD_LOCATION+"] or ["+BuildSettings.APP_BASE_DIR+"] not set! Specify the system property.") ;
        }
    }

    /**
     * @return Whether a reload location is specified
     */
    public boolean hasReloadLocation() {
        return isNotBlank(getReloadLocationInternal());
    }

    private String getReloadLocationInternal() {
        String location = System.getProperty(RELOAD_LOCATION);
        if(!isNotBlank(location)) location = System.getProperty(BuildSettings.APP_BASE_DIR);
        return location;
    }
}
