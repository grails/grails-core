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
package org.codehaus.groovy.grails.support;

import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * A ResourceLoader that loads Resources from Strings that are registered as Mock resources
 *
 * @author Graeme Rocher
 * @since 0.4
 * 
 *        <p/>
 *        Created: Feb 23, 2007
 *        Time: 2:00:15 PM
 */
public class MockStringResourceLoader extends MockResourceLoader {

    private Map mockResources = new HashMap();

    public Resource getResource(String location) {
        if(mockResources.containsKey(location))
            return (Resource)mockResources.get(location);
        else
            return super.getResource(location);
    }


    /**
     * Registers a mock resource with the first argument as the location and the second as the contents
     * of the resource
     *
     * @param location The location
     * @param contents The contents of the resource
     */
    public void registerMockResource(String location, String contents) {
        this.mockResources.put(location, new GrailsByteArrayResource(contents.getBytes(), location));
    }

    /**
     * Registers a mock resource with the first argument as the location and the second as the contents
     * of the resource
     *
     * @param location The location
     * @param contents The contents of the resource
     */
    public void registerMockResource(String location, byte[] contents) {
        this.mockResources.put(location, new GrailsByteArrayResource(contents, location));
    }
}
