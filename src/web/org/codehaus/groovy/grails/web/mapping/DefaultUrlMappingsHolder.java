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
package org.codehaus.groovy.grails.web.mapping;

import org.codehaus.groovy.grails.validation.ConstrainedProperty;

import java.util.*;

/**
 * <p>The default implementation of the UrlMappingsHolder interface that takes a list of mappings and
 * then sorts them according to their precdence rules as defined in the implementation of Comparable
 *
 * @see org.codehaus.groovy.grails.web.mapping.UrlMapping
 * @see Comparable
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Mar 6, 2007
 *        Time: 8:21:00 AM
 */
public class DefaultUrlMappingsHolder implements UrlMappingsHolder {

    private List urlMappings = new ArrayList();
    private UrlMapping[] mappings;
    private Map mappingsLookup = new HashMap();


    public DefaultUrlMappingsHolder(List mappings) {
        this.urlMappings = mappings;
        initialize();
    }

    private void initialize() {
        Collections.sort(this.urlMappings);
        Collections.reverse(this.urlMappings);
        
        this.mappings = (UrlMapping[])this.urlMappings.toArray(new UrlMapping[this.urlMappings.size()]);

        for (int i = 0; i < mappings.length; i++) {
            UrlMapping mapping = mappings[i];
            String controllerName = mapping.getControllerName() instanceof String ? mapping.getControllerName().toString() : null;
            String actionName = mapping.getActionName() instanceof String ? mapping.getActionName().toString() : null;

            ConstrainedProperty[] params = mapping.getConstraints();
            Set requiredParams = new HashSet();
            int optionalIndex = -1;
            for (int j = 0; j < params.length; j++) {
                ConstrainedProperty param = params[j];
                if(!param.isNullable()) {
                    requiredParams.add(param.getPropertyName());
                }
                else {
                   optionalIndex = j;
                    break;
                }
            }
            this.mappingsLookup.put(new UrlMappingKey(controllerName, actionName, requiredParams), mapping);

            Set requiredParamsAndOptionals = new HashSet(requiredParams);
            if(optionalIndex > -1) {
                for (int j = optionalIndex; j < params.length; j++) {
                    ConstrainedProperty param = params[j];
                    requiredParamsAndOptionals.add(param.getPropertyName());
                    UrlMappingKey key = new UrlMappingKey(controllerName, actionName, new HashSet(requiredParamsAndOptionals));
                    mappingsLookup.put(key, mapping);                    
                }
            }
        }
    }

    public UrlMapping[] getUrlMappings() {
        return this.mappings;
    }

    public UrlMapping getReverseMapping(final String controller, final String action, Map params) {
        if(params == null) params = Collections.EMPTY_MAP;

        
        UrlMappingKey key = new UrlMappingKey(controller, action, params.keySet());
        UrlMapping mapping = (UrlMapping)mappingsLookup.get(key);
        if(mapping == null) {
            Map lookup = new HashMap() {{
                put(UrlMapping.CONTROLLER,controller);
                put(UrlMapping.ACTION,action);
            }};

            key = new UrlMappingKey(null,null,lookup.keySet());
            mapping = (UrlMapping)mappingsLookup.get(key);

            if(mapping == null) {
                lookup.remove(UrlMapping.ACTION);
                key = new UrlMappingKey(null,null,lookup.keySet());
                return (UrlMapping)mappingsLookup.get(key);
            }
        }
        return mapping;
    }


    /**
     * A class used as a key to lookup a UrlMapping based on controller, action and parameter names
     */
    class UrlMappingKey {
        String controller;
        String action;
        Set paramNames = Collections.EMPTY_SET;


        public UrlMappingKey(String controller, String action, Set paramNames) {
            this.controller = controller;
            this.action = action;
            this.paramNames = paramNames;
        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UrlMappingKey that = (UrlMappingKey) o;

            if (action != null && !action.equals(that.action)) return false;
            if (controller != null && !controller.equals(that.controller)) return false;           
            if (!paramNames.equals(that.paramNames)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (controller != null ? controller.hashCode() : 0);
            result = 31 * result + (action != null ? action.hashCode() : 0);
            result = 31 * result + paramNames.hashCode();
            return result;
        }
    }
}
