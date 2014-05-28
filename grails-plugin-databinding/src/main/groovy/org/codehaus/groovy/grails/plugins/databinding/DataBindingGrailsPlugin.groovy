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
package org.codehaus.groovy.grails.plugins.databinding

import grails.util.GrailsUtil
import grails.web.databinding.GrailsWebDataBinder;

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.binding.BindEventListenerAdapter
import org.codehaus.groovy.grails.web.binding.bindingsource.DataBindingSourceRegistry
import org.codehaus.groovy.grails.web.binding.bindingsource.DefaultDataBindingSourceRegistry
import org.codehaus.groovy.grails.web.binding.bindingsource.HalJsonDataBindingSourceCreator
import org.codehaus.groovy.grails.web.binding.bindingsource.HalXmlDataBindingSourceCreator
import org.codehaus.groovy.grails.web.binding.bindingsource.JsonDataBindingSourceCreator
import org.codehaus.groovy.grails.web.binding.bindingsource.XmlDataBindingSourceCreator
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.grails.databinding.converters.DateConversionHelper
import org.grails.databinding.converters.TimeZoneConverter
import org.grails.databinding.converters.web.LocaleAwareBigDecimalConverter
import org.grails.databinding.converters.web.LocaleAwareNumberConverter

/**
 * @author Jeff Brown
 * @since 2.3
 */
class DataBindingGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()

    def doWithSpring = {
        def databindingConfig

        databindingConfig = application?.config?.grails?.databinding

        if(Boolean.TRUE.equals(databindingConfig?.useSpringBinder)) {
            def msg = 'The grails.databinding.useSpringBinder config property is set to true.  The Spring data binder has been deprecated and will be removed in a future release of Grails.'
            def log = LogFactory.getLog(DataBindingGrailsPlugin)
            log.warn msg
        }

        def autoGrowCollectionLimitSetting = databindingConfig?.autoGrowCollectionLimit

        "${DataBindingUtils.DATA_BINDER_BEAN_NAME}"(GrailsWebDataBinder, ref('grailsApplication')) {

            // trimStrings defaults to TRUE
            trimStrings = !Boolean.FALSE.equals(databindingConfig?.trimStrings)

            // convertEmptyStringsToNull defaults to TRUE
            convertEmptyStringsToNull = !Boolean.FALSE.equals(databindingConfig?.convertEmptyStringsToNull)

            // autoGrowCollectionLimit defaults to 256
            if(autoGrowCollectionLimitSetting instanceof Integer) {
                autoGrowCollectionLimit = autoGrowCollectionLimitSetting
            }
        }

        timeZoneConverter(TimeZoneConverter)
        
        defaultDateConverter(DateConversionHelper) {
            if(databindingConfig?.dateFormats instanceof List) {
                formatStrings = databindingConfig.dateFormats
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
        
        if(Boolean.TRUE.equals(databindingConfig?.enableSpringEventAdapter)) {
            grailsBindEventListenerAdapter(BindEventListenerAdapter)
        }
    }
}
