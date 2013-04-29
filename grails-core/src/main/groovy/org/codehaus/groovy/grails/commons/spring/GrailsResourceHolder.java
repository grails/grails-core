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
package org.codehaus.groovy.grails.commons.spring;

import java.io.IOException;

import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.springframework.core.io.Resource;

/**
 * Holds references to all the Grails resource instances to support class reloading.
 *
 * @author Graeme Rocher
 * @deprecated No longer used and will be removed from a future release
 */
@Deprecated
public class GrailsResourceHolder {

    public static final String APPLICATION_CONTEXT_ID = "grailsResourceHolder";

    private Resource[] resources = {};

    public Resource[] getResources() {
        return resources;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
    }

    /**
     * Retrieves the class name of the specified resource.
     * @param resource
     * @return the name
     */
    public String getClassName(Resource resource) {
        try {
            return GrailsResourceUtils.getClassName(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            return null;
        }
    }
}
