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
package org.grails.plugins.web.mime

import grails.config.Config
import grails.config.Settings
import grails.core.GrailsApplication
import grails.web.mime.MimeType
import grails.web.mime.MimeTypeProvider
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Creates the MimeType[] object that defines the configured mime types.
 *
 * @author Graeme Rocher
 * @since 2.0
 * @deprecated Use {@link MimeTypesConfiguration} instead
 */
@CompileStatic
@Deprecated
class MimeTypesFactoryBean implements FactoryBean<MimeType[]>, ApplicationContextAware{

    ApplicationContext applicationContext
    GrailsApplication grailsApplication

    private MimeType[] mimeTypes

    @Autowired(required = false)
    Collection<MimeTypeProvider> mimeTypeProviders = []

    @Override
    MimeType[] getObject() {
        final grailsApplication = this.grailsApplication ?: applicationContext.getBean(GrailsApplication)
        def config = grailsApplication?.config
        def mimeConfig = getMimeConfig(config)
        if (!mimeConfig) {
            mimeTypes = MimeType.createDefaults()
            return mimeTypes
        }

        def mimes = []
        for (entry in mimeConfig.entrySet()) {
            if (entry.value instanceof List) {
                for (i in entry.value) {
                    mimes << new MimeType(i.toString(),entry.key.toString())
                }
            }
            else {
                mimes << new MimeType(entry.value.toString(), entry.key.toString())
            }
        }
        for(MimeTypeProvider mtp in mimeTypeProviders) {
            for(MimeType mt in mtp.mimeTypes) {
                if (!mimes.contains(mt)) {
                    mimes << mt
                }
            }
        }
        mimeTypes = mimes as MimeType[]
        mimeTypes

    }

    @Override
    Class<?> getObjectType() { MimeType[].class }

    @Override
    boolean isSingleton() { true }


    @CompileStatic(TypeCheckingMode.SKIP)
    protected Map<CharSequence, CharSequence> getMimeConfig(Config config) {
        return config.getProperty(Settings.MIME_TYPES, Map.class)
    }
}
