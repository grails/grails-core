/*
 * Copyright 2004-2019 the original author or authors.
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

package org.grails.plugins.web.mime;

import grails.config.Config;
import grails.config.Settings;
import grails.core.GrailsApplication;
import grails.web.mime.MimeType;
import grails.web.mime.MimeTypeProvider;
import grails.web.mime.MimeTypeResolver;
import grails.web.mime.MimeUtility;
import groovy.transform.CompileStatic;
import groovy.transform.TypeCheckingMode;
import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.context.annotation.Factory;
import org.grails.web.mime.DefaultMimeTypeResolver;
import org.grails.web.mime.DefaultMimeUtility;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Codecs
 *
 * @author graemerocher
 * @since 4.0
 */
@Configuration
public class MimeTypesConfiguration {

    private final GrailsApplication grailsApplication;
    private final ApplicationContext applicationContext;
    private final List<MimeTypeProvider> mimeTypeProviders;

    public MimeTypesConfiguration(GrailsApplication grailsApplication, List<MimeTypeProvider> mimeTypeProviders) {
        this.grailsApplication = grailsApplication;
        this.applicationContext = grailsApplication.getMainContext();
        this.mimeTypeProviders = mimeTypeProviders;
    }

    @Bean("mimeTypesHolder")
    @Primary
    MimeTypesHolder mimeTypesHolder(BeanDefinitionRegistry registry) {
        final MimeType[] mimeTypes = mimeTypes();

        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).getBeanFactory().registerSingleton(
                    MimeType.BEAN_NAME,
                    mimeTypes
            );
        }
        return new MimeTypesHolder(mimeTypes);
    }

    @Bean("mimeTypes")
    @Primary
    protected MimeType[] mimeTypes() {
        final Config config = grailsApplication.getConfig();
        final Map<CharSequence, Object> mimeConfig = getMimeConfig(config);
        MimeType[] mimeTypes;
        if (mimeConfig.isEmpty()) {
            mimeTypes = MimeType.createDefaults();
            return mimeTypes;
        } else {

            List<MimeType> mimes = new ArrayList<>();
            for (Map.Entry<CharSequence, Object> entry : mimeConfig.entrySet()) {
                final String key = entry.getKey().toString();
                final Object v = entry.getValue();
                if (v instanceof List) {
                    List list = (List) v;
                    for (Object i : list) {
                        mimes.add(new MimeType(i.toString(), key));
                    }
                }
                else {
                    mimes.add(new MimeType(v.toString(), key));
                }

            }

            final List<MimeTypeProvider> mimeTypeProviders = this.mimeTypeProviders;
            processProviders(mimes, mimeTypeProviders);
            final Map<String, MimeTypeProvider> childTypes = applicationContext.getBeansOfType(MimeTypeProvider.class);
            processProviders(mimes, childTypes.values());
            mimeTypes = mimes.toArray(new MimeType[0]);
            return mimeTypes;
        }
    }

    @Bean("grailsMimeUtility")
    @Primary
    protected MimeUtility mimeUtility(MimeTypesHolder mimeTypesHolder) {
        return new DefaultMimeUtility(mimeTypesHolder.mimeTypes);
    }

    @Bean("mimeTypeResolver")
    @Primary
    protected MimeTypeResolver mimeTypeResolver() {
        return new DefaultMimeTypeResolver();
    }

    protected Map<CharSequence, Object> getMimeConfig(Config config) {
        return config.getProperty(Settings.MIME_TYPES, Map.class);
    }

    private void processProviders(List<MimeType> mimes, Iterable<MimeTypeProvider> mimeTypeProviders) {
        for (MimeTypeProvider mimeTypeProvider : mimeTypeProviders) {
            for (MimeType mimeType : mimeTypeProvider.getMimeTypes()) {
                if (!mimes.contains(mimeType)) {
                    mimes.add(mimeType);
                }
            }
        }
    }

    static class MimeTypesHolder {
        final MimeType[] mimeTypes;

        public MimeTypesHolder(MimeType[] mimeTypes) {
            this.mimeTypes = mimeTypes;
        }
    }
}
