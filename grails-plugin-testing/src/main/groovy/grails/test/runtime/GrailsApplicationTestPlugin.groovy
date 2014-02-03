/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime;

import java.lang.reflect.Modifier;

import grails.async.Promises
import grails.spring.BeanBuilder
import grails.test.MockUtils
import grails.util.Holders
import grails.util.Metadata
import grails.validation.DeferredBindingActions
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.CodecArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations
import org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport
import org.codehaus.groovy.grails.validation.ConstraintEvalUtils
import org.grails.async.factory.SynchronousPromiseFactory
import org.junit.runner.Description
import org.springframework.beans.CachedIntrospectionResults
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

/**
 * A TestPlugin for TestRuntime that builds the GrailsApplication instance for tests
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
class GrailsApplicationTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['metaClassCleaner']
    String[] providedFeatures = ['grailsApplication']
    int ordinal = 0

    void initGrailsApplication(TestRuntime runtime, Map callerInfo) {
        GrailsWebApplicationContext applicationContext = new GrailsWebApplicationContext()

        DefaultGrailsApplication grailsApplication = new DefaultGrailsApplication()
        if(callerInfo.description instanceof Description) {
            executeDoWithConfigCallback(runtime, grailsApplication, (Description)callerInfo.description)
        }
        grailsApplication.initialise()
        grailsApplication.setApplicationContext(applicationContext)
        if(!grailsApplication.metadata[Metadata.APPLICATION_NAME]) {
            grailsApplication.metadata[Metadata.APPLICATION_NAME] = "GrailsUnitTestMixin"
        }
        grailsApplication.applicationContext = applicationContext
        runtime.putValue("grailsApplication", grailsApplication)
        registerBeans(runtime, grailsApplication)
        if(callerInfo.description instanceof Description) {
            executeDoWithSpringCallback(runtime, grailsApplication, (Description)callerInfo.description)
        }
        applicationContext.refresh()

        GrailsWebApplicationContext mainContext = new GrailsWebApplicationContext(applicationContext)
        mainContext.registerSingleton UrlConverter.BEAN_NAME, CamelCaseUrlConverter
        mainContext.refresh()
        grailsApplication.mainContext = mainContext
        
        MockServletContext servletContext = new MockServletContext()
        servletContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext
        servletContext.setAttribute ApplicationAttributes.APPLICATION_CONTEXT, applicationContext
        applicationContext.servletContext = servletContext
        mainContext.servletContext = servletContext
        
        Holders.setServletContext servletContext
        runtime.putValue("servletContext", servletContext)
        
        applicationInitialized(runtime, grailsApplication)
    }

    void initialState() {
        ExpandoMetaClass.enableGlobally()
        ClassPropertyFetcher.clearClassPropertyFetcherCache()
        CachedIntrospectionResults.clearClassLoader(this.getClass().classLoader)
        Promises.promiseFactory = new SynchronousPromiseFactory()
    }
    
    void registerBeans(TestRuntime runtime, DefaultGrailsApplication grailsApplication) {
        runtime.publishEvent("registerBeans", [grailsApplication: grailsApplication], [immediateDelivery: true])
    }
    
    @CompileStatic(TypeCheckingMode.SKIP)
    void executeDoWithSpringCallback(TestRuntime runtime, DefaultGrailsApplication grailsApplication, Description testDescription) {
        def testClass=testDescription.testClass
        MetaProperty doWithSpringProperty=testClass.getMetaClass().hasProperty(testClass, "doWithSpring")
        if(doWithSpringProperty && Modifier.isStatic(doWithSpringProperty?.modifiers)) {
            def closure=testClass.doWithSpring.clone()
            runtime.publishEvent("defineBeans", [closure: closure], [immediateDelivery: true])
        }
    }
    
    @CompileStatic(TypeCheckingMode.SKIP)
    void executeDoWithConfigCallback(TestRuntime runtime, DefaultGrailsApplication grailsApplication, Description testDescription) {
        def testClass=testDescription.testClass
        def configClosure
        MetaProperty doWithConfigProperty=testClass.getMetaClass().hasProperty(testClass, "doWithConfig")
        if(doWithConfigProperty && Modifier.isStatic(doWithConfigProperty?.modifiers)) {
            configClosure=testClass.doWithConfig.clone()
        } else {
            MetaMethod doWithConfigMetaMethod=testClass.getMetaClass().respondsTo(testClass, "doWithConfig")?.find{it}
            if(doWithConfigMetaMethod?.isStatic()) {
                configClosure={config -> testClass.doWithConfig(config) }
            }
        }
        if(configClosure) {
            configClosure(grailsApplication.config)
        }
    }

    void applicationInitialized(TestRuntime runtime, DefaultGrailsApplication grailsApplication) {
        runtime.publishEvent("applicationInitialized", [grailsApplication: grailsApplication])
        // TODO: which is correct mainContext or parentContext?
        ConvertersPluginSupport.enhanceApplication(grailsApplication, grailsApplication.mainContext)
    }
    
    Set getLoadedCodecs(TestRuntime runtime) {
        (Set)runtime.getValueOrCreate("loadedCodecs", { new HashSet() })
    }
    
    void mockCodec(TestRuntime runtime, Class codecClass) {
        Set loadedCodecs = getLoadedCodecs(runtime)
        if (loadedCodecs.contains(codecClass)) {
            return
        }
        loadedCodecs << codecClass
        DefaultGrailsCodecClass grailsCodecClass = new DefaultGrailsCodecClass(codecClass)
        grailsCodecClass.configureCodecMethods()
        if(runtime.containsValueFor('grailsApplication')) {
            def grailsApplication = ((GrailsApplication)runtime.getValue('grailsApplication'))
            grailsApplication.addArtefact(CodecArtefactHandler.TYPE, grailsCodecClass)
        }
    }
    
    GrailsApplication getGrailsApplication(TestRuntime runtime) {
        (GrailsApplication)runtime.getValue('grailsApplication')
    }
    
    void mockForConstraintsTests(TestRuntime runtime, Class clazz, List instances) {
        ConstraintEvalUtils.clearDefaultConstraints()
        MockUtils.prepareForConstraintsTests(clazz, (Map)runtime.getValueOrCreate("validationErrorsMap", { new IdentityHashMap() }), instances ?: [], ConstraintEvalUtils.getDefaultConstraints(getGrailsApplication(runtime).config))
    }

    void defineBeans(TestRuntime runtime, Closure callable) {
        def bb = new BeanBuilder()
        def binding = new Binding()
        DefaultGrailsApplication grailsApplication = (DefaultGrailsApplication)runtime.getValue("grailsApplication")
        binding.setVariable "application", grailsApplication
        bb.setBinding binding
        def beans = bb.beans(callable)
        beans.registerBeans((BeanDefinitionRegistry)grailsApplication.getParentContext())
    }

    void resetGrailsApplication(TestRuntime runtime) {
        MockUtils.TEST_INSTANCES.clear()
        ClassPropertyFetcher.clearClassPropertyFetcherCache()
        if(runtime.containsValueFor('grailsApplication')) {
            ((DefaultGrailsApplication)runtime.getValue('grailsApplication'))?.clear()
        }
        runtime.removeValue("loadedCodecs")
    }
    
    void shutdownApplicationContext(TestRuntime runtime) {
        if(runtime.containsValueFor("grailsApplication")) {
            DefaultGrailsApplication grailsApplication = (DefaultGrailsApplication)runtime.getValue("grailsApplication")
            ApplicationContext applicationContext = grailsApplication.getParentContext()
            
            if (applicationContext instanceof ConfigurableApplicationContext) {
                if (((ConfigurableApplicationContext) applicationContext).isActive()) {
                    if(grailsApplication.mainContext instanceof Closeable) {
                        ((Closeable)grailsApplication.mainContext).close()
                    }
                    applicationContext.close()
                }
            }
            
            ShutdownOperations.runOperations()
            DeferredBindingActions.clear()
    
            runtime.removeValue("grailsApplication")
            
            Holders.setServletContext null
            runtime.removeValue("servletContext")
            
            Promises.promiseFactory = null
        }
    }
    
    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'beforeClass':
            case 'before':
                // trigger grailsApplication initialization by requesting value
                event.runtime.getValue("grailsApplication", event.arguments)
                break
            case 'afterClass':
                shutdownApplicationContext(event.runtime)
                break
            case 'after':
                resetGrailsApplication(event.runtime)
                break
            case 'grailsApplicationRequested':
                initialState()
                initGrailsApplication(event.runtime, (Map)event.arguments.callerInfo)
                break
            case 'valueMissing':
                if(event.arguments.name=='grailsApplication') {
                    event.runtime.publishEvent('grailsApplicationRequested', [callerInfo: event.arguments.callerInfo])
                }
                break
            case 'defineBeans':
                defineBeans(event.runtime, (Closure)event.arguments.closure)
                break
            case 'mockCodec':   
                mockCodec(event.runtime, (Class)event.arguments.codecClass)
                break
            case 'mockForConstraintsTests':   
                mockForConstraintsTests(event.runtime, (Class)event.arguments.clazz, (List)event.arguments.instances)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        
    }
}
