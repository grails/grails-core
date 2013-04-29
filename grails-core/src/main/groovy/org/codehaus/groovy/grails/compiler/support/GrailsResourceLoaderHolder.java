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
package org.codehaus.groovy.grails.compiler.support;

import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;

/**
 * A holder for the GrailsResourceLoader object.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsResourceLoaderHolder {

    private static GrailsResourceLoader resourceLoader;

    public static synchronized GrailsResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public static synchronized void setResourceLoader(GrailsResourceLoader resourceLoader) {
        GrailsResourceLoaderHolder.resourceLoader = resourceLoader;
    }
}
