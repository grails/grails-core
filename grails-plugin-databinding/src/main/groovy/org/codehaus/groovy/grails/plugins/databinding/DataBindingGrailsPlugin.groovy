/* Copyright 2013 the original author or authors.
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
import org.codehaus.groovy.grails.web.binding.GormAwareDataBinder

class DataBindingGrailsPlugin {
    
    def version = GrailsUtil.getGrailsVersion()

    def doWithSpring = {
        def autoGrowCollectionLimitSetting = application?.config?.grails?.databinding?.autoGrowCollectionLimit
        "${DataBindingUtils.DATA_BINDER_BEAN_NAME}"(GormAwareDataBinder, ref('grailsApplication')) {
            trimStrings = !Boolean.FALSE.equals(application?.config?.grails?.databinding?.trimStrings)
            if(autoGrowCollectionLimitSetting instanceof Integer) {
                autoGrowCollectionLimit = autoGrowCollectionLimitSetting
            }
        }
    }
}
