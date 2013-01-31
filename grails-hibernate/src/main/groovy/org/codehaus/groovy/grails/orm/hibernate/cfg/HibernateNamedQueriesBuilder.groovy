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

class NamedCriteriaProxy<T> {

    private criteriaClosure
    private GrailsDomainClass domainClass
    private dynamicMethods
    private namedCriteriaParams
    private previousInChain
    private queryBuilder
    private inCountMethod = false

    private invokeCriteriaClosures(additionalCriteriaClosures = null) {
        def crit = getPreparedCriteriaClosure(additionalCriteriaClosures)
        crit()
    }

    void propertyMissing(String propName, val) {
        queryBuilder?."${propName}" = val
    }

    private listInternal(Object[] params, def additionalCriteriaClosures, Boolean isDistinct) {
        def listClosure = {
            queryBuilder = delegate
            invokeCriteriaClosures(additionalCriteriaClosures)
            if (isDistinct) {
                resultTransformer = CriteriaSpecification.DISTINCT_ROOT_ENTITY
            }
        }

        def paramsMap
        if (params && params[-1] instanceof Map) {
            paramsMap = params[-1]
        }

        if (paramsMap) {
            domainClass.clazz.createCriteria().list(paramsMap, listClosure)
        } else {
            domainClass.clazz.withCriteria(listClosure)
        }
    }

    def list(Object[] params, def additionalCriteriaClosures = null) {
        handleParams(params, true, false)
    }

    def listDistinct(Object[] params, def additionalCriteriaClosures = null) {
        handleParams(params, true, true)
    }

    def call(Object[] params) {
        handleParams(params, false)
    }

    /**
     * Method that interprets the params for all calls to the named query
     * @param params - all the parameters
     * @param calledFromList - depending on this flag, the method considers that the first parameters from the array are named criteria params
     * @param isDistinct  - distinct flag
     * @return
     */
    private handleParams(Object[] params, boolean calledFromList, Boolean isDistinct = false){

        // will handle the following cases
        // 1) no additional criteria closures
        // 2) one or multiple criteria closures at the end
        // 3) an array of multiple criteria closures at the end

        boolean isLastParameterCollectionOfClosures =  params &&
            ([Collection, Closure[]].any { it.isAssignableFrom(params[-1].getClass()) }) &&  // check if last parameter is a collection
            ( params[-1] && (params[-1][0] instanceof Closure))  // check that it contains at least a Closure

        if ( calledFromList || ((params && (params[-1] instanceof Closure) ) || isLastParameterCollectionOfClosures)) {
            def additionalCriteriaClosures = []

            if (isLastParameterCollectionOfClosures){
                //the last parameter is a collection of additional criteria closures
                additionalCriteriaClosures = params[-1]
                params = removeLastParam(params)

            } else{
                // one or multiple criteria Closures at the end
                while (params && (params[-1] instanceof Closure)) {
                    additionalCriteriaClosures << params[-1]
                    params = removeLastParam(params)
                }

            }

            if (!calledFromList){
                // after stripping all the closures set the namedCriteriaParams
                if (params) {
                    if (params[-1] instanceof Map) {  // the last parameter is a map with list settings
                        if (params.length > 1) {
                            setNamedCriteriaParams(params[0..-2] as Object[])
                            params = [params[-1]] as Object[]
                        }
                    } else {    // no list settings
                        setNamedCriteriaParams(params)
                        params = new Object[0]
                    }
                }
            }

            //forward call to list
            listInternal(params, additionalCriteriaClosures, isDistinct)
        } else {
            //no additional criteria closures
            setNamedCriteriaParams(params)
            this
        }
    }


    /**
     * utility method that verifies if the parameters are compatible with the named query
     * @param params
     */
    private void setNamedCriteriaParams(params){
        //for now only check the no of parameters
        if ((params.size() != this.criteriaClosure.maximumNumberOfParameters) && !(params.size()==0 && this.criteriaClosure.maximumNumberOfParameters==1)){
            throw new IllegalArgumentException("""
                    The named query must be invoked with ${this.criteriaClosure.maximumNumberOfParameters} parameters.
                """)
        }
        namedCriteriaParams = params
    }

    def removeLastParam(params){
        params.length > 1 ? params[0..-2] : [:]
    }

    T get() {
        def getClosure = {
            queryBuilder = delegate
            invokeCriteriaClosures()
            maxResults 1
            uniqueResult = true
        }
        domainClass.clazz.withCriteria(getClosure)
    }

    T get(id) {
        id = HibernatePluginSupport.convertValueToIdentifierType(domainClass, id)
        def getClosure = {
            queryBuilder = delegate
            invokeCriteriaClosures()
            eq 'id', id
            uniqueResult = true
        }
        domainClass.clazz.withCriteria(getClosure)
    }

    int count(def additionalCriteriaClosures) {
        def countClosure = {
            inCountMethod = true
            queryBuilder = delegate
            invokeCriteriaClosures(additionalCriteriaClosures)
            uniqueResult = true
            projections {
                rowCount()
            }
            inCountMethod = false
        }
        domainClass.clazz.withCriteria(countClosure)
    }

    def findWhere(params) {
        findAllWhere(params, true)
    }

    void order(String propName) {
        if (!inCountMethod) {
            queryBuilder?.order propName
        }
    }

    void order(String propName, String direction) {
        if (!inCountMethod) {
            queryBuilder?.order propName, direction
        }
    }

    def findAllWhere(Map params, Boolean uniq = false) {
        def queryClosure = {
            queryBuilder = delegate
            invokeCriteriaClosures()
            params.each { key, val ->
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

    private getPreparedCriteriaClosure(additionalCriteriaClosures = null) {
        def closureClone = criteriaClosure.clone()
        closureClone.resolveStrategy = Closure.DELEGATE_FIRST
        if (namedCriteriaParams) {
            closureClone = closureClone.curry(*namedCriteriaParams)
        }
        def c = {
            closureClone.delegate = delegate
            if (previousInChain) {
                def previousClosure = previousInChain.getPreparedCriteriaClosure()
                previousClosure.delegate = delegate
                previousClosure()
            }
            closureClone()
            if (additionalCriteriaClosures) {
                additionalCriteriaClosures.each { Closure additionalCriteriaClosure ->
                    additionalCriteriaClosure = additionalCriteriaClosure.clone()
                    additionalCriteriaClosure.delegate = delegate
                    additionalCriteriaClosure()
                }
            }
        }
        c
    }
}
