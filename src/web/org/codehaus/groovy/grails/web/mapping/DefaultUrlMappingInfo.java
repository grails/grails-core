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

import java.util.Map;
import java.util.Collections;

/**
 * A Class that implements the UrlMappingInfo interface and holds information established from a matched
 * URL
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Mar 1, 2007
 *        Time: 7:19:35 AM
 */
public class DefaultUrlMappingInfo implements UrlMappingInfo {
    private Map params = Collections.EMPTY_MAP;
    private String controllerName;
    private String actionName;
    private static final String ID_PARAM = "id";
    private String id;

    public DefaultUrlMappingInfo(String controllerName, String actionName, Map params) {
        if(controllerName == null) throw new IllegalArgumentException("Argument [controllerName] cannot be null or blank");
        if(params == null) throw new IllegalArgumentException("Argument [params] cannot be null");

        this.params = Collections.unmodifiableMap(params);
        this.controllerName = controllerName;
        this.actionName = actionName;
        this.id = (String)params.get(ID_PARAM);
    }
    public Map getParameters() {
        return params;
    }

    public String getControllerName() {
        return controllerName;
    }

    public String getActionName() {
        return actionName;
    }

    public String getId() {
        return id;
    }
}
