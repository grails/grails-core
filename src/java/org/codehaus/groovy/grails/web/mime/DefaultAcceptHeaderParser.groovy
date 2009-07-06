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
* Parsed the HTTP accept header into a a list of MimeType instances in the order of priority. Priority is dictated
* by the order of the mime entries and the associated q parameter. The higher the q parameter the higher the prioirity.
 
*
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
            LOG.debug "No mime types configured, defaulting to 'text/html'"
            return MimeType.createDefaults()
        }
        else if(!header) {
            return MimeType.getConfiguredMimeTypes()
        }
        else {
            def tokens = header.split(',')
            for(t in tokens) {
                if(t.indexOf(';') > -1) {
                    t = t.split(';')                         
                    def params = [:]
                    t[1..-1].each {
                        def i = it.indexOf('=')
                        if(i > -1) {                          
                          params[it[0..i-1].trim()] = it[i+1..-1].trim()
                        }
                    }
                    if(params)
                      createMimeTypeAndAddToList(t[0].trim(),mimeConfig, mimes, params)
                    else
                      createMimeTypeAndAddToList(t[0].trim(),mimeConfig, mimes)                    
                }
                else {
                    createMimeTypeAndAddToList(t.trim(),mimeConfig, mimes)
                }
            }

            if(!mimes) {
               LOG.debug "No configured mime types found for Accept header: $header"
               return MimeType.createDefaults()
            }
        }

        
        // remove duplicate text/xml and application/xml entries
        MimeType textXml = mimes.find { it.name == 'text/xml' }
        MimeType appXml = mimes.find { it.name ==  MimeType.XML }
        if(textXml && appXml) {
            // take the largest q value
            appXml.parameters.q = [textXml.parameters.q.toBigDecimal(), appXml.parameters.q.toBigDecimal()].max()

            mimes.remove(textXml)
        }
        else if(textXml) {
            textXml.name = MimeType.XML
        }

        if(appXml) {
            // prioritise more specific XML types like xhtml+xml if they are of equal quality
            def specificTypes = mimes.findAll { it.name ==~ /\S+?\+xml$/ }
            def appXmlIndex = mimes.indexOf(appXml)
            def appXmlQuality = appXml.parameters.q.toBigDecimal()
            for(mime in specificTypes) {
                if(mime.parameters.q.toBigDecimal() < appXmlQuality) continue
                
                def mimeIndex = mimes.indexOf(mime)
                if(mimeIndex > appXmlIndex) {
                    mimes.remove(mime)
                    mimes.add(appXmlIndex, mime)
                }
            }
        }
        return mimes.sort(new QualityComparator()) as MimeType[]
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
class QualityComparator implements Comparator {

    public int compare(Object t, Object t1) {
        def left = t.parameters.q.toBigDecimal()
        def right = t1.parameters.q.toBigDecimal()
        if(left > right) return -1
        else if(left < right ) return 1
        return 0;
    }

}