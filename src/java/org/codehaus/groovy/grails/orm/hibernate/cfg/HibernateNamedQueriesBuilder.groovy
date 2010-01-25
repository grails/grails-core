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

import org.codehaus.groovy.grails.orm.hibernate.metaclass.*

/**
 * A builder that implements the ORM named queries DSL

 * @author Jeff Brown
 *
 */

class HibernateNamedQueriesBuilder {

    private final domainClass
    private final dynamicMethods

	private boolean initializationComplete = false

    /**
     * @param domainClass the GrailsDomainClass defining the named queries
     * @param grailsApplication a GrailsApplication instance
     * @param ctx the main spring application context
     */
    HibernateNamedQueriesBuilder(domainClass, grailsApplication, ctx) {
        this.domainClass = domainClass

        def classLoader = grailsApplication.classLoader
        def sessionFactory = ctx.getBean('sessionFactory')

        dynamicMethods = [
                new FindAllByBooleanPropertyPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new FindAllByPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new FindByPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new FindByBooleanPropertyPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new CountByPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new ListOrderByPersistentMethod(sessionFactory, classLoader)
        ]

    }

	def evaluate(Closure namedQueriesClosure) {
        def closure = namedQueriesClosure.clone()
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = this
        closure.call()
		initializationComplete = true
    }

    private handleMethodMissing = {String name, args ->
        def propertyName = name[0].toUpperCase() + name[1..-1]
        domainClass.metaClass.static."get${propertyName}" = {->
            // creating a new proxy each time because the proxy class has
            // some state that cannot be shared across requests (namedCriteriaParams)
            new NamedCriteriaProxy(criteriaClosure: args[0], domainClass: domainClass.clazz, dynamicMethods: dynamicMethods)
        }
    }

    def methodMissing(String name, args) {
        if (!initializationComplete && args && args[0] instanceof Closure) {
            return handleMethodMissing(name, args)
        }
        throw new MissingMethodException(name, HibernateNamedQueriesBuilder, args)
    }
}

class NamedCriteriaProxy {

    private criteriaClosure
    private domainClass
    private dynamicMethods
    private namedCriteriaParams

    def list(Object[] params) {
        def closureClone = getPreparedCriteriaClosure()
        def listClosure = {
            closureClone.delegate = delegate
            def paramsMap
            if (params && params[-1] instanceof Map) {
                paramsMap = params[-1]
                params = params.size() > 1 ? params[0..-2] : []
            }
            closureClone(* params)
            if (paramsMap?.max) {
                maxResults(paramsMap.max)
            }
            if (paramsMap?.offset) {
                firstResult paramsMap.offset
            }
        }
        domainClass.withCriteria(listClosure)
    }

    def call(Object[] params) {
        namedCriteriaParams = params
        this
    }

    def get(id) {
        def closureClone = getPreparedCriteriaClosure()
        def getClosure = {
            closureClone.delegate = delegate
            closureClone()
            eq 'id', id
            uniqueResult = true
        }
        domainClass.withCriteria(getClosure)
    }

    def count() {
        def closureClone = getPreparedCriteriaClosure()
        def countClosure = {
            closureClone.delegate = delegate
            closureClone()
            uniqueResult = true
            projections {
                rowCount()
            }
        }
        domainClass.withCriteria(countClosure)
    }

    def findWhere(params) {
        findAllWhere(params, true)
    }

    def findAllWhere(Map params, Boolean uniq = false) {
        def closureClone = criteriaClosure.clone()
        def queryClosure = {
            closureClone.delegate = delegate
            closureClone()
            params.each {key, val ->
                eq key, val
            }
            if (uniq) {
                maxResults 1
                uniqueResult = true
            }
        }
        domainClass.withCriteria(queryClosure)
    }


    def methodMissing(String methodName, args) {

        def method = dynamicMethods.find {it.isMethodMatch(methodName)}

        if (method) {
            def preparedClosure = getPreparedCriteriaClosure()
            return method.invoke(domainClass, methodName, preparedClosure, args)
        }
        throw new MissingMethodException(methodName, NamedCriteriaProxy, args)
    }

    private getPreparedCriteriaClosure() {
        def closureClone = criteriaClosure.clone()
        if (namedCriteriaParams) {
            closureClone = closureClone.curry(namedCriteriaParams)
        }
        closureClone
    }
}
