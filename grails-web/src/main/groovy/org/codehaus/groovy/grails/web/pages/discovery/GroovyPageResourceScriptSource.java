/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.web.pages.discovery;

import org.springframework.core.io.Resource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 6/30/11
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class GroovyPageResourceScriptSource extends ResourceScriptSource implements GroovyPageScriptSource{
    private String uri;

    /**
     * Create a new ResourceScriptSource for the given resource.
     *
     * @param resource the Resource to load the script from
     */
    public GroovyPageResourceScriptSource(String uri, Resource resource) {
        super(resource);
        this.uri = uri;
    }

    public String getURI() {
        return uri;
    }
}
