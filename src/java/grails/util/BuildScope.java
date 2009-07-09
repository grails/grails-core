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

/**
 * An enum that represents the different scopes that plugins apply to
 * 
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Dec 12, 2008
 */
public enum BuildScope {
    TEST, WAR, RUN, ALL, FUNCTIONAL_TEST;

    /**
     * The key used to lookup the build scope in the System properties
     */
    public static final String KEY = "grails.buildScope";


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    /**
     * Returns the current Scope object based on the currently set "grails.scope" System property
     * @return The Scope object
     */
    public static BuildScope getCurrent() {
        String key = System.getProperty(KEY);
        if(key== null) {
            key = (String) Metadata.getCurrent().get(KEY);
        }

        if(key!=null) {
            try {
                return BuildScope.valueOf(key.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                return BuildScope.ALL;
            }
        }
        return BuildScope.ALL;
    }

    /**
     * Returns whether the specified scope name(s) are valid given the current scope
     *
     * @param scopeNames The list of scope names
     * @return True if they are valid
     */
    public static boolean isValid(String... scopeNames) {
        BuildScope currentScope = getCurrent();
        if(currentScope.equals(ALL)) return true;
        for (String scopeName : scopeNames) {
            BuildScope specifiedScope = BuildScope.valueOf(scopeName.toUpperCase());
            if (currentScope == specifiedScope) return true;
        }
        return false;
    }

    /**
     * Enables this build scope as the curent system wide instance
     */
    public void enable() {
        System.setProperty(KEY, this.toString());
    }
}
