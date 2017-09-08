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
package org.grails.plugins.domain

import grails.core.GrailsApplication
import grails.plugins.Plugin
import grails.util.GrailsUtil
import grails.validation.ConstraintsEvaluator
import org.grails.plugins.domain.support.DefaultConstraintEvaluatorFactoryBean
import org.grails.plugins.domain.support.DefaultMappingContextFactoryBean
import org.grails.plugins.domain.support.ValidatorRegistryFactoryBean
import org.grails.validation.DefaultConstraintEvaluator

/**
 * Configures the domain classes in the spring context.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class DomainClassGrailsPlugin extends Plugin {

    def watchedResources = ["file:./grails-app/domain/**/*.groovy",
                            "file:./plugins/*/grails-app/domain/**/*.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [i18n:version]
    def loadAfter = ['controllers', 'dataSource']

    Closure doWithSpring() {{->
        GrailsApplication application = grailsApplication
        "${ConstraintsEvaluator.BEAN_NAME}"(DefaultConstraintEvaluator)
        validateableConstraintsEvaluator(DefaultConstraintEvaluatorFactoryBean)
        grailsDomainClassMappingContext(DefaultMappingContextFactoryBean, application, applicationContext)
        gormValidatorRegistry(ValidatorRegistryFactoryBean)
    }}
}
