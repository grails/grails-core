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

import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.binding.GrailsWebDataBinder
import org.codehaus.groovy.grails.web.binding.bindingsource.DataBindingSourceRegistry
import org.codehaus.groovy.grails.web.binding.bindingsource.DefaultDataBindingSourceRegistry
import org.codehaus.groovy.grails.web.binding.bindingsource.HalJsonDataBindingSourceCreator
import org.codehaus.groovy.grails.web.binding.bindingsource.JsonDataBindingSourceCreator
import org.codehaus.groovy.grails.web.binding.bindingsource.XmlDataBindingSourceCreator
import org.grails.databinding.converters.DateConversionHelper

/**
 * @author Jeff Brown
 * @since 2.3
 */
class DataBindingGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()

    def doWithSpring = {
        def databindingConfig

        databindingConfig = application?.config?.grails?.databinding

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

        defaultDateConverter(DateConversionHelper) {
            if(databindingConfig?.dateFormats instanceof List) {
                formatStrings = databindingConfig.dateFormats
            }
        }

        "${DataBindingSourceRegistry.BEAN_NAME}"(DefaultDataBindingSourceRegistry)

        xmlDataBindingSourceCreator(XmlDataBindingSourceCreator)
        jsonDataBindingSourceCreator(JsonDataBindingSourceCreator)
        halJsonDataBindingSourceCreator(HalJsonDataBindingSourceCreator)
    }
}
