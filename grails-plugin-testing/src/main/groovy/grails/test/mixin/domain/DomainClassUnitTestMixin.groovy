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
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin
import org.codehaus.groovy.grails.validation.ConstraintEvalUtils
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Validator

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
    private static final Set<String> REQUIRED_FEATURES = (["domainClass"] as Set<String>).asImmutable()
    
    public DomainClassUnitTestMixin(Set<String> features) {
        super((REQUIRED_FEATURES + features) as Set<String>)
    }
    
    public DomainClassUnitTestMixin() {
        super(REQUIRED_FEATURES)
    }

    SimpleMapDatastore getSimpleDatastore() {
        grailsApplication.mainContext.getBean(SimpleMapDatastore)
    }
    
    PlatformTransactionManager getTransactionManager() {
        grailsApplication.mainContext.getBean('transactionManager')
    }
    
    @CompileStatic
    def mockDomains(Class<?>... domainClassesToMock) {
        initialMockDomainSetup()
        Collection<PersistentEntity> entities = simpleDatastore.mappingContext.addPersistentEntities(domainClassesToMock)
        for (PersistentEntity entity in entities) {
            GrailsDomainClass domain = registerGrailsDomainClass(entity.javaClass)

            Validator validator = registerDomainClassValidator(domain)
            simpleDatastore.mappingContext.addEntityValidator(entity, validator)
        }
        def enhancer = new GormEnhancer(simpleDatastore, transactionManager)
        final failOnError = getFailOnError()
        enhancer.failOnError = failOnError instanceof Boolean ? failOnError : false

        initializeMappingContext()

        enhancer.enhance()
    }

    protected void initializeMappingContext() {
        simpleDatastore.mappingContext.initialize()
    }

    protected getFailOnError() {
        config?.grails?.gorm?.failOnError
    }

    /**
     * Mocks a domain class providing the equivalent GORM behavior but against an in-memory concurrent hash map instead
     * of a database
     *
     * @param domainClassToMock The domain class to mock
     * @return An instance of the mocked domain class
     */
    @CompileStatic
    def mockDomain(Class<?> domainClassToMock, List domains = []) {
        mockDomains(domainClassToMock)
        final entity = simpleDatastore.mappingContext.getPersistentEntity(domainClassToMock.name)
        if (domains) {
            saveDomainList(entity, domains)
        }
    }

    protected void initialMockDomainSetup() {
        ConstraintEvalUtils.clearDefaultConstraints()
        grailsApplication.getArtefactHandler(DomainClassArtefactHandler.TYPE).setGrailsApplication(grailsApplication)
    }

    protected void saveDomainList(PersistentEntity entity, List domains) {
        for (obj in domains) {
            if (obj instanceof Map) {
                entity.javaClass.newInstance(obj).save()
            } else if (entity.isInstance(obj)) {
                obj.save()
            }
        }
    }

    protected void enhanceSingleEntity(PersistentEntity entity) {
        def enhancer = new GormEnhancer(simpleDatastore, transactionManager)
        final failOnError = config?.grails?.gorm?.failOnError
        enhancer.failOnError = failOnError instanceof Boolean ? failOnError : false
        if (entity.javaClass.getAnnotation(Enhanced) != null) {
            enhancer.enhance(entity, true)
        } else {
            enhancer.enhance(entity)
        }
    }

    protected Validator registerDomainClassValidator(GrailsDomainClass domain) {
        String validationBeanName = "${domain.fullName}Validator"
        defineBeans(true) {
            "${domain.fullName}"(domain.clazz) { bean ->
                bean.singleton = false
                bean.autowire = "byName"
            }
            "$validationBeanName"(MockCascadingDomainClassValidator) { bean ->
                delegate.messageSource = ref("messageSource")
                bean.lazyInit = true
                domainClass = domain
                delegate.grailsApplication = grailsApplication
            }
        }

        applicationContext.getBean(validationBeanName, Validator)
    }

    @CompileStatic
    protected GrailsDomainClass registerGrailsDomainClass(Class<?> domainClassToMock) {
        GrailsDomainClass domain = (GrailsDomainClass)grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, domainClassToMock)

        final mc = GrailsClassUtils.getExpandoMetaClass(domainClassToMock)

        ControllersGrailsPlugin.enhanceDomainWithBinding(domain, mc)
        DomainClassGrailsPlugin.registerConstraintsProperty(mc, domain)
        return domain
    }
}
