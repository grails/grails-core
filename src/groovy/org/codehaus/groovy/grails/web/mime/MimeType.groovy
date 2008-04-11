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

import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 23, 2007       
 */
public class MimeType {

    private static MIMES

    MimeType(String n, Map params = null) {
        this.name = n
        this.parameters = params
    }

    
    String name
    String extension
    Map parameters

    String toString() {
        return "MimeType { name=$name,extension=$extension,parameters=$parameters }".toString()
    }

    static MimeType[] getConfiguredMimeTypes() {
        if(MIMES) return MIMES
        else {
            def config = ConfigurationHolder.getConfig()
            def mimeConfig = config?.grails?.mime?.types
            if(!mimeConfig) return createDefaults()
            def mimes = []
            for(entry in mimeConfig) {
                if(entry.value instanceof List) {
                    for(i in entry.value) {
                        mimes << new MimeType(i)
                        mimes[-1].extension = entry.key
                    }
                }
                else {
                    mimes << new MimeType(entry.value)
                    mimes[-1].extension = entry.key
                }
            }
            MIMES = mimes as MimeType[]
            return MIMES 
        }
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