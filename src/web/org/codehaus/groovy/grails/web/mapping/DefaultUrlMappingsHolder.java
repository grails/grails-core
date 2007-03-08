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

    public DefaultUrlMappingsHolder(List mappings) {
        this.urlMappings = mappings;
        initialize();
    }

    private void initialize() {
        Collections.sort(this.urlMappings);
        Collections.reverse(this.urlMappings);
        
        this.mappings = (UrlMapping[])this.urlMappings.toArray(new UrlMapping[this.urlMappings.size()]);
    }

    public UrlMapping[] getUrlMappings() {
        return this.mappings;
    }
}
