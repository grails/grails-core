/*
 * Copyright 2013 the original author or authors.
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
package org.grails.plugins.databinding

import grails.plugins.Plugin
import grails.util.GrailsUtil
import grails.web.databinding.DataBindingUtils
import grails.web.databinding.GrailsWebDataBinder
import org.grails.web.databinding.bindingsource.DataBindingSourceRegistry
import org.grails.web.databinding.bindingsource.DefaultDataBindingSourceRegistry
import org.grails.web.databinding.bindingsource.HalJsonDataBindingSourceCreator
import org.grails.web.databinding.bindingsource.HalXmlDataBindingSourceCreator
import org.grails.web.databinding.bindingsource.JsonDataBindingSourceCreator
import org.grails.web.databinding.bindingsource.XmlDataBindingSourceCreator
import org.grails.databinding.converters.CurrencyValueConverter
import org.grails.databinding.converters.DateConversionHelper
import org.grails.databinding.converters.TimeZoneConverter
import org.grails.databinding.converters.web.LocaleAwareBigDecimalConverter
import org.grails.databinding.converters.web.LocaleAwareNumberConverter

/**
 * Plugin for configuring the data binding features of Grails
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @since 2.3
 */
class DataBindingGrailsPlugin extends Plugin {

    public static final String TRIM_STRINGS = 'grails.databinding.trimStrings'
    public static final String CONVERT_EMPTY_STRINGS_TO_NULL = 'grails.databinding.convertEmptyStringsToNull'
    public static final String AUTO_GROW_COLLECTION_LIMIT = 'grails.databinding.autoGrowCollectionLimit'
    public static final String DATE_FORMATS = 'grails.databinding.dateFormats'

    def version = GrailsUtil.getGrailsVersion()

    @Override
    Closure doWithSpring() {{->
        def application = grailsApplication
        def config = application.config
        boolean trimStringsSetting = config.getProperty(TRIM_STRINGS, Boolean, true)
        boolean convertEmptyStringsToNullSetting = config.getProperty(CONVERT_EMPTY_STRINGS_TO_NULL, Boolean, true)
        Integer autoGrowCollectionLimitSetting = config.getProperty(AUTO_GROW_COLLECTION_LIMIT, Integer, 256)
        List dateFormats = config.getProperty(DATE_FORMATS, List, [])


        "${DataBindingUtils.DATA_BINDER_BEAN_NAME}"(GrailsWebDataBinder, grailsApplication) {
            // trimStrings defaults to TRUE
            trimStrings = trimStringsSetting
            // convertEmptyStringsToNull defaults to TRUE
            convertEmptyStringsToNull = convertEmptyStringsToNullSetting
            // autoGrowCollectionLimit defaults to 256
            autoGrowCollectionLimit = autoGrowCollectionLimitSetting
        }

        timeZoneConverter(TimeZoneConverter)

        defaultDateConverter(DateConversionHelper) {
            if(dateFormats) {
                formatStrings = dateFormats
            }
        }
        [Short,   Short.TYPE,
         Integer, Integer.TYPE,
         Float,   Float.TYPE,
         Long,    Long.TYPE,
         Double,  Double.TYPE].each { numberType ->
            "defaultGrails${numberType.name}Converter"(LocaleAwareNumberConverter) {
                targetType = numberType
            }
        }
        defaultGrailsBigDecimalConverter(LocaleAwareBigDecimalConverter) {
            targetType = BigDecimal
        }
        defaultGrailsBigIntegerConverter(LocaleAwareBigDecimalConverter) {
            targetType = BigInteger
        }

        "${DataBindingSourceRegistry.BEAN_NAME}"(DefaultDataBindingSourceRegistry)

        xmlDataBindingSourceCreator(XmlDataBindingSourceCreator)
        jsonDataBindingSourceCreator(JsonDataBindingSourceCreator)
        halJsonDataBindingSourceCreator(HalJsonDataBindingSourceCreator)
        halXmlDataBindingSourceCreator(HalXmlDataBindingSourceCreator)

        defaultCurrencyConverter CurrencyValueConverter
    }}
}
