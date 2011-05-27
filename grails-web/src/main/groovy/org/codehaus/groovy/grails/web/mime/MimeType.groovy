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
package org.codehaus.groovy.grails.web.mime

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

 /**
 * @author Graeme Rocher
 * @since 1.0
 */
class MimeType {

    /**
     * The bean name used to store the mime type definitions
     */
    public static final String BEAN_NAME = "mimeTypes"

    static final XML = 'application/xml'

    private static MIMES
    private static DEFAULTS = createDefaults()

    MimeType(String n, Map params = [:]) {
        name = n
        parameters.putAll(params)
    }

    String name
    String extension
    Map parameters = [q:"1.0"]

    boolean equals(Object o) { o instanceof MimeType && name.equals(o.name) }
    int hashCode() { name.hashCode() }

    String toString() {
        "MimeType { name=$name,extension=$extension,parameters=$parameters }"
    }

    /**
     * @return An array of MimeTypes
     */
    static MimeType[] getConfiguredMimeTypes() {
        def webRequest = GrailsWebRequest.lookup()
        def context = webRequest?.getApplicationContext()
        if (context?.containsBean(MimeType.BEAN_NAME)) {
            return context?.getBean(MimeType.BEAN_NAME, MimeType[].class)
        }
        return DEFAULTS
    }

    static reset() {
        MIMES = null
    }

    /**
     * Creates the default MimeType configuration if none exists in Config.groovy
     */
    static MimeType[] createDefaults() {
        def mimes = [ new MimeType('text/html') ]
        mimes[-1].extension = 'html'
        return mimes as MimeType[]
    }
}
