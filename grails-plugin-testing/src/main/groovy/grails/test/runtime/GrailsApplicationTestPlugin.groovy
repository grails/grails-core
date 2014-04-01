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
import org.codehaus.groovy.grails.web.context.ServletEnvironmentGrailsApplicationDiscoveryStrategy

import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.CodecArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations
import org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport
import org.codehaus.groovy.grails.validation.ConstraintEvalUtils
import org.grails.async.factory.SynchronousPromiseFactory
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
        executeDoWithConfigCallback(runtime, grailsApplication, callerInfo)
        grailsApplication.initialise()
        grailsApplication.setApplicationContext(applicationContext)
        if(!grailsApplication.metadata[Metadata.APPLICATION_NAME]) {
            grailsApplication.metadata[Metadata.APPLICATION_NAME] = "GrailsUnitTestMixin"
        }
        grailsApplication.applicationContext = applicationContext
        runtime.putValue("grailsApplication", grailsApplication)
        registerBeans(runtime, grailsApplication)
        executeDoWithSpringCallback(runtime, grailsApplication, callerInfo)
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
        Holders.addApplicationDiscoveryStrategy(new ServletEnvironmentGrailsApplicationDiscoveryStrategy(servletContext));
        Holders.setGrailsApplication(grailsApplication)
        runtime.putValue("servletContext", servletContext)
        
        applicationInitialized(runtime, grailsApplication)
    }

    void initialState() {
        ExpandoMetaClass.enableGlobally()
        Holders.clear()
        ClassPropertyFetcher.clearClassPropertyFetcherCache()
        CachedIntrospectionResults.clearClassLoader(this.getClass().classLoader)
        Promises.promiseFactory = new SynchronousPromiseFactory()
    }
    
    void registerBeans(TestRuntime runtime, DefaultGrailsApplication grailsApplication) {
        runtime.publishEvent("registerBeans", [grailsApplication: grailsApplication], [immediateDelivery: true])
    }
    
    @CompileStatic(TypeCheckingMode.SKIP)
    void executeDoWithSpringCallback(TestRuntime runtime, DefaultGrailsApplication grailsApplication, Map callerInfo) {
        def testInstanceOrClass=callerInfo.testInstance ?: callerInfo.testClass
        MetaProperty doWithSpringProperty=testInstanceOrClass.getMetaClass().hasProperty(testInstanceOrClass, "doWithSpring")
        boolean atClassLevel=(testInstanceOrClass instanceof Class) as boolean
        if(doWithSpringProperty != null && (!atClassLevel || Modifier.isStatic(doWithSpringProperty.getModifiers()))) {
            def closure=doWithSpringProperty.getProperty(testInstanceOrClass)
            runtime.publishEvent("defineBeans", [closure: closure], [immediateDelivery: true])
        }
    }
    
    @CompileStatic(TypeCheckingMode.SKIP)
    void executeDoWithConfigCallback(TestRuntime runtime, DefaultGrailsApplication grailsApplication, Map callerInfo) {
        def testInstanceOrClass=callerInfo.testInstance ?: callerInfo.testClass
        def configClosure
        MetaProperty doWithConfigProperty=testInstanceOrClass.getMetaClass().hasProperty(testInstanceOrClass, "doWithConfig")
        boolean atClassLevel=(testInstanceOrClass instanceof Class) as boolean
        if(doWithConfigProperty != null && (!atClassLevel || Modifier.isStatic(doWithConfigProperty.getModifiers()))) {
            configClosure=doWithConfigProperty.getProperty(testInstanceOrClass)
        } else {
            MetaMethod doWithConfigMetaMethod=testInstanceOrClass.getMetaClass().respondsTo(testInstanceOrClass, "doWithConfig")?.find{it}
            if(doWithConfigMetaMethod && (!atClassLevel || doWithConfigMetaMethod.isStatic())) {
                configClosure={config -> testInstanceOrClass.doWithConfig(config) }
            }
        }
        if(configClosure) {
            configClosure(grailsApplication.config)
            // reset flatConfig
            grailsApplication.configChanged() 
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
            resetGrailsApplication(runtime)
            
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
            
            Holders.clear()
            ConfigurationHelper.clearCachedConfigs()
        }
    }
    
    public void onTestEvent(TestEvent event) {
        TestRuntime runtime = event.runtime
        switch(event.name) {
            case 'requestFreshRuntime':
            case 'closeRuntime':
                shutdownApplicationContext(runtime)
                break
            case 'before':
                // trigger grailsApplication initialization by requesting value
                runtime.getValue("grailsApplication", event.arguments)
                break
            case 'after':
                resetGrailsApplication(runtime)
                break
            case 'grailsApplicationRequested':
                initialState()
                initGrailsApplication(runtime, (Map)event.arguments.callerInfo)
                break
            case 'valueMissing':
                if(event.arguments.name=='grailsApplication') {
                    runtime.publishEvent('grailsApplicationRequested', [callerInfo: event.arguments.callerInfo])
                }
                break
            case 'defineBeans':
                defineBeans(runtime, (Closure)event.arguments.closure)
                break
            case 'mockCodec':   
                mockCodec(runtime, (Class)event.arguments.codecClass)
                break
            case 'mockForConstraintsTests':   
                mockForConstraintsTests(runtime, (Class)event.arguments.clazz, (List)event.arguments.instances)
                break
        }
    }
    
    
    public void close(TestRuntime runtime) {
        
    }
}
