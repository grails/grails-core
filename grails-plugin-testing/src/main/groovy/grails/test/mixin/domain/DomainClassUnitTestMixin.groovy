/*
 * Copyright 2011 SpringSource
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

package grails.test.mixin.domain

import grails.artefact.Enhanced
import grails.test.mixin.support.GrailsUnitTestMixin
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.ConstraintsEvaluatorFactoryBean
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Validator
import org.codehaus.groovy.grails.validation.ConstraintEvalUtils
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.datastore.gorm.validation.constraints.UniqueConstraint
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.junit.AfterClass
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher

/**
 * <p>A mixin that can be applied to JUnit or Spock tests to add testing support
 * to a users test classes. Can be used in combination with {@link grails.test.mixin.web.ControllerUnitTestMixin}
 * to fully test controller interaction with domains without needing a database</p>
 *
 * <p>
 *  The majority of GORM features are mocked by this unit test mixin including:
 * </p>
 *
 * <ul>
 *    <li>Dynamic finders</li>
 *    <li>Criteria queries</li>
 *    <li>Basic persistence methods</li>
 *    <li>Validation</li>
 *    <li>Data binding</li>
 * </ul>
 *
 * <p>
 *   Mocking can be applied to any domain class by simply calling the {@link DomainClassUnitTestMixin#mockDomain(Class) }
 *   method
 * </p>
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class DomainClassUnitTestMixin extends GrailsUnitTestMixin {

    static SimpleMapDatastore simpleDatastore
    static PlatformTransactionManager transactionManager

    protected Session currentSession

    @BeforeClass
    static void initializeDatastoreImplementation() {
        ClassPropertyFetcher.clearCache()
        if (applicationContext == null) {
            super.initGrailsApplication()
        }

        simpleDatastore = new SimpleMapDatastore(applicationContext)
        transactionManager = new DatastoreTransactionManager(datastore: simpleDatastore)
        applicationContext.addApplicationListener new DomainEventListener(simpleDatastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(simpleDatastore)
        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(simpleDatastore))

        defineBeans {
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                defaultConstraints = DomainClassGrailsPlugin.getDefaultConstraints(grailsApplication.config)
            }
        }
    }

    @AfterClass
    static void cleanupDatastore() {
        ClassPropertyFetcher.clearCache()
        ConstrainedProperty.removeConstraint("unique")
    }

    @Before
    void connectDatastore() {
        currentSession = DatastoreUtils.bindSession(simpleDatastore.connect())
    }

    @After
    void shutdownDatastoreImplementation() {
        currentSession?.disconnect()
        if(currentSession != null)
            DatastoreUtils.unbindSession(currentSession)
        simpleDatastore.clearData()
    }

    /**
     * Mocks a domain class providing the equivalent GORM behavior but against an in-memory concurrent hash map instead
     * of a database
     *
     * @param domainClassToMock The domain class to mock
     * @return An instance of the mocked domain class
     */
    def mockDomain(Class domainClassToMock, List domains = []) {
        ConstraintEvalUtils.clearDefaultConstraints()
        grailsApplication.getArtefactHandler(DomainClassArtefactHandler.TYPE).setGrailsApplication(grailsApplication)
        GrailsDomainClass domain = grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, domainClassToMock)
        PersistentEntity entity = simpleDatastore.mappingContext.addPersistentEntity(domainClassToMock)

        final mc = GrailsClassUtils.getExpandoMetaClass(domainClassToMock)

        ControllersGrailsPlugin.enhanceDomainWithBinding(applicationContext, domain, mc)
        DomainClassGrailsPlugin.registerConstraintsProperty(mc, domain)
//        DomainClassGrailsPlugin.addRelationshipManagementMethods(domain, applicationContext)
        def validationBeanName = "${domain.fullName}Validator"
        defineBeans {
            "${domain.fullName}"(domain.clazz) { bean ->
                bean.singleton = false
                bean.autowire = "byName"
            }
            "$validationBeanName"(GrailsDomainClassValidator) { bean ->
                delegate.messageSource = ref("messageSource")
                bean.lazyInit = true
                domainClass = domain
                delegate.grailsApplication = grailsApplication
            }
        }

        def validator = applicationContext.getBean(validationBeanName, Validator.class)
        simpleDatastore.mappingContext.addEntityValidator(entity, validator)

        def enhancer = new GormEnhancer(simpleDatastore, transactionManager)
        if (domainClassToMock.getAnnotation(Enhanced) != null) {
            enhancer.enhance(entity, true)
        }
        else {
            enhancer.enhance(entity)
        }
        if (domains) {
            for (obj in domains) {
                if (obj instanceof Map) {
                    domainClassToMock.newInstance(obj).save()
                }
                else if (entity.isInstance(obj)) {
                    obj.save()
                }
            }
        }
        else {
            return applicationContext.getBean(domain.fullName)
        }
    }
}
