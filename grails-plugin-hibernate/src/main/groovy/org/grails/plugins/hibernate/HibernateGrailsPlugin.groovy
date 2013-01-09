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
package org.grails.plugins.hibernate

import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport

/**
 * Binary plugin descriptor for Hibernate
 *
 * @since 2.3
 * @author Graeme Rocher
 */
class HibernateGrailsPlugin {
    def author = "Graeme Rocher"
    def title = "Hibernate for Grails"
    def description = "Provides integration between Grails and Hibernate through GORM"

    def version = GrailsUtil.grailsVersion
    def documentation = "http://grails.org/doc/$version"
    def observe = ['domainClass']

    def loadAfter = ['controllers', 'domainClass']

    def watchedResources = ["file:./grails-app/conf/hibernate/**.xml"]

    def artefacts = [new AnnotationDomainClassArtefactHandler()]

    def doWithSpring = HibernatePluginSupport.doWithSpring

    def doWithDynamicMethods = HibernatePluginSupport.doWithDynamicMethods

    def onChange = HibernatePluginSupport.onChange
}
