/*
 * Copyright 2008 the original author or authors.
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
package grails.test

import grails.validation.ValidationException
import groovy.xml.StreamingMarkupBuilder

import java.beans.Introspector
import java.beans.PropertyDescriptor

import org.apache.commons.logging.Log
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.testing.GrailsMockErrors
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.plugins.web.mimes.FormatInterceptor
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.codehaus.groovy.grails.web.binding.DataBindingLazyMetaPropertyMap
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.converters.Converter
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding
import org.codehaus.groovy.grails.web.taglib.GroovyPageAttributes
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.beans.BeanUtils
import org.springframework.beans.SimpleTypeConverter
import org.springframework.mock.web.MockHttpSession
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView

/**
 * A utility/helper class for mocking various types of Grails artifacts
 * and is one of the foundations of the Grails unit testing framework.
 *
 * @author Peter Ledbrook
 */
class MockUtils {
    static final COMPARATORS = [
            "IsNull",
            "IsNotNull",
            "LessThan",
            "LessThanEquals",
            "GreaterThan",
            "GreaterThanEquals",
            "NotEqual",
            "Like",
            "Ilike",
            "Between",
            "InList"].asImmutable()
    static final COMPARATORS_RE = COMPARATORS.join("|")
    static final DYNAMIC_FINDER_RE = /(\w+?)(${COMPARATORS_RE})?((And|Or)(\w+?)(${COMPARATORS_RE})?)?/

    static final errorsObjects = new ThreadLocalMap()
    static final Map<Class, Long> IDS = [:]

    /**
     * Enhances a class that has the method signatures setAttribute/getAttribute (such as the Request object) to allow property style access
     * @param clazz The class or interface to mock
     */
    static void mockAttributeAccess(Class clazz) {
        clazz.metaClass.getProperty = { String name ->
            if (delegate.metaClass.hasProperty(delegate,name)) {
                return delegate.metaClass.getMetaProperty(name).getProperty(delegate)
            }
            return delegate.getAttribute(name)
        }
        clazz.metaClass.setProperty = { String name, val ->
            if (delegate.metaClass.hasProperty(delegate,name)) {
                delegate.metaClass.getMetaProperty(name).setProperty(delegate, val)
            }
            else {
                delegate.setAttribute(name,val)
            }
        }
    }

    /**
     * Call this to mock the given controller class. It adds mock versions of
     * the various methods available on controllers that are normally provided
     * by Grails such as render and redirect.
     * @param clazz The class of the controller to mock
     */
    static void mockController(Class clazz) {
        mockLogging(clazz)
        addCommonWebProperties(clazz)

        // Set up the argument maps and method implementations for the
        // render() and redirect() methods.
        def fwdArgs = [:]
        def redArgs = [:]
        def renArgs = [:]
        def chaArgs = [:]
        def template = [:]
        def modelAndView = new ModelAndView()
        clazz.metaClass.getForwardArgs = {-> fwdArgs}
        clazz.metaClass.getRedirectArgs ={-> redArgs}
        clazz.metaClass.getRenderArgs ={-> renArgs}
        clazz.metaClass.getChainArgs ={-> chaArgs}
        clazz.metaClass.forward = {Map map -> forwardArgs.putAll(map)}
        clazz.metaClass.redirect = {Map map -> redirectArgs.putAll(map)}
        clazz.metaClass.chain = {Map map -> chainArgs.putAll(map)}
        clazz.metaClass.render = {String text -> delegate.response.writer << text}
        clazz.metaClass.render = {Converter arg -> delegate.response.writer << arg.toString()}

        clazz.metaClass.withFormat = { Closure callable ->
            def formatInterceptor = new FormatInterceptor()
            def originalDelegate = delegate
            try {
                callable.delegate = formatInterceptor
                callable.resolveStrategy = Closure.DELEGATE_ONLY
                callable.call()
            }
            finally {
                callable.delegate = originalDelegate
                callable.resolveStrategy = Closure.OWNER_FIRST
            }
            def formats = formatInterceptor.formatOptions
            def response = null
            if (request.format && formats.containsKey(request.format)) {
                response = formats[request.format]
            }
            else {
                response = formats[formats.firstKey()]
            }
            if (response instanceof Closure) {
                return response.call()
            }
            if (response instanceof Map) {
                return response
            }
        }

        clazz.metaClass.withForm { Closure callable ->
            def result
            if (!delegate.request.invalidToken) {
                def nullInvalidTokenHandler = { Closure c -> result }

                result = callable()
                if (result instanceof Map) {
                    result["invalidToken"] = nullInvalidTokenHandler
                }
                else {
                    result = [ invalidToken: nullInvalidTokenHandler ]
                }
            }
            else {
                delegate.flash.invalidToken = "token"
                result = [ invalidToken: { Closure c ->
                    // Clear the flash, since with have an "invalid token" handler.
                    delegate.flash.remove("invalidToken")

                    // Call the handler.
                    return c()
                } ]
            }

            return result
        }

        clazz.metaClass.render = {Map map ->
            renderArgs.putAll(map)
            if (map["status"] != null) {
                delegate.response.status = map["status"]
            }
            if (map["template"] != null) {
                assert map["view"] == null : "'view' cannot be used with 'template' in render"
                assert map["text"] == null : "'text' cannot be used with 'template' in render"

                template["name"] = map["template"]

                if (map["model"] != null) {
                    assert map["collection"] == null : "'collection' cannot be used with 'model' in render"
                    assert map["bean"] == null : "'bean' cannot be used with 'model' in render"

                    template["model"] = map["model"]
                }
                else if (map["collection"] != null) {
                    assert map["bean"] == null : "'bean' cannot be used with 'collection' in render"

                    template["collection"] = map["collection"]
                }
                else if (map["bean"] != null) {
                    template["bean"] = map["bean"]
                }
            }
            else if (map["view"] != null) {
                assert map["text"] == null : "'text' cannot be used with 'view' in render"

                modelAndView.viewName =  map["view"]
                modelAndView.addAllObjects(map["model"])
                return map["model"] // backwards compatibility for GRAILS-7022 - shouldn't ordinarily be referenced, use renderArgs instead
            }
            else if (map["text"] != null) {
                delegate.response.outputStream << map["text"]
            }
        }

        clazz.metaClass.render = {Map map, Closure c ->
            renderArgs.putAll(map)

            switch(map["contentType"]) {
            case null:
                break

            case "application/xml":
            case "text/xml":
                def b = new StreamingMarkupBuilder()
                if (map["encoding"]) b.encoding = map["encoding"]

                def writable = b.bind(c)
                delegate.response.outputStream << writable
                break

            default:
                println "Nothing"
                break
            }
        }

        clazz.metaClass.getTemplate = {-> template}
        clazz.metaClass.getModelAndView = {-> modelAndView}
        clazz.metaClass.setModelAndView = { ModelAndView mv -> modelAndView = mv}
    }

    /**
     * Call this to mock the given taglib class. It adds mock versions
     * of the various methods and properties available on tag libraries
     * that are normally provided by Grails. For example, it adds "flash",
     * "request", and the "render" tag. Other tags should be mocked
     * separately.
     * @param clazz The class of the tag library to mock.
     */
    static void mockTagLib(Class clazz) {
        mockLogging(clazz)
        addCommonWebProperties(clazz)

        // Set up the "out" property and the method for generating tag
        // errors.
        def mockOut = new StringWriter()
        clazz.metaClass.throwTagError = {String message -> throw new GrailsTagException(message) }
        clazz.metaClass.getOut = {-> mockOut }

        def mockPageScope = new GroovyPageBinding()
        clazz.metaClass.getPageScope = {->mockPageScope}

        // Render tag (called as a method).
        def renArgs = [:]
        def template = [:]
        clazz.metaClass.getRenderArgs ={-> renArgs}
        clazz.metaClass.getTemplate = {-> template}

        clazz.metaClass.render = {Map map ->
            renderArgs.putAll(map)
            if (map["template"]) {
                assert !map["view"]  : "'view' cannot be used with 'template' in render"
                assert !map["text"] : "'text' cannot be used with 'template' in render"

                template["name"] = map["template"]

                if (map["model"]) {
                    assert !map["collection"] : "'collection' cannot be used with 'model' in render"
                    assert !map["bean"] : "'bean' cannot be used with 'model' in render"

                    template["model"] = map["model"]
                }
                else if (map["collection"]) {
                    assert !map["bean"] : "'bean' cannot be used with 'collection' in render"

                    template["collection"] = map["collection"]
                }
                else if (map["bean"]) {
                    template["bean"] = map["bean"]
                }
            }
            else {
                assert false : "'template' attribute must be provided."
            }
        }
    }

    /**
     * Adds the properties common to controllers and tag libraries (and
     * anything else HTTP based) to objects of the given class.
     * @param clazz The class to add the properties to.
     */
    static void addCommonWebProperties(Class clazz) {
        mockAttributeAccess(MockHttpSession)
        def mockRequest = new GrailsMockHttpServletRequest()
        def mockResponse = new GrailsMockHttpServletResponse()
        def mockParams = new GroovyPageAttributes()
        def mockFlash = [:]
        def mockChainModel = [:]

        clazz.metaClass.getRequest = {-> mockRequest}
        clazz.metaClass.getResponse = {-> mockResponse}
        clazz.metaClass.getSession = {-> mockRequest.getSession() }
        clazz.metaClass.getParams = {-> mockParams}
        clazz.metaClass.getFlash = {-> mockFlash}
        clazz.metaClass.getChainModel = {-> mockChainModel}
        clazz.metaClass.getActionName = {-> RequestContextHolder.currentRequestAttributes().actionName }
        clazz.metaClass.getControllerName = {-> RequestContextHolder.currentRequestAttributes().controllerName }
        clazz.metaClass.getServletContext = {-> mockRequest.servletContext }

        // Provide access to "g" taglib namespace.
        clazz.metaClass.getG = {-> delegate }
    }

    /**
     * Mocks a command object class by adding a "validate()" method and
     * errors-related methods, like "getErrors()" and "hasErrors()".
     *
     * @deprecated Use {@link #mockCommandObject(Class, Map)}
     */
    static void mockCommandObject(Class clazz) {
        mockCommandObject(clazz, errorsObjects.get())
    }

    /**
     * Mocks a command object class by adding a "validate()" method and
     * errors-related methods, like "getErrors()" and "hasErrors()".
     */
    static void mockCommandObject(Class clazz, Map errorsMap) {
        addValidateMethod(clazz, null, errorsMap)
    }

    /**
     * Provides a mock implementation of the "log" property for the
     * given class. By default, debug and trace levels are ignored
     * but you can enable printing of debug statements via the <code>
     * enableDebug</code> argument.
     * @param clazz The class to add the log method to.
     * @param enableDebug An optional flag to switch on printing of
     * debug statements.
     */
    static void mockLogging(Class clazz, boolean enableDebug = false) {
        // Get the name of the class + the last component of the package
        // (if it the class is in a package).
        def pos = clazz.name.lastIndexOf('.')
        if (pos != -1) pos = clazz.name.lastIndexOf('.', pos - 1)
        def shortName = clazz.name.substring(pos + 1)

        def simple = {String key, def msg, Throwable t = null ->
            println "$key (${shortName}): $msg"
            if (t) {
                println " Exception thrown - ${t.message}"
            }
        }

        // Dynamically inject a mock logger that simply prints the
        // log message (and optional exception) to stdout.
        def mockLogger = [
            fatal: { def msg, Throwable t = null ->
                simple('FATAL', msg, t)
            },
            error: { def msg, Throwable t = null ->
                simple('ERROR', msg, t)
            },
            warn: { def msg, Throwable t = null ->
                simple('WARN', msg, t)
            },
            info: { def msg, Throwable t = null ->
                simple('INFO', msg, t)
            },
            debug: enableDebug ? { def msg, Throwable t = null ->
                simple('DEBUG', msg, t)
            } : { def msg, Throwable t = null -> },
            trace: { String msg, Throwable t = null -> },
            isFatalEnabled: {-> true},
            isErrorEnabled: {-> true},
            isWarnEnabled: {-> true},
            isInfoEnabled: {-> true},
            isDebugEnabled: {-> enableDebug},
            isTraceEnabled: {-> false} ] as Log
        clazz.metaClass.getLog = {-> mockLogger }
    }

    /**
     * Call this to mock the given domain class. It adds mock versions
     * of the various static and instance methods that are normally
     * injected by Grails. The methods behave as if there is no data
     * in the database, i.e. <code>DomainClass.list()</code> would
     * return an empty list.
     * @param clazz The domain class to mock.
     *
     * @deprecated Use {@link #mockDomain(Class, Map, List)}
     */
    static GrailsDomainClass mockDomain(Class clazz) {
        return mockDomain(clazz, [])
    }

    /**
     * <p>Call this to mock the given domain class. It adds mock versions
     * of the various static and instance methods that are normally
     * injected by Grails. The methods behave as if the given list of
     * domain instances are already in the database. In fact, the finders
     * and other query methods return the instances in the same order as
     * they appear in the list <code>testInstances</code>. This makes
     * testing much easier as you can rely on this ordering.</p>
     * @param clazz The domain class to mock.
     * @param testInstances A list of instances of type <code>clazz</code>
     * or of anything that can act as that type, such as a map with keys
     * that match the domain class's fields.
     *
     * @deprecated Use {@link #mockDomain(Class, Map, List)}
     */
    static GrailsDomainClass mockDomain(GrailsDomainClass dc, Class clazz, List testInstances) {
        mockDomain(dc, clazz, errorsObjects.get(), testInstances)
    }

    static GrailsDomainClass mockDomain(Class clazz, List testInstances) {
        mockDomain(null, clazz, testInstances)
    }

    static TEST_INSTANCES = [:]

    static GrailsDomainClass mockDomain(Class clazz, Map errorsMap, List testInstances = []) {
        mockDomain null, clazz, errorsMap, testInstances
    }

    /**
     * <p>Call this to mock the given domain class. It adds mock versions
     * of the various static and instance methods that are normally
     * injected by Grails. The methods behave as if the given list of
     * domain instances are already in the database. In fact, the finders
     * and other query methods return the instances in the same order as
     * they appear in the list <code>testInstances</code>. This makes
     * testing much easier as you can rely on this ordering.</p>
     * @param dc The optional GrailsDomainClass
     * @param clazz The domain class to mock.
     * @param errorsMap A map that Errors instances will be stored in.
     * Each Errors object will be stored against the domain instance it
     * is attached to, so this should ideally be an "identity" map of
     * some sort.
     * @param testInstances A list of instances of type <code>clazz</code>
     * or of anything that can act as that type, such as a map with keys
     * that match the domain class's fields.
     */
    static GrailsDomainClass mockDomain(GrailsDomainClass dc, Class clazz, Map errorsMap, List testInstances = []) {
        if (!dc) {
            dc = new DefaultGrailsDomainClass(clazz)
        }

        def rootInstances = testInstances.findAll { clazz.isInstance(it) }
        def childInstances = testInstances.findAll { clazz.isInstance(it) && it.class != clazz }.groupBy { it.class }

        TEST_INSTANCES[clazz] = rootInstances
        addDynamicFinders(clazz, rootInstances)
        addGetMethods(clazz, dc, rootInstances)
        addCountMethods(clazz, dc, rootInstances)
        addListMethod(clazz, rootInstances)
        addValidateMethod(clazz, dc, errorsMap, rootInstances)
        addDynamicInstanceMethods(clazz, rootInstances)
        addOtherStaticMethods(clazz, rootInstances)

        // Note that if the test instances are of type "clazz", they
        // will not have the extra dynamic methods because they were
        // created before the methods were added to the class.
        //
        // So, for each test object that is an instance of "clazz", we
        // manually change its metaclass to "clazz"'s so that it gets
        // the extra methods.
        updateMetaClassForClass(rootInstances, clazz)

        childInstances.each { Class childClass, List instances ->
            TEST_INSTANCES[childClass] = instances
            def childDomain = new DefaultGrailsDomainClass(childClass)
            addDynamicFinders(childClass, instances)
            addGetMethods(childClass, childDomain, instances)
            addCountMethods(childClass, childDomain, instances)
            addListMethod(childClass, instances)
            addValidateMethod(childClass, childDomain, errorsMap, instances)
            addDynamicInstanceMethods(childClass, instances)
            addOtherStaticMethods(childClass, instances)
            updateMetaClassForClass(rootInstances, childClass)
        }

        return dc
    }

    /**
     * Adds a <code>validate()</code> method to the given domain class
     * that performs validation against the constraints and returns a
     * map of errors. Each key in the map is the name of a field with
     * at least one error, while the value is the name of the constraint
     * that triggered the error, e.g. "nullable", or "min".
     * @param clazz The domain class to mock.
     *
     * @deprecated Use {@link #prepareForConstraintsTests(Class, Map, List)}
     */
    static void prepareForConstraintsTests(Class clazz) {
        prepareForConstraintsTests(clazz, [])
    }

    /**
     * <p>Adds a <code>validate()</code> method to the given domain class
     * that performs validation against the constraints and returns a
     * map of errors. Each key in the map is the name of a field with
     * at least one error, while the value is the name of the constraint
     * that triggered the error, e.g. "nullable", or "min".</p>
     * <p>The main use for this version of the method is to test unique
     * constraints. The domain instance being validated will be checked
     * against the given list of objects and if a uniqueness constraint
     * is violated, it will appear in the returned list of errors.</p>
     * @param clazz The domain class to mock.
     * @param testInstances A list of instances of type <code>clazz</code>
     * or of anything that can act as that type, such as a map with keys
     * that match the domain class's fields. In fact, the instances only
     * need properties that match the fields taking part in the unique
     * constraints.
     *
     * @deprecated Use {@link #prepareForConstraintsTests(Class, Map, List)}
     */
    static void prepareForConstraintsTests(Class clazz, List testInstances) {
        prepareForConstraintsTests(clazz, errorsObjects.get(), testInstances)
    }

    /**
     * <p>Adds a <code>validate()</code> method to the given domain class
     * that performs validation against the constraints and returns a
     * map of errors. Each key in the map is the name of a field with
     * at least one error, while the value is the name of the constraint
     * that triggered the error, e.g. "nullable", or "min".</p>
     * <p>The main use for this version of the method is to test unique
     * constraints. The domain instance being validated will be checked
     * against the given list of objects and if a uniqueness constraint
     * is violated, it will appear in the returned list of errors.</p>
     * @param clazz The domain class to mock.
     * @param errorsMap A map that Errors instances will be stored in.
     * Each Errors object will be stored against the domain instance it
     * is attached to, so this should ideally be an "identity" map of
     * some sort.
     * @param testInstances A list of instances of type <code>clazz</code>
     * or of anything that can act as that type, such as a map with keys
     * that match the domain class's fields. In fact, the instances only
     * need properties that match the fields taking part in the unique
     * constraints.
     */
    static void prepareForConstraintsTests(Class clazz, Map errorsMap, List testInstances = [], Map defaultConstraints = [:]) {
        def dc = null
        if (DomainClassArtefactHandler.isDomainClass(clazz))
           dc = new DefaultGrailsDomainClass(clazz, defaultConstraints)

        addValidateMethod(clazz, dc, errorsMap, testInstances, defaultConstraints)

        // Note that if the test instances are of type "clazz", they
        // will not have the extra dynamic methods because they were
        // created before the methods were added to the class.
        //
        // So, for each test object that is an instance of "clazz", we
        // manually change its metaclass to "clazz"'s so that it gets
        // the extra methods.
        updateMetaClassForClass(testInstances, clazz)
    }

    static void resetIds() {
        IDS.clear()
    }

    private static void addDynamicFinders(Class clazz, List testInstances) {
        // Implement the dynamic class methods for domain classes.

        clazz.metaClass.static.findAll = { -> TEST_INSTANCES[clazz] }

        clazz.metaClass.static.findAllWhere = { args = [:] ->
            TEST_INSTANCES[clazz].findAll { instance ->
                args.every { k,v -> instance[k] == v }
            }
        }

        clazz.metaClass.static.methodMissing = { method, args ->
            def m = method =~ /^find(All)?By${DYNAMIC_FINDER_RE}$/
            if (m) {
                def field = Introspector.decapitalize(m[0][2])
                def comparator = m[0][3]

                // How many arguments do we need to pass for the given
                // comparator?
                def numArgs = getArgCountForComparator(comparator)

                // Strip out that number of arguments from the ones
                // we've been passed.
                def subArgs = args[0..<numArgs]
                def result = processInstances(TEST_INSTANCES[clazz], field, comparator, subArgs)

                args = args[numArgs..<args.size()]

                // If we have a second clause, evaluate it now.
                def join = m[0][5]
                if (join) {
                    field = Introspector.decapitalize(m[0][6])
                    comparator = m[0][7]
                    numArgs = getArgCountForComparator(comparator)
                    subArgs = args[0..<numArgs]

                    def secondResult = processInstances(TEST_INSTANCES[clazz], field, comparator, subArgs)

                    args = args[numArgs..<args.size()]

                    // Combine the first result with the second result
                    // based on the join type.
                    if (join == "And") {
                        result = intersect(result, secondResult)
                    }
                    else if (join == "Or") {
                        result = intersect(TEST_INSTANCES[clazz], result + secondResult)
                    }
                    else {
                        throw new RuntimeException("Unrecognised join type: '$join'")
                    }
                }

                // Check whether we have any options, such as "sort".
                if (args) {
                    assert args[0] instanceof Map
                    result = applyQueryOptions(result, args[0])
                }

                if (m[0][1]) {
                    // We're doing a findAllBy* so return a list.
                    return result ?: []
                }
                // we're doing a findBy* so just return the first
                // result (or null if there are none).
                return result ? result[0] : null
            } else {
                m = method =~ /^countBy${DYNAMIC_FINDER_RE}$/
                if (m) {
                    switch(args.size()) {
                        case 0: return clazz."findAllBy${method[7..-1]}"().size()
                        case 1: return clazz."findAllBy${method[7..-1]}"(args[0]).size()
                        case 2: return clazz."findAllBy${method[7..-1]}"(args[0], args[1]).size()
                        case 3: return clazz."findAllBy${method[7..-1]}"(args[0], args[1], args[2]).size()
                        case 4: return clazz."findAllBy${method[7..-1]}"(args[0], args[1], args[2], args[3]).size()
                    }
                }
                else {
                    throw new MissingMethodException(method, delegate, args)
                }
            }
        }
    }

    /**
     * Adds methods that mock the behavior of the count() methods.
     */
    private static void addCountMethods(Class clazz, GrailsDomainClass dc, List testInstances) {
         clazz.metaClass.static.count = {-> testInstances.size() }
    }

    private static void addGetMethods(Class clazz, GrailsDomainClass dc, List testInstances) {
        // We need to know the type of the "id" field for some of these methods.
        Class idType = dc.identifier.type

        // First get()...
        clazz.metaClass.static.get = { id ->
            id = convertToType(id, idType)
            return testInstances.find { it?.id == id }
        }

        clazz.metaClass.static.get = { Serializable id ->
            id = convertToType(id, idType)
            return testInstances.find { it?.id == id }
        }

        // ..then read()...
        clazz.metaClass.static.read = { id ->
            // We don't do anything different to get(). We certainly
            // don't enforce the "read-only" aspect, which would over-
            // complicate the implementation without any real benefit.
            delegate.get id
        }
        clazz.metaClass.static.read = { Serializable id ->
            delegate.get id
        }

        // ..then load()...
        clazz.metaClass.static.load = { id -> delegate.get id }

       clazz.metaClass.static.load = { Serializable id ->
            delegate.get id
        }

        // ...then getAll()...
        clazz.metaClass.static.getAll = { Object[] args ->
            def idList = args
            if (args.length == 1 && (args[0] instanceof List || args[0].getClass().array)) {
                idList = args[0]
            }

            idList = idList?.collect { convertToType(it, idType) }
            return idList?.collect {id -> testInstances.find { it.id == id }}?.findAll { it != null }
        }

        clazz.metaClass.static.getAll = { List idList ->
            idList = idList?.collect { convertToType(it, idType) }
            return idList?.collect {id -> testInstances.find { it.id == id }}?.findAll { it != null }
        }

        // ...then ident()...
        clazz.metaClass.ident = {-> delegate.id }

        // ...and finally exists().
        clazz.metaClass.static.exists = {id ->
            id = convertToType(id, idType)
            return testInstances.find { it?.id == id } != null
        }
    }

    private static void addListMethod(Class clazz, List testInstances) {
        // Implement the dynamic class methods for domain classes.
        clazz.metaClass.static.list = { args = [:] ->
            if (args) {
                return applyQueryOptions(testInstances, args)
            }
            return testInstances
        }
    }

    /**
     * Adds the remaining static methods that are normally added to
     * domain classes.
     */
    private static void addOtherStaticMethods(Class clazz, List testInstances) {
        clazz.metaClass.static.create = {-> clazz.newInstance()}
    }

    private static void triggerEvent(Object delegate, String eventName) {
        if (delegate.respondsTo(eventName, [] as Object[])) {
            delegate."$eventName"()
        } else if (delegate.hasProperty(eventName) && delegate."$eventName" instanceof Closure) {
            delegate."$eventName"()
        }
    }

    private static void setTimestamp(Object delegate, String propertyName, PropertyDescriptor[] properties, Map mapping) {
        def property = properties.find { it.name == propertyName }
        if (property && mapping.autoTimestamp) {
            def value = property.propertyType.newInstance(System.currentTimeMillis())
            delegate."$propertyName" = value
        }
    }

    private static void addDynamicInstanceMethods(Class clazz, List testInstances) {
        // Add save() method.
        clazz.metaClass.save = { Map args = [:] ->
            if (!validate()) {
                if (args.failOnError) {
                    throw new ValidationException("Validation Error(s) occurred during save()", delegate.errors)
                }
                return null
            }

            def properties = Introspector.getBeanInfo(clazz).propertyDescriptors
            def mapping = evaluateMapping(clazz)

            boolean isInsert
            if (mapping?.id?.generator == "assigned") {
                isInsert = !testInstances.contains(delegate)
            } else {
                isInsert = !delegate.id
            }

            if (isInsert) {
                triggerEvent delegate, 'beforeInsert'
                if (!testInstances.contains(delegate)) {
                    testInstances << delegate
                    setId delegate, clazz
                }
                setTimestamp delegate, 'dateCreated', properties, mapping
                setTimestamp delegate, 'lastUpdated', properties, mapping
                triggerEvent delegate, 'afterInsert'
            } else {
                triggerEvent delegate, 'beforeUpdate'
                setTimestamp delegate, 'lastUpdated', properties, mapping
                triggerEvent delegate, 'afterUpdate'
            }

            return delegate
        }

        // Add delete() method.
        clazz.metaClass.delete = { Map args = [:] ->
            for (int i in 0..<testInstances.size()) {
                if (testInstances[i] == delegate) {
                    triggerEvent delegate, 'beforeDelete'
                    testInstances.remove(i)
                    triggerEvent delegate, 'afterDelete'
                    break
                }
            }
        }

        // these don't need to do anything.
        clazz.metaClass.discard = {-> delegate }
        clazz.metaClass.refresh = {-> delegate }
        clazz.metaClass.attach = {-> delegate }

        // instanceOf() method - just delegates to regular operator
        clazz.metaClass.instanceOf = { Class c -> c.isInstance(delegate) }

        // Add the "addTo*" and "removeFrom*" methods.

        def collectionTypes = [:]
        def hasMany = GrailsClassUtils.getStaticPropertyValue(clazz, 'hasMany')
        if (hasMany) {
            for (name in hasMany.keySet()) {
                // pre-populate with Set, override with PropertyDescriptors below
                collectionTypes[name] = Set
            }
        }

        Introspector.getBeanInfo(clazz).propertyDescriptors.each { PropertyDescriptor pd ->
            if (Collection.isAssignableFrom(pd.propertyType)) {
                collectionTypes[pd.name] = pd.propertyType
            }
        }

        collectionTypes.each { String propertyName, propertyType ->
            // Capitalise the name of the property.
            def collectionName = propertyName[0].toUpperCase() + propertyName[1..-1]

            clazz.metaClass."addTo$collectionName" = { arg ->
                def obj = delegate
                if (obj."$propertyName" == null) {
                    obj."$propertyName" = GrailsClassUtils.createConcreteCollection(propertyType)
                }

                def instanceClass
                if (arg instanceof Map) {
                    instanceClass = hasMany[propertyName]
                    arg = createFromMap(arg, instanceClass)
                }
                else {
                    instanceClass = arg.getClass()
                }

                obj."$propertyName" << arg

                // now set back-reference
                if (!(arg instanceof Map)) {
                    def otherHasMany = GrailsClassUtils.getStaticPropertyValue(instanceClass, 'hasMany')
                    // if there are hasMany definition, try to find back-reference among fields defined there
                    boolean fieldFound = false
                    if (otherHasMany) {
                        // many-to-many
                        otherHasMany.each { String otherCollectionName, Class otherCollectionType ->
                            if (clazz.isAssignableFrom(otherCollectionType) && clazz != otherCollectionType) {
                                if (arg."$otherCollectionName" == null) {
                                    arg."$otherCollectionName" = GrailsClassUtils.createConcreteCollection(otherCollectionType)
                                }
                                arg."$otherCollectionName" << obj
                                fieldFound = true
                            }
                        }
                    }
                    // if back-reference is not found, try among 1-many fields
                    if (!fieldFound) {
                        // 1-many
                        for (PropertyDescriptor pd in Introspector.getBeanInfo(instanceClass).propertyDescriptors) {
                            if (clazz.isAssignableFrom(pd.propertyType)) {
                                arg[pd.name] = obj
                            }
                        }
                    }
                }

                return obj
            }

            clazz.metaClass."removeFrom$collectionName" = { arg ->
                if (arg instanceof Map) {
                    arg = createFromMap(arg, hasMany[propertyName])
                }

                delegate."$propertyName"?.remove(arg)
                return delegate
            }
        }
    }

    private static createFromMap(map, instanceClass) {
        if (instanceClass) {
            def instance = instanceClass.newInstance()
            DataBindingUtils.bindObjectToInstance(instance, map)
            return instance
        }
        null
    }

    private static evaluateMapping(Class clazz) {
        Closure mappingBlock = GrailsClassUtils.getStaticPropertyValue(clazz, "mapping")
        def builder = new MappingBuilder()
        if (mappingBlock) {
            mappingBlock.delegate = builder
            mappingBlock.call()
        }
        return builder.mapping
    }

    /**
     * Adds a <code>validate()</code> method to the given domain class
     * or command object. It also adds the errors-related methods and
     * data-binding support (via the constructor and the <code>properties
     * </code> property).
     * @param clazz The domain or command class to add the methods to.
     * @param testInstances An optional list of existing instances of
     * the associated class, which is used to test the "unique" constraint.
     */
    private static void addValidateMethod(
            Class clazz,
            final GrailsDomainClass dc,
            final Map errorsMap,
            final List testInstances = [],
            final Map defaultConstraints = [:]) {
        def constraintsEvaluator = new DefaultConstraintEvaluator(defaultConstraints)

        // If we have a GrailsDomainClass, i.e. we are adding the method
        // to a domain class, then create a validator for it. This gives
        // us support for cascading validation, as well as being the
        // "real deal" (the validator used by running applications).
        def constrainedProperties
        if (dc) {
            def v = new GrailsDomainClassValidator()
            v.domainClass = dc
            constrainedProperties = constraintsEvaluator.evaluate(clazz, dc.properties)
        }
        else {
            def c = GrailsClassUtils.getStaticPropertyValue(clazz, "constraints")
            if (c instanceof Map) {
                constrainedProperties = c
            } else {
                def constraintsBuilder = new ConstrainedPropertyBuilder(clazz)
                // Get clazz's class hierarchy up to, but not including, Object
                // as a linked list. The list starts with the ultimate base class
                // and ends with "clazz".
                LinkedList classChain = new LinkedList()
                while (clazz != Object) {
                    classChain.addFirst(clazz)
                    clazz = clazz.getSuperclass()
                }

                // Now get build up our constraints from all "constraints"
                // properties in all the classes in the hierarchy.
                for (Iterator it = classChain.iterator(); it.hasNext();) {
                    clazz = (Class) it.next()
                    // Read the constraints.
                    c = GrailsClassUtils.getStaticPropertyValue(clazz, "constraints")

                    if (c) {
                        c = c.clone()
                        c.delegate = constraintsBuilder
                        try {
                            c.call()
                        } finally {
                            c.delegate = null
                        }
                    }
                }

                constrainedProperties = constraintsBuilder.constrainedProperties
            }
        }

        // Attach the instantiated constraints to the domain/command
        // object.
        clazz.metaClass.getConstraints = {->
            constrainedProperties
        }

        // Add data binding capabilities

        clazz.metaClass.constructor = { Map params ->
            def obj = BeanUtils.instantiateClass(delegate)
            DataBindingUtils.bindObjectToInstance(obj,params)
            return obj
        }

        clazz.metaClass.setProperties = { Object o ->
            DataBindingUtils.bindObjectToInstance(delegate,o)
        }

        clazz.metaClass.getProperties = {->
            new DataBindingLazyMetaPropertyMap(delegate)
        }

        // Add all the errors-related methods.
        clazz.metaClass.getErrors = {-> getErrorsFor(errorsMap, delegate) }
        clazz.metaClass.hasErrors = {-> getErrorsFor(errorsMap, delegate).hasErrors() }
        clazz.metaClass.setErrors = { Errors errors ->
            if (!(errors instanceof GrailsMockErrors)) {
                def mockErrors = new GrailsMockErrors(delegate)
                mockErrors.addAllErrors errors
                errors = mockErrors
            }
            setErrorsFor(errorsMap, delegate, errors)
        }
        clazz.metaClass.clearErrors = {-> clearErrorsFor(errorsMap, delegate) }

        // Finally add the "validate()" method, which can simply be
        // used to test the constraints or used from code under test.
        clazz.metaClass.validate = { Map args ->
            if (args == null) args = [:]
            def obj = delegate

            // Make sure any existing errors are cleared.
            clearErrors()

            // If we have a GrailsDomainClass, then we can use its
            // validator to perform the validation.
            if (dc) {
                dc.validator.validate(obj, errors, args["deepValidate"] == null ? true : args["deepValidate"])
            }

            // The GDC validator does not handle the "unique" constraint
            // for mocked domain classes, so we have to run through all
            // the constraints and check any "unique" ones. While doing
            // that, we also do the normal validation for the case where
            // we don't have a GDC, i.e. if we're validating a command
            // object.
            constrainedProperties.each { property, ConstrainedProperty constraint ->
                // Only perform the validation if we don't have a GDC
                // (since if there is one the validation has already
                // been done).
                if (!dc) constraint.validate(obj, obj."${property}", errors)

                // Handle the unique constraint if this field has one.
                def uniqueValue = constraint.getMetaConstraintValue("unique") ?: constraint.getAppliedConstraint("unique")?.parameter
                if (uniqueValue) {
                    def props
                    if (uniqueValue instanceof Boolean && uniqueValue) {
                        props = []
                    }
                    else if (uniqueValue instanceof String) {
                        props = [ uniqueValue ]
                    }
                    else if (uniqueValue instanceof Collection) {
                        props = uniqueValue
                    }

                    props = [ property ] + props
                    def existing = testInstances.find { inst -> !inst.is(obj) && props.every { inst."$it" != null && inst."$it" == obj."$it" } }
                    if (existing != null) {
                        errors.rejectValue(property, "unique")
                    }
                }
            }
            return !errors.hasErrors()
        }

        def beforeValidateHelper
        try {
            def helperClass = Thread.currentThread().contextClassLoader.loadClass('org.grails.datastore.gorm.support.BeforeValidateHelper')
            beforeValidateHelper = helperClass.newInstance()
        }
        catch (ignored) {
            // Hibernate isn't installed
        }

        // add no-arg attributes validator, just to be inline with what HibernatePluginSupport.addValidationMethods does.
        // It works lke validate(Map) with empty map
        clazz.metaClass.validate = { ->
            beforeValidateHelper?.invokeBeforeValidate delegate, null
            validate([:])
        }

        // add validator that is able to discard changes to the domain objects, that possible validator can do.
        // Validator is required for compatibility with HibernatePluginSupport.addValidationMethods.
        // Internally it call validator(Map) with evict parameter. Because current mock implementation of validator does not do any database interaction
        // so this validator works exactly the same way as validate()
        clazz.metaClass.validate = { Boolean b -> validate([evict: b]) }

        // add validator that validates only fields that names are passed in input list of fieldsToValdate.
        // All errors for the other fields are removed.
        clazz.metaClass.validate = { List fieldsToValidate ->
            beforeValidateHelper?.invokeBeforeValidate delegate, fieldsToValidate
            if (!validate([:]) && fieldsToValidate != null && !fieldsToValidate.isEmpty()) {
                def result = new GrailsMockErrors(delegate)
                for (e in errors.allErrors) {
                    if (e instanceof FieldError && !fieldsToValidate.contains(e.field)) {
                        continue
                    }
                    result.addError(e)
                }
                setErrors result
            }

            return !errors.hasErrors()
        }
    }

    /**
     * Badly named method that filters a list of objects using the
     * "findBy()" comparators such as "IsNull", "GreaterThan", etc.
     */
    private static processInstances(instances, property, comparator, args) {
        def result = []
        instances.each { record ->
            def propValue = record."${property}"
            switch(comparator) {
            case null:
                if (propValue == args[0]) result << record
                break

            case "IsNull":
                if (propValue == null) result << record
                break

            case "IsNotNull":
                if (propValue != null) result << record
                break

            case "LessThan":
                if (propValue < args[0]) result << record
                break

            case "LessThanEquals":
                if (propValue <= args[0]) result << record
                break

            case "GreaterThan":
                if (propValue > args[0]) result << record
                break

            case "GreaterThanEquals":
                if (propValue >= args[0]) result << record
                break

            case "NotEqual":
                if (propValue != args[0]) result << record
                break

            case "Like":
                if (propValue ==~ args[0].replaceAll("%", ".*")) result << record
                break

            case "Ilike":
                if (propValue ==~ /(?i)${args[0].replaceAll("%", ".*")}/) result << record
                break

            case "Between":
                if (propValue >= args[0] && propValue <= args[1]) result << record
                break

            case "InList":
                 if (propValue in args[0]) result << record
                 break

            default:
                throw new RuntimeException("Unrecognised comparator: ${comparator}")
            }
        }

        return result
    }

    private static int getArgCountForComparator(String comparator) {
        if (comparator == "Between") {
            return 2
        }
        if (["IsNull", "IsNotNull"].contains(comparator)) {
            return 0
        }

        return 1
    }

    /**
     * Applies the standard sorting and pagination query options to a
     * list of objects.
     */
    private static List applyQueryOptions(List instances, Map options) {
        List result = instances

        // Start by sorting the objects.
        def sort = options["sort"]
        if (sort) {
            def reverseSort = options["order"] == "desc"
            def ignoreCase = options["ignoreCase"] == null ? true : options["ignoreCase"]
            if (ignoreCase) {
                // Case-insensitive sort for string properties.
                result = instances.sort { val1, val2 ->
                    def retval
                    if (val1."${sort}" instanceof String) {
                        retval = val1."${sort}".compareToIgnoreCase(val2."${sort}")
                    }
                    else {
                        retval = val1."${sort}" <=> val2."${sort}"
                    }
                    return reverseSort ? -retval : retval
                }
            }
            else {
                result = instances.sort { val1, val2 ->
                    def retval = val1."${sort}" <=> val2."${sort}"
                    return reverseSort ? -retval : retval
                }
            }
        }

        // Now apply any pagination options.
        def offset = options["offset"] ?: 0
        def max = options["max"] ?: result.size()

        if (offset > result.size() || result.size() == 0) {
            result = []
        }
        else {
            offset = offset < 0 ? 0 : offset
            max = max - 1 + offset
            if (max >= result.size()) max = result.size() - 1
            if (max < 0) max = result.size() - 1

            result = result[offset..max]
        }

        return result
    }

    /**
     * Returns a list of all the items that are in both <code>left</code>
     * and <code>right</code>. The items in the returned list have the
     * same order as the items in <code>left</code>.
     */
    private static List intersect(List left, List right) {
        def result = new ArrayList(left.size())
        left.each { item ->
            if (right.contains(item)) result << item
        }

        return result
    }

    /**
     * Gets the errors instance for an object if one exists, or creates a
     * new one otherwise. The errors instances are stored in a thread-local
     * map.
     * @param object The object whose errors instance should be retrieved.
     * @param replaceExisting <code>true</code> if any existing errors
     * instance should be replaced by a fresh one.
     * @return the Errors instance attached to the object.
     */
    private static Errors getErrorsFor(Map errorsMap, object, boolean replaceExisting = false) {
        // Check whether there is already an errors object for this
        // command object. If so, use that...
        def errors = errorsMap[object]

        // ...otherwise create a new one and store it in the thread
        // local map.
        if (!errors || replaceExisting) {
            // This takes advantage of the fact that "clearErrorsFor()"
            // creates a new, empty Errors instance.
            errors = clearErrorsFor(errorsMap, object)
        }

        return errors
    }

    /**
     * Sets the errors instance for an object. The errors instances are
     * stored in a thread-local map.
     * @param object The object whose errors instance should be changed.
     * @param errors The new Errors instance for the object.
     */
    private static void setErrorsFor(Map errorsMap, object, Errors errors) {
        errorsMap[object] = errors
    }

    /**
     * Clears any errors that might be attached to the given object.
     * What it really does is replace the errors instance in the
     * thread-local map with a completely new one.
     * @param object The object to clear the errors from.
     * @return the new, empty Errors instance attached to the object.
     */
    private static Errors clearErrorsFor(Map errorsMap, object) {
        Errors errors = new GrailsMockErrors(object)
        errorsMap[object] = errors
        return errors
    }

    /**
     * Updates all object in the given collection of type "clazz" so
     * that their meta-class is whatever clazz's is. This also injects
     * an "id" property into all the objects.
     * @param instances The objects whose meta-classes need updating.
     * @param clazz The type of objects we want to update. The "new"
     * meta-class is retrieved from this class instance.
     */
    private static void updateMetaClassForClass(Collection instances, Class clazz) {
        // For each object in the collection that is an instance of
        // "clazz", we manually change its metaclass to "clazz"'s.
        instances.each { obj ->
            setId obj, clazz

            if (clazz.isAssignableFrom(obj.getClass())) {
                obj.metaClass = clazz.metaClass
            }
        }
    }

    private static void setId(instance, clazz) {
        def prop = instance.metaClass.hasProperty(instance, "id")
        def mapping = evaluateMapping(clazz)
        if (prop && !instance.id && mapping.id?.generator != "assigned") {
            def id = (IDS[clazz] ?: 0) + 1
            IDS[clazz] = id
            if (Number.isAssignableFrom(prop.type)) {
                instance.id = id
            } else if (prop.type == String) {
                instance.id = id.toString()
            }
        }
    }

    /**
     * Converts the given value to the given type if a conversion is
     * necessary.
     */
    private static convertToType(value, Class targetType) {
        if (value instanceof Number && Long.class.equals(targetType)) {
            return value.toLong()
        }

        return new SimpleTypeConverter().convertIfNecessary(value, targetType)
    }
}

/**
 * Custom ThreadLocal that stores a weak hash map. This is the map that
 * we use to associate command objects/domain instances with their
 * corresponding errors objects.
 */
class ThreadLocalMap extends ThreadLocal {
    protected initialValue() {
        return new WeakHashMap()
    }
}

class MappingBuilder {
    def mapping = [autoTimestamp: true]

    void methodMissing(String name, args) {
        mapping[name] = args[0]
    }
}
