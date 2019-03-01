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

package org.grails.databinding.converters;

import grails.core.GrailsApplication;
import grails.databinding.converters.ValueConverter;
import io.micronaut.context.exceptions.NoSuchBeanException;
import org.grails.databinding.converters.web.LocaleAwareBigDecimalConverter;
import org.grails.databinding.converters.web.LocaleAwareNumberConverter;
import org.grails.plugins.databinding.DataBindingConfigurationProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Default converters configuration.
 */
@Configuration
public class DefaultConvertersConfiguration {

    private final DataBindingConfigurationProperties configurationProperties;
    private final LocaleResolver localResolver;

    public DefaultConvertersConfiguration(GrailsApplication grailsApplication, DataBindingConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
        LocaleResolver localResolver;
        try {
            localResolver = grailsApplication.getMainContext().getBean(LocaleResolver.class);
        } catch (NoSuchBeanException e) {
             localResolver = null;
        }
        this.localResolver = localResolver;
    }

    /**
     * @return The default currency converter
     */
    @Bean("defaultCurrencyConverter")
    protected CurrencyValueConverter defaultCurrencyConverter() {
        return new CurrencyValueConverter();
    }

    @Bean("defaultGrailsBigDecimalConverter")
    protected ValueConverter defaultGrailsBigDecimalConverter() {
        LocaleAwareBigDecimalConverter converter = new LocaleAwareBigDecimalConverter();
        converter.setTargetType(BigDecimal.class);
        converter.setLocaleResolver(localResolver);
        return converter;
    }

    @Bean("defaultGrailsBigIntegerConverter")
    protected ValueConverter defaultGrailsBigIntegerConverter() {
        LocaleAwareBigDecimalConverter converter = new LocaleAwareBigDecimalConverter();
        converter.setTargetType(BigInteger.class);
        converter.setLocaleResolver(localResolver);
        return converter;
    }

    @Bean("defaultDateConverter")
    protected DateConversionHelper defaultDateConverter() {
        DateConversionHelper converter = new DateConversionHelper();
        converter.setDateParsingLenient(configurationProperties.isDateParsingLenient());
        converter.setFormatStrings(configurationProperties.getDateFormats());
        return converter;
    }

    @Bean("timeZoneConverter")
    protected TimeZoneConverter defaultTimeZoneConverter() {
        return new TimeZoneConverter();
    }

    @Bean("defaultShortConverter")
    protected LocaleAwareNumberConverter shortConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(Short.class);
        return converter;
    }

    @Bean("defaultshortConverter")
    protected LocaleAwareNumberConverter primitiveShortConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(short.class);
        return converter;
    }

    @Bean("defaultIntegerConverter")
    protected LocaleAwareNumberConverter integerConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(Integer.class);
        return converter;
    }

    @Bean("defaultintConverter")
    protected LocaleAwareNumberConverter primitiveIntConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(int.class);
        return converter;
    }

    @Bean("defaultFloatConverter")
    protected LocaleAwareNumberConverter floatConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(Float.class);
        return converter;
    }

    @Bean("defaultfloatConverter")
    protected LocaleAwareNumberConverter primitiveFloattConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(float.class);
        return converter;
    }

    @Bean("defaultLongConverter")
    protected LocaleAwareNumberConverter longConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(Long.class);
        return converter;
    }

    @Bean("defaultlongConverter")
    protected LocaleAwareNumberConverter primitiveLongConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(long.class);
        return converter;
    }

    @Bean("defaultDoubleConverter")
    protected LocaleAwareNumberConverter doubleConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(Double.class);
        return converter;
    }

    @Bean("defaultdoubleConverter")
    protected LocaleAwareNumberConverter primitiveDoubleConverter() {
        final LocaleAwareNumberConverter converter = new LocaleAwareNumberConverter();
        converter.setLocaleResolver(localResolver);
        converter.setTargetType(double.class);
        return converter;
    }
}
