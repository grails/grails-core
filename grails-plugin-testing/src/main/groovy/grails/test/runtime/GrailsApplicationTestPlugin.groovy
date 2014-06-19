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
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.lang.reflect.Modifier

import javax.servlet.ServletContext

import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.CodecArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper
import grails.web.servlet.context.GrailsWebApplicationContext
import org.codehaus.groovy.grails.commons.spring.OptimizedAutowireCapableBeanFactory;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration;
import org.grails.core.lifecycle.ShutdownOperations
import org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport
import org.grails.validation.ConstraintEvalUtils
import org.grails.web.servlet.context.GrailsConfigUtils;
import org.grails.web.context.ServletEnvironmentGrailsApplicationDiscoveryStrategy
import org.grails.async.factory.SynchronousPromiseFactory
import org.springframework.beans.CachedIntrospectionResults
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.mock.web.MockServletContext

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
    List<Closure> queuedBeanClosures = null
    List<Closure> queuedParentBeanClosures = null

    protected void startQueuingDefineBeans(TestRuntime runtime, boolean parent=false) {
        if(!parent) {
            queuedBeanClosures = []
        } else {
            queuedParentBeanClosures = []
        }
    }
    
    protected void finishQueuingDefineBeans(TestRuntime runtime, RuntimeSpringConfiguration targetSpringConfig, boolean parent=false) {
        defineBeans(runtime, parent == false ? queuedBeanClosures : queuedParentBeanClosures, targetSpringConfig, parent)
        if(!parent) {
            queuedBeanClosures = null
        } else {
            queuedParentBeanClosures = null
        }
    }
    
    void initGrailsApplication(final TestRuntime runtime, final Map callerInfo) {
        ServletContext servletContext = createServletContext(runtime, callerInfo)
        runtime.putValue("servletContext", servletContext)
        
        DefaultGrailsApplication grailsApplication = createGrailsApplication(runtime, callerInfo)
        runtime.putValue("grailsApplication", grailsApplication)
        addGrailsApplicationHolder(grailsApplication)
        
        GrailsWebApplicationContext parentContext = createParentContext(runtime, callerInfo, grailsApplication, servletContext)
                
        GrailsWebApplicationContext mainContext = createMainContext(runtime, callerInfo, grailsApplication, servletContext)

        applicationInitialized(runtime, grailsApplication)
    }

    protected addGrailsApplicationHolder(DefaultGrailsApplication grailsApplication) {
        Holders.setGrailsApplication(grailsApplication)
        Holders.config = grailsApplication.config
    }

    protected GrailsWebApplicationContext createMainContext(final TestRuntime runtime, final Map callerInfo, final GrailsApplication grailsApplication, final ServletContext servletContext) {
        GrailsTestRuntimeConfigurator runtimeConfigurator = new GrailsTestRuntimeConfigurator(grailsApplication, grailsApplication.getParentContext()) {
                    protected void initializeContext(ApplicationContext mainContext) {
                        startQueuingDefineBeans(runtime)
                        registerBeans(runtime, grailsApplication)
                        finishQueuingDefineBeans(runtime, webSpringConfig)

                        startQueuingDefineBeans(runtime)
                        executeDoWithSpringCallback(runtime, grailsApplication, callerInfo)
                        finishQueuingDefineBeans(runtime, webSpringConfig)

                        super.initializeContext(mainContext)
                    }
                }
        boolean loadExternalBeans = resolveTestCallback(callerInfo, "loadExternalBeans")
        
        GrailsWebApplicationContext mainContext = (GrailsWebApplicationContext)runtimeConfigurator.configure(servletContext, loadExternalBeans)
        
        if(servletContext != null) {
            Holders.setServletContext(servletContext);
            Holders.addApplicationDiscoveryStrategy(new ServletEnvironmentGrailsApplicationDiscoveryStrategy(servletContext));
            GrailsConfigUtils.configureServletContextAttributes(servletContext, grailsApplication, runtimeConfigurator.getPluginManager(), mainContext)
        }
        
        return mainContext
    }

    protected ServletContext createServletContext(final TestRuntime runtime, final Map callerInfo) {
        MockServletContext servletContext = new MockServletContext()
        return servletContext
    }

    protected GrailsWebApplicationContext createParentContext(final TestRuntime runtime, final Map callerInfo, GrailsApplication grailsApplication, ServletContext servletContext) {
        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(null, grailsApplication.getClassLoader());
        springConfig.setBeanFactory(new OptimizedAutowireCapableBeanFactory());
        grailsApplication.setApplicationContext(springConfig.getUnrefreshedApplicationContext());
        createParentBeans(runtime, springConfig, grailsApplication);
        GrailsWebApplicationContext parentContext = (GrailsWebApplicationContext)springConfig.getApplicationContext();
        if(servletContext != null) {
            parentContext.servletContext = servletContext
        }
        return parentContext
    }
    
    protected void createParentBeans(final TestRuntime runtime, RuntimeSpringConfiguration targetSpringConfig, GrailsApplication grailsApplication) {
        startQueuingDefineBeans(runtime, true)
        registerParentBeans(runtime, grailsApplication)
        finishQueuingDefineBeans(runtime, targetSpringConfig, true)
    }

    protected DefaultGrailsApplication createGrailsApplication(TestRuntime runtime, Map callerInfo) {
        def classLoader = resolveClassLoader()
        DefaultGrailsApplication grailsApplication = new DefaultGrailsApplication(classLoader)
        if(!grailsApplication.metadata[Metadata.APPLICATION_NAME]) {
            grailsApplication.metadata[Metadata.APPLICATION_NAME] = "GrailsUnitTestMixin"
        }
        executeDoWithConfigCallback(runtime, grailsApplication, callerInfo)
        grailsApplication.initialise()
        return grailsApplication
    }

    protected ClassLoader resolveClassLoader() {
        Thread.currentThread().getContextClassLoader()
    }

    void initialState() {
        ExpandoMetaClass.enableGlobally()
        Holders.clear()
        ClassPropertyFetcher.clearClassPropertyFetcherCache()
        CachedIntrospectionResults.clearClassLoader(this.getClass().classLoader)
        CachedIntrospectionResults.clearClassLoader(resolveClassLoader())
        Promises.promiseFactory = new SynchronousPromiseFactory()
    }
    
    void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        runtime.publishEvent("registerBeans", [grailsApplication: grailsApplication], [immediateDelivery: true])
    }

    void registerParentBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        runtime.publishEvent("registerParentBeans", [grailsApplication: grailsApplication], [immediateDelivery: true])
    }
        
    void executeDoWithSpringCallback(TestRuntime runtime, GrailsApplication grailsApplication, Map callerInfo) {
        def doWithSpringClosure = resolveTestCallback(callerInfo, "doWithSpring", null)
        if(doWithSpringClosure) {
            runtime.publishEvent("defineBeans", [closure: doWithSpringClosure], [immediateDelivery: true])
        }
    }
    
    void executeDoWithConfigCallback(TestRuntime runtime, GrailsApplication grailsApplication, Map callerInfo) {
        Closure configClosure = (Closure)resolveTestCallback(callerInfo, "doWithConfig", "doWithConfig")
        if(configClosure) {
            configClosure(grailsApplication.config)
            // reset flatConfig
            grailsApplication.configChanged() 
            Holders.config = grailsApplication.config
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Object resolveTestCallback(Map callerInfo, String propertyName, String methodName = null) {
        def testInstanceOrClass=callerInfo.testInstance ?: callerInfo.testClass
        def value
        MetaProperty metaProperty=testInstanceOrClass.getMetaClass().hasProperty(testInstanceOrClass, propertyName)
        boolean atClassLevel=(testInstanceOrClass instanceof Class) as boolean
        if(metaProperty != null && (!atClassLevel || Modifier.isStatic(metaProperty.getModifiers()))) {
            value=metaProperty.getProperty(testInstanceOrClass)
        } else if (methodName != null) {
            MetaMethod metaMethod=testInstanceOrClass.getMetaClass().respondsTo(testInstanceOrClass, methodName)?.find{it}
            if(metaMethod && (!atClassLevel || metaMethod.isStatic())) {
                value={ Object[] params -> metaMethod.doMethodInvoke(testInstanceOrClass, params) }
            }
        }
        return value
    }

    void applicationInitialized(TestRuntime runtime, DefaultGrailsApplication grailsApplication) {
        runtime.publishEvent("applicationInitialized", [grailsApplication: grailsApplication])
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

    void defineBeans(TestRuntime runtime, List<Closure> callables, RuntimeSpringConfiguration targetSpringConfig = null, boolean parent = false) {
        if(!callables) return
        def binding = new Binding()
        DefaultGrailsApplication grailsApplication = (DefaultGrailsApplication)runtime.getValue("grailsApplication")
        def bb = new BeanBuilder(parent==false ? grailsApplication.getParentContext() : null, targetSpringConfig, grailsApplication.getClassLoader())
        binding.setVariable "application", grailsApplication
        bb.setBinding binding
        for(Closure callable : callables) {
            bb.beans(callable)
        }
        if (targetSpringConfig == null) {
            bb.registerBeans((BeanDefinitionRegistry)((parent==false) ? grailsApplication.getMainContext() : grailsApplication.getParentContext()))
        }
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
                def beansClosure = (Closure)event.arguments.closure
                if(queuedBeanClosures != null) {
                     queuedBeanClosures << beansClosure
                } else {
                    defineBeans(runtime, [beansClosure])
                }
                break
            case 'defineParentBeans':
                def beansClosure = (Closure)event.arguments.closure
                if(queuedParentBeanClosures != null) {
                     queuedParentBeanClosures << beansClosure
                } else {
                    defineBeans(runtime, [beansClosure], null, true)
                }
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
