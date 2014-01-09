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

import grails.async.Promises
import grails.spring.BeanBuilder
import grails.test.GrailsMock
import grails.test.MockUtils
import grails.util.Holders
import grails.util.Metadata
import grails.validation.DeferredBindingActions
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import groovy.transform.CompileStatic
import junit.framework.AssertionFailedError

import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.CodecArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.databinding.DataBindingGrailsPlugin
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.ConstraintEvalUtils
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter
import org.grails.async.factory.SynchronousPromiseFactory
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.springframework.beans.CachedIntrospectionResults
import org.springframework.context.MessageSource
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.context.support.StaticMessageSource
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

/**
 * A base unit testing mixin that watches for MetaClass changes and unbinds them on tear down.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class GrailsUnitTestMixin {

    static {
        ExpandoMetaClass.enableGlobally()
    }

    static GrailsWebApplicationContext applicationContext
    static GrailsWebApplicationContext mainContext
    static GrailsApplication grailsApplication
    static ConfigObject config
    static MessageSource messageSource

    private static MetaClassRegistryCleaner metaClassRegistryListener = MetaClassRegistryCleaner.createAndRegister()

    Map validationErrorsMap = new IdentityHashMap()
    Set loadedCodecs = []

    @CompileStatic
    static void defineBeans(Closure callable) {
        def bb = new BeanBuilder()
        def binding = new Binding()
        binding.setVariable "application", grailsApplication
        bb.setBinding binding
        def beans = bb.beans(callable)
        beans.registerBeans(applicationContext)
    }

    @BeforeClass
    @CompileStatic
    static void initGrailsApplication() {
        ClassPropertyFetcher.clearClassPropertyFetcherCache()
        CachedIntrospectionResults.clearClassLoader(GrailsUnitTestMixin.class.classLoader)
        registerMetaClassRegistryWatcher()
        Promises.promiseFactory = new SynchronousPromiseFactory()
        if (applicationContext == null) {
            ExpandoMetaClass.enableGlobally()
            applicationContext = new GrailsWebApplicationContext()

            registerBeans()
            applicationContext.refresh()
            grailsApplication = applicationContext.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication)
            grailsApplication.metadata[Metadata.APPLICATION_NAME] =  GrailsUnitTestMixin.simpleName
            applicationContext.beanFactory.addBeanPostProcessor(new GrailsApplicationAwareBeanPostProcessor(grailsApplication))
            messageSource = applicationContext.getBean("messageSource", MessageSource)

            mainContext = new GrailsWebApplicationContext(applicationContext)
            mainContext.registerSingleton UrlConverter.BEAN_NAME, CamelCaseUrlConverter
            grailsApplication.mainContext = mainContext
            mainContext.refresh()
            grailsApplication.initialise()
            def servletContext = new MockServletContext()
            servletContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mainContext
            Holders.setServletContext servletContext

            grailsApplication.applicationContext = applicationContext
            config = grailsApplication.config
        }
    }

    protected static void registerBeans() {
        defineBeans(new DataBindingGrailsPlugin().doWithSpring)

        defineBeans {
            xmlns context:"http://www.springframework.org/schema/context"
            // adds AutowiredAnnotationBeanPostProcessor, CommonAnnotationBeanPostProcessor and others
            // see org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors method
            context.'annotation-config'()

            grailsProxyHandler(DefaultProxyHandler)
            grailsApplication(DefaultGrailsApplication)
            pluginManager(DefaultGrailsPluginManager, [] as Class[], ref("grailsApplication"))
            messageSource(StaticMessageSource)
            "${ConstraintsEvaluator.BEAN_NAME}"(DefaultConstraintEvaluator)
            conversionService(ConversionServiceFactoryBean)
        }
    }

    @After
    void resetGrailsApplication() {
        MockUtils.TEST_INSTANCES.clear()
        ClassPropertyFetcher.clearClassPropertyFetcherCache()
        grailsApplication?.clear()
        cleanupModifiedMetaClasses()
    }

    @CompileStatic
    static void registerMetaClassRegistryWatcher() {
        GroovySystem.metaClassRegistry.addMetaClassRegistryChangeEventListener metaClassRegistryListener
    }

    @CompileStatic
    static void cleanupModifiedMetaClasses() {
        metaClassRegistryListener.clean()
    }

    @AfterClass
    @CompileStatic
    static void deregisterMetaClassCleaner() {
        Promises.promiseFactory = null
        GroovySystem.metaClassRegistry.removeMetaClassRegistryChangeEventListener(metaClassRegistryListener)
    }

    /**
     * Mocks the given class (either a domain class or a command object)
     * so that a "validate()" method is added. This can then be used
     * to test the constraints on the class.
     */
    @CompileStatic
    void mockForConstraintsTests(Class clazz, List instances = []) {
        ConstraintEvalUtils.clearDefaultConstraints()
        MockUtils.prepareForConstraintsTests(clazz, validationErrorsMap, instances, ConstraintEvalUtils.getDefaultConstraints(grailsApplication.config))
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
    @CompileStatic
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
            throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name")
        }

        if (!clazz.isInstance(th)) {
            throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name, instead got Exception $th")
        }

        return th.message
    }
    /**
     * Loads the given codec, adding the "encodeAs...()" and "decode...()"
     * methods to objects.
     * @param codecClass The codec to load, e.g. HTMLCodec.
     */
    @CompileStatic
    void mockCodec(Class codecClass) {
        if (loadedCodecs.contains(codecClass)) {
            return
        }

        loadedCodecs << codecClass

        DefaultGrailsCodecClass grailsCodecClass = new DefaultGrailsCodecClass(codecClass)
        grailsCodecClass.configureCodecMethods()
        if(grailsApplication != null) {
            grailsApplication.addArtefact(CodecArtefactHandler.TYPE, grailsCodecClass)
        }
    }

    @AfterClass
    @CompileStatic
    static void shutdownApplicationContext() {
        if (applicationContext.isActive()) {
            if(grailsApplication.mainContext instanceof Closeable) {
                ((Closeable)grailsApplication.mainContext).close()
            }
            applicationContext.close()
        }
        ShutdownOperations.runOperations()
        DeferredBindingActions.clear()

        applicationContext = null
        grailsApplication = null
        Holders.setServletContext null
    }
}
