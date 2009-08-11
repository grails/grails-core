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

/**
 * A builder that implements the ORM named queries DSL

 * @author Jeff Brown
 *
 */

class HibernateNamedQueriesBuilder {

    private final domainClass


    /**
     * @param domainClass the GrailsDomainClass defining the named queries
     */
    HibernateNamedQueriesBuilder(domainClass) {
        this.domainClass = domainClass
    }

    def evaluate(Closure namedQueriesClosure) {
        def closure = namedQueriesClosure.clone()
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = this
        closure.call()
    }

    private handleMethodMissing = { String name, args ->
        def propertyName = name[0].toUpperCase() + name[1..-1]
        def proxy = new NamedCriteriaProxy(criteriaClosure: args[0], domainClass: domainClass.clazz)
        domainClass.metaClass.static."get${propertyName}" = { ->
            proxy   
        }

    }

    void methodMissing(String name, args) {
        if(args && args[0] instanceof Closure) {
            handleMethodMissing(name, args)
        }
    }
}

class NamedCriteriaProxy {

    private criteriaClosure
    private domainClass

    def list(params = [:]) {
        def closureClone = criteriaClosure.clone()
        def listClosure = {
            closureClone.delegate = delegate
            closureClone()
            if(params.max) {
                maxResults(params.max)
            }
            if(params.offset) {
                firstResult params.offset
            }
        }
        domainClass.withCriteria(listClosure)
    }

    def call(params = [:]) {
        list(params)
    }

    def get(id) {
        def closureClone = criteriaClosure.clone()
        def getClosure = {
            closureClone()
            eq 'id', id
            uniqueResult = true
        }
        domainClass.withCriteria(getClosure)
    }

    def count() {
        def closureClone = criteriaClosure.clone()
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

    def findAllWhere(params) {
        def closureClone = criteriaClosure.clone()
        def queryClosure = {
            closureClone.delegate = delegate
            closureClone()
            params.each {key , val ->
                eq key, val
            }
        }
        domainClass.withCriteria(queryClosure)
    }
}
