/*
 * Copyright 2006-2007 Graeme Rocher
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

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Loads locations starting with /WEB-INF from the Grails web-app directory.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class CommandLineResourceLoader extends DefaultResourceLoader {

    @Override
    public Resource getResource(String location) {
        if (location.startsWith("/WEB-INF")) {
            return new FileSystemResource("./web-app" + location);
        }
        return super.getResource(location);
    }
}
