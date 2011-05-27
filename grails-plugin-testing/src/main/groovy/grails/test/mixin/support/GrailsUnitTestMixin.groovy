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
package grails.test.mixin.support

import grails.spring.BeanBuilder
import grails.test.GrailsMock
import grails.test.MockUtils
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
import org.springframework.context.support.StaticMessageSource
import junit.framework.AssertionFailedError
import grails.validation.DeferredBindingActions

/**
 * A base unit testing mixin that watches for MetaClass changes and unbinds them on tear down
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class GrailsUnitTestMixin {

    static GrailsWebApplicationContext applicationContext
    static GrailsApplication grailsApplication
    static ConfigObject config
    static StaticMessageSource messageSource

    private static List emcEvents = []
    Map validationErrorsMap = new IdentityHashMap()
    Set loadedCodecs = []

    static void defineBeans(Closure callable) {
        def bb = new BeanBuilder()
        def beans = bb.beans(callable)
        beans.registerBeans(applicationContext)
    }

    @BeforeClass
    static void initGrailsApplication() {
        if (applicationContext == null) {
            ExpandoMetaClass.enableGlobally()
            applicationContext = new GrailsWebApplicationContext()
            final autowiringPostProcessor = new AutowiredAnnotationBeanPostProcessor()
            autowiringPostProcessor.beanFactory = applicationContext.autowireCapableBeanFactory
            applicationContext.beanFactory.addBeanPostProcessor(autowiringPostProcessor)

            defineBeans {
                grailsProxyHandler(DefaultProxyHandler)
                grailsApplication(DefaultGrailsApplication)
                messageSource(StaticMessageSource)
            }
            applicationContext.refresh()
            grailsApplication = applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication)
            applicationContext.beanFactory.addBeanPostProcessor(new GrailsApplicationAwareBeanPostProcessor(grailsApplication))
            messageSource = applicationContext.getBean("messageSource")
            grailsApplication.initialise()

            grailsApplication.applicationContext = applicationContext
            config = grailsApplication.config
        }
    }

    @After
    void resetGrailsApplication() {
        grailsApplication?.clear()
        DeferredBindingActions.clear()
    }

    /**
     * Mocks the given class (either a domain class or a command object)
     * so that a "validate()" method is added. This can then be used
     * to test the constraints on the class.
     */
    void mockForConstraintsTests(Class clazz, List instances = []) {
        MockUtils.prepareForConstraintsTests(clazz, validationErrorsMap, instances)
    }

    /**
     * Creates a new Grails mock for the given class. Use it as you
     * would use MockFor and StubFor.
     * @param clazz The class to mock.
     * @param loose If <code>true</code>, the method returns a loose-
     * expectation mock, otherwise it returns a strict one. The default
     * is a strict mock.
     */
    GrailsMock mockFor(Class clazz, boolean loose = false) {
        return new GrailsMock(clazz, loose)
    }

    /**
     * Asserts that the given code closure fails when it is evaluated
     *
     * @param code
     * @return the message of the thrown Throwable
     */
    String shouldFail(Closure code) {
        boolean failed = false
        String result = null
        try {
            code.call()
        }
        catch (GroovyRuntimeException gre) {
            failed = true
            result = ScriptBytecodeAdapter.unwrap(gre).getMessage()
        }
        catch (Throwable e) {
            failed = true
            result = e.getMessage()
        }
        if (!failed) {
            throw new AssertionFailedError("Closure " + code + " should have failed")
        }

        return result
    }

    /**
     * Asserts that the given code closure fails when it is evaluated
     * and that a particular exception is thrown.
     *
     * @param clazz the class of the expected exception
     * @param code  the closure that should fail
     * @return the message of the expected Throwable
     */
    String shouldFail(Class clazz, Closure code) {
        Throwable th = null
        try {
            code.call()
        } catch (GroovyRuntimeException gre) {
            th = ScriptBytecodeAdapter.unwrap(gre)
        } catch (Throwable e) {
            th = e
        }

        if (th == null) {
            throw new AssertionFailedError("Closure " + code + " should have failed with an exception of type " + clazz.getName())
        } else if (!clazz.isInstance(th)) {
            throw new AssertionFailedError("Closure " + code + " should have failed with an exception of type " + clazz.getName() + ", instead got Exception " + th);
        }
        return th.getMessage();
    }
    /**
     * Loads the given codec, adding the "encodeAs...()" and "decode...()"
     * methods to objects.
     * @param codecClass The codec to load, e.g. HTMLCodec.
     */
    void mockCodec(Class codecClass) {
        if (loadedCodecs.contains(codecClass)) {
            return
        }

        loadedCodecs << codecClass

        // Instantiate the codec so we can use it.
        final codec = codecClass.newInstance()

        // Add the encode and decode methods.
        def codecName = GrailsNameUtils.getLogicalName(codecClass, "Codec")
        Object.metaClass."encodeAs$codecName" = { -> codec.encode(delegate) }
        Object.metaClass."decode$codecName" = { -> codec.decode(delegate) }
    }

    private metaClassRegistryListener = { MetaClassRegistryChangeEvent event ->
        GrailsUnitTestMixin.emcEvents << event
    } as MetaClassRegistryChangeEventListener

    @Before
    void registerMetaClassRegistryWatcher() {
        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener metaClassRegistryListener
    }

    @After
    void cleanupModifiedMetaClasses() {
        GroovySystem.metaClassRegistry.removeMetaClassRegistryChangeEventListener(metaClassRegistryListener)
        emcEvents*.clazz.each { GroovySystem.metaClassRegistry.removeMetaClass(it)}
        emcEvents.clear()
    }

    @AfterClass
    static void shutdownApplicationContext() {
        if (applicationContext.isActive()) {
            applicationContext.close()
        }
        applicationContext = null
        grailsApplication = null
    }
}
