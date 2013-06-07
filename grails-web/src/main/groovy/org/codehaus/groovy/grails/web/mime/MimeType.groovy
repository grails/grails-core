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

import grails.util.Holders
import groovy.transform.CompileStatic;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.ContextLoader

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class MimeType {

    /**
     * The bean name used to store the mime type definitions
     */
    public static final String BEAN_NAME = "mimeTypes"

    static final MimeType ALL = new MimeType("*/*", "all")
    static final MimeType HTML = new MimeType('text/html', "html")
    static final MimeType XHTML = new MimeType("application/xhtml+xml", "html")
    static final MimeType XML = new MimeType('application/xml', "xml")
    static final MimeType JSON = new MimeType('application/json', "json")
    static final MimeType TEXT_XML = new MimeType('text/xml', "xml")
    static final MimeType TEXT_JSON = new MimeType('text/json', "json")
    static final MimeType HAL_JSON = new MimeType('application/hal+json', "json")
    static final MimeType HAL_XML = new MimeType('application/hal+xml', "xml")

    private static DEFAULTS = createDefaults()
    public static final String QUALITY_RATING = "1.0"

    MimeType(String name, Map params = [:]) {
        this(name, null, params)
    }

    MimeType(String name, String extension, Map<String, String> params = [:]) {
        this.name = name
        this.extension = extension
        this.name = name
        parameters.putAll(params)
    }

    String name
    String extension
    Map<String, String> parameters = [q: QUALITY_RATING]

    /**
     * @return The quality of the Mime type
     */
    String getQuality() {
        return parameters.q ?: QUALITY_RATING
    }

    /**
     * @return The version of the Mime type
     */
    String getVersion() {
        return parameters.v ?: null
    }

    boolean equals(Object o) { o instanceof MimeType && name.equals(o.name) }
    int hashCode() { name.hashCode() }

    String toString() {
        "MimeType { name=$name,extension=$extension,parameters=$parameters }"
    }

    /**
     * @return An array of MimeTypes
     */
    static MimeType[] getConfiguredMimeTypes() {
        def ctx = Holders.findApplicationContext()
        if(ctx == null) {
            ctx = GrailsWebRequest.lookup()?.getApplicationContext()
        }
        (MimeType[])ctx?.containsBean(MimeType.BEAN_NAME) ? ctx?.getBean(MimeType.BEAN_NAME, MimeType[]) : DEFAULTS
    }

    /**
     * Creates the default MimeType configuration if none exists in Config.groovy
     */
    static MimeType[] createDefaults() {
        def mimes = [new MimeType('text/html')]
        mimes[-1].extension = 'html'
        mimes as MimeType[]
    }
}
