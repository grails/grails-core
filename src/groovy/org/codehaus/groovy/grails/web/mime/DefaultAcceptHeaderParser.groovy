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
import org.apache.commons.logging.*

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Nov 23, 2007
*/
class DefaultAcceptHeaderParser implements AcceptHeaderParser{

    static final LOG = LogFactory.getLog(DefaultAcceptHeaderParser)
    public MimeType[] parse(String header) {
        def config = ConfigurationHolder.getConfig()
        def mimes = []
        def qualifiedMimes = []
        def mimeConfig = config?.grails?.mime?.types
        if(!mimeConfig) {
            LOG.warn "No mime types configured, defaulting to 'text/html'"
            return createDefaultMimeTypeConfig()
        }
        else if(!header) {
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
        }
        else {
            def tokens = header.split(',')
            for(t in tokens) {
                if(t.indexOf(';') > -1) {
                    t = t.split(';')                         
                    def params = [:]
                    t[1..-1].each {
                        def i = it.indexOf('=')
                        params[it[0..i-1].trim()] = it[i+1..-1].trim()
                    }
                    def mimeList = params.q ? qualifiedMimes : mimes
                    createMimeTypeAndAddToList(t[0].trim(),mimeConfig, mimeList, params)
                }
                else {
                    createMimeTypeAndAddToList(t.trim(),mimeConfig, mimes)
                }
            }

            if(!mimes) {
               LOG.warn "No configured mime types found for Accept header: $header"
               return createDefaultMimeTypeConfig()
            }
        }
        return (qualifiedMimes.sort { it.parameters.q.toBigDecimal() }.reverse() + mimes) as MimeType[]
    }

    private createDefaultMimeTypeConfig() {
        def mimes = [ new MimeType('text/html') ]
        mimes[-1].extension = 'html'
        return mimes as MimeType[]
    }

    private createMimeTypeAndAddToList(name, mimeConfig, mimes, params = null) {
        def mime = params ? new MimeType(name, params ) : new MimeType(name)
        def ext = mimeConfig.find { it.value == name }
        if(!ext) {

            def multiMimeFormats = mimeConfig.findAll {it.value instanceof List}
            ext = multiMimeFormats?.find { it.value?.find { it == name } }
        }
        if(ext) {
            mime.extension = ext.key
            mimes << mime
        }
    }


}