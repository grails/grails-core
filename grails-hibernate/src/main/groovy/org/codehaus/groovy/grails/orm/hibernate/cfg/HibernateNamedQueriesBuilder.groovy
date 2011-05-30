/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg

import grails.util.GrailsNameUtils

import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.orm.hibernate.metaclass.*
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport
import org.grails.datastore.gorm.finders.FinderMethod
import org.hibernate.criterion.CriteriaSpecification

/**
 * A builder that implements the ORM named queries DSL.
 *
 * @author Jeff Brown
 */
class HibernateNamedQueriesBuilder {

    private final domainClass
    private final dynamicMethods
    private boolean initialized = false

    /**
     * @param domainClass the GrailsDomainClass defining the named queries
     * @param finders dynamic finders
     */
    HibernateNamedQueriesBuilder(domainClass, List<FinderMethod> finders) {
        this.domainClass = domainClass
        dynamicMethods = finders
    }

    def evaluate(Closure namedQueriesClosure) {
        def closure = namedQueriesClosure.clone()
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = this
        closure.call()
        initialized = true
    }

    private handleMethodMissing = {String name, args ->
        def classesToAugment = [domainClass]

        def subClasses = domainClass.subClasses
        if (subClasses) {
            classesToAugment += subClasses
        }

        def getterName = GrailsNameUtils.getGetterName(name)
        classesToAugment.each { clz ->
            clz.metaClass.static."${getterName}" = {->
                // creating a new proxy each time because the proxy class has
                // some state that cannot be shared across requests (namedCriteriaParams)
                new NamedCriteriaProxy(criteriaClosure: args[0], domainClass: clz, dynamicMethods: dynamicMethods)
            }
        }
    }

    def methodMissing(String name, args) {
        if (args && args[0] instanceof Closure && !initialized) {
            return handleMethodMissing(name, args)
        }
        throw new MissingMethodException(name, HibernateNamedQueriesBuilder, args)
    }
}

class NamedCriteriaProxy {

    private criteriaClosure
    private GrailsDomainClass domainClass
    private dynamicMethods
    private namedCriteriaParams
    private previousInChain
    private queryBuilder

    private invokeCriteriaClosure(additionalCriteriaClosure = null) {
        def crit = getPreparedCriteriaClosure(additionalCriteriaClosure)
        crit()
    }

    void propertyMissing(String propName, val) {
        queryBuilder?."${propName}" = val
    }

    private listInternal(Object[] params, Closure additionalCriteriaClosure, Boolean isDistinct) {
        def listClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure(additionalCriteriaClosure)
            def paramsMap
            if (params && params[-1] instanceof Map) {
                paramsMap = params[-1]
            }
            if (paramsMap) {
                domainClass.grailsApplication
                GrailsHibernateUtil.populateArgumentsForCriteria domainClass.grailsApplication, domainClass.clazz, queryBuilder.instance, paramsMap
            }
            if (isDistinct) {
                resultTransformer = CriteriaSpecification.DISTINCT_ROOT_ENTITY
            }
        }
        domainClass.clazz.withCriteria(listClosure)
    }

    def list(Object[] params, Closure additionalCriteriaClosure = null) {
        listInternal params, additionalCriteriaClosure, false
    }

    def listDistinct(Object[] params, Closure additionalCriteriaClosure = null) {
        listInternal params, additionalCriteriaClosure, true
    }

    def call(Object[] params) {
        if (params && params[-1] instanceof Closure) {
            def additionalCriteriaClosure = params[-1]
            params = params.length > 1 ? params[0..-2] : [:]
            if (params) {
                if (params[-1] instanceof Map) {
                    if (params.length > 1) {
                        namedCriteriaParams = params[0..-2] as Object[]
                    }
                } else {
                    namedCriteriaParams = params
                }
            }
            list(params, additionalCriteriaClosure)
        }
        else {
            namedCriteriaParams = params
            this
        }
    }

    def get(id) {
        id = HibernatePluginSupport.convertValueToIdentifierType(domainClass, id)
        def getClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure()
            eq 'id', id
            uniqueResult = true
        }
        domainClass.clazz.withCriteria(getClosure)
    }

    def count(Closure additionalCriteriaClosure = null) {
        def countClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure(additionalCriteriaClosure)
            uniqueResult = true
            projections {
                rowCount()
            }
        }
        domainClass.clazz.withCriteria(countClosure)
    }

    def findWhere(params) {
        findAllWhere(params, true)
    }

    def findAllWhere(Map params, Boolean uniq = false) {
        def queryClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure()
            params.each {key, val ->
                eq key, val
            }
            if (uniq) {
                maxResults 1
                uniqueResult = true
            }
        }
        domainClass.clazz.withCriteria(queryClosure)
    }

    def propertyMissing(String propertyName) {
        if (domainClass.metaClass.getMetaProperty(propertyName)) {
            def nextInChain = domainClass.metaClass.getMetaProperty(propertyName).getProperty(domainClass)
            nextInChain.previousInChain = this
            return nextInChain
        }
        throw new MissingPropertyException(propertyName, NamedCriteriaProxy)
    }

    def methodMissing(String methodName, args) {

        def method = dynamicMethods.find {it.isMethodMatch(methodName)}

        if (method) {
            def preparedClosure = getPreparedCriteriaClosure()
            def c = {
                queryBuilder = delegate
                preparedClosure()
            }
            return method.invoke(domainClass.clazz, methodName, c, args)
        }

        if (!queryBuilder && domainClass.metaClass.getMetaProperty(methodName)) {
            def nextInChain = domainClass.metaClass.getMetaProperty(methodName).getProperty(domainClass)
            nextInChain.previousInChain = this
            return nextInChain(args)
        }

        def proxy = getNamedCriteriaProxy(domainClass, methodName)
        if (proxy) {
            def nestedCriteria = proxy.criteriaClosure.clone()
            nestedCriteria.delegate = this
            return nestedCriteria(*args)
        }
        try {
            def returnValue = queryBuilder."${methodName}"(*args)
            return returnValue
        } catch (MissingMethodException mme) {
            def targetType = queryBuilder?.targetClass
            proxy = getNamedCriteriaProxy(targetType, methodName)
            if (proxy) {
                def nestedCriteria = proxy.criteriaClosure.clone()
                nestedCriteria.delegate = this
                return nestedCriteria(*args)
            }
            throw mme
        }
    }

    private getNamedCriteriaProxy(targetClass, name) {
        def proxy = null
        def metaProperty = targetClass.metaClass.getMetaProperty(name)
        if (metaProperty && Modifier.isStatic(metaProperty.modifiers)) {
            def prop = metaProperty.getProperty(targetClass)
            if (prop instanceof NamedCriteriaProxy) {
                proxy = prop
            }
        }
        proxy
    }

    private getPreparedCriteriaClosure(additionalCriteriaClosure = null) {
        def closureClone = criteriaClosure.clone()
        closureClone.resolveStrategy = Closure.DELEGATE_FIRST
        if (namedCriteriaParams) {
            closureClone = closureClone.curry(namedCriteriaParams)
        }
        def c = {
            closureClone.delegate = delegate
            if (previousInChain) {
                def previousClosure = previousInChain.getPreparedCriteriaClosure()
                previousClosure.delegate = delegate
                previousClosure()
            }
            closureClone()
            if (additionalCriteriaClosure) {
                additionalCriteriaClosure = additionalCriteriaClosure.clone()
                additionalCriteriaClosure.delegate = delegate
                additionalCriteriaClosure()
            }
        }
        c
    }
}
