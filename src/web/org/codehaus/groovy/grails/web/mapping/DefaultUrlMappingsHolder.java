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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.springframework.core.style.ToStringCreator;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    private static final transient Log LOG = LogFactory.getLog(DefaultUrlMappingsHolder.class);

    private List urlMappings = new ArrayList();
    private UrlMapping[] mappings;
    private Map mappingsLookup = new HashMap();
    private Set DEFAULT_CONTROLLER_PARAMS = new HashSet() {{
           add(UrlMapping.CONTROLLER);
           add(UrlMapping.ACTION);
    }};
    private Set DEFAULT_ACTION_PARAMS = new HashSet() {{
           add(UrlMapping.ACTION);
    }};


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
            UrlMappingKey key = new UrlMappingKey(controllerName, actionName, requiredParams);
            this.mappingsLookup.put(key, mapping);
            if( LOG.isDebugEnabled() ) {
                LOG.debug( "Reverse mapping: " + key + " -> " + mapping );
            }
            Set requiredParamsAndOptionals = new HashSet(requiredParams);
            if(optionalIndex > -1) {
                for (int j = optionalIndex; j < params.length; j++) {
                    ConstrainedProperty param = params[j];
                    requiredParamsAndOptionals.add(param.getPropertyName());
                    key = new UrlMappingKey(controllerName, actionName, new HashSet(requiredParamsAndOptionals));
                    mappingsLookup.put(key, mapping);                    
                    if( LOG.isDebugEnabled() ) {
                        LOG.debug( "Reverse mapping: " + key + " -> " + mapping );
                    }
                }
            }
        }
    }

    public UrlMapping[] getUrlMappings() {
        return this.mappings;
    }

    /**
     * @see UrlMappingsHolder#getReverseMapping(String, String, java.util.Map)  
     */
    public UrlCreator getReverseMapping(final String controller, final String action, Map params) {
        if(params == null) params = Collections.EMPTY_MAP;

        UrlMapping mapping = lookupMapping(controller, action, params);
        if(mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            mapping = (UrlMapping)mappingsLookup.get(new UrlMappingKey(controller, action, Collections.EMPTY_SET));                   
        }
        if(mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            Set lookupParams = new HashSet(DEFAULT_ACTION_PARAMS);
            Set paramKeys = params.keySet();
            lookupParams.addAll(paramKeys);
            mapping = (UrlMapping)mappingsLookup.get(new UrlMappingKey(controller, null, lookupParams));
            if(mapping == null) {
                lookupParams.removeAll(paramKeys);
                mapping = (UrlMapping)mappingsLookup.get(new UrlMappingKey(controller, null, lookupParams));
            }
        }
        if(mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            Set lookupParams = new HashSet(DEFAULT_CONTROLLER_PARAMS);
            Set paramKeys = params.keySet();
            lookupParams.addAll(paramKeys);
            mapping = (UrlMapping)mappingsLookup.get(new UrlMappingKey(null, null, lookupParams));
            if(mapping == null) {
                lookupParams.removeAll(paramKeys);
                mapping = (UrlMapping)mappingsLookup.get(new UrlMappingKey(null, null, lookupParams));
            }            
        }
        if(mapping == null || (mapping instanceof ResponseCodeUrlMapping)) {
            return new DefaultUrlCreator(controller, action);
        }
        else {
            return mapping;
        }
    }

    private UrlMapping lookupMapping(String controller, String action, Map params) {
        Set paramSet = params.keySet();
        List paramList = new ArrayList(paramSet);
        UrlMapping mapping = (UrlMapping) mappingsLookup.get(new UrlMappingKey(controller, action, paramSet));
        while(!paramList.isEmpty() && mapping == null) {
            paramList.remove(paramList.size()-1);
            paramSet = new HashSet(paramList);
            mapping = (UrlMapping) mappingsLookup.get(new UrlMappingKey(controller, action, paramSet));
        }
        return mapping;
    }

    /**
     * @see org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder#match(String)
     */
    public UrlMappingInfo match(String uri) {
        UrlMappingInfo info = null;
        for (int i = 0; i < mappings.length; i++) {

            UrlMapping mapping = mappings[i];
            if(LOG.isDebugEnabled())
                LOG.debug("Attempting to match URI ["+uri+"] with pattern ["+mapping.getUrlData().getUrlPattern()+"]");

            info = mapping.match(uri);

            if(info!=null) {
                break;
            }
        }

        return info;
    }

    public UrlMappingInfo[] matchAll(String uri) {
        List matchingUrls = new ArrayList();
        for (int i = 0; i < mappings.length; i++) {
            UrlMapping mapping = mappings[i];
            if(LOG.isDebugEnabled())
                LOG.debug("Attempting to match URI ["+uri+"] with pattern ["+mapping.getUrlData().getUrlPattern()+"]");

            UrlMappingInfo current = mapping.match(uri);
            if(current!=null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Matched URI ["+uri+"] with pattern ["+mapping.getUrlData().getUrlPattern()+"], adding to posibilities");

                matchingUrls.add(current);
            }
        }
        return (UrlMappingInfo[])matchingUrls.toArray(new UrlMappingInfo[matchingUrls.size()]);
    }

    public UrlMappingInfo[] matchAll(String uri, String httpMethod) {
        return new UrlMappingInfo[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UrlMappingInfo matchStatusCode(int responseCode) {
        for (int i = 0; i < mappings.length; i++) {
            UrlMapping mapping = mappings[i];
            if (mapping instanceof ResponseCodeUrlMapping) {
                ResponseCodeUrlMapping responseCodeUrlMapping = (ResponseCodeUrlMapping) mapping;
                final UrlMappingInfo current = responseCodeUrlMapping.match(responseCode);
                if (current != null) return current;
            }
        }

        return null;
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("URL Mappings");
        pw.println("------------");
        for (int i = 0; i < mappings.length; i++) {
            UrlMapping mapping = mappings[i];
            pw.println(mapping);
        }
        pw.flush();
        return sw.toString();
    }

    /**
     * A class used as a key to lookup a UrlMapping based on controller, action and parameter names
     */
    class UrlMappingKey {
        String controller;
        String action;
        Collection paramNames = Collections.EMPTY_SET;


        public UrlMappingKey(String controller, String action, Collection paramNames) {
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

        public String toString() {
            return new ToStringCreator(this).append( "controller", controller ).append("action",action ).append( "params", paramNames ).toString();
        }
    }
}
