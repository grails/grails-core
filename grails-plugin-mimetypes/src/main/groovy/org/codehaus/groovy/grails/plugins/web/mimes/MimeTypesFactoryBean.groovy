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
package org.codehaus.groovy.grails.plugins.web.mimes

import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.beans.factory.FactoryBean
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.InitializingBean

/**
 *
 * Creates the MimeType[] object that defines the configured mime types
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class MimeTypesFactoryBean implements FactoryBean<MimeType[]>, GrailsApplicationAware, InitializingBean{

    GrailsApplication grailsApplication

    private MimeType[] mimeTypes

    MimeType[] getObject() {
        return mimeTypes
    }

    Class<?> getObjectType() {
        return MimeType[].class
    }

    boolean isSingleton() { true }

    void afterPropertiesSet() {
        def config = grailsApplication?.config
        def mimeConfig = config?.grails?.mime?.types
        if (!mimeConfig) mimeTypes = MimeType.createDefaults()

        def mimes = []
        for (entry in mimeConfig) {
            if (entry.value instanceof List) {
                for (i in entry.value) {
                    mimes << new MimeType(i)
                    mimes[-1].extension = entry.key
                }
            }
            else {
                mimes << new MimeType(entry.value)
                mimes[-1].extension = entry.key
            }
        }
        mimeTypes = mimes as MimeType[]
    }
}
