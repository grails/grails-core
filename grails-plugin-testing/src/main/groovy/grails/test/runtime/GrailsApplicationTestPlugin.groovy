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

package grails.test.runtime

import grails.async.Promises
import grails.boot.config.GrailsApplicationPostProcessor
import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.core.GrailsApplicationLifeCycle
import grails.plugins.GrailsPluginManager
import grails.spring.BeanBuilder
import grails.util.Holders
import grails.util.Metadata
import grails.validation.DeferredBindingActions
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.async.factory.SynchronousPromiseFactory
import org.grails.commons.CodecArtefactHandler
import org.grails.commons.DefaultGrailsCodecClass
import org.grails.core.lifecycle.ShutdownOperations
import org.grails.plugins.IncludingPluginFilter
import org.grails.spring.RuntimeSpringConfiguration
import org.grails.web.context.ServletEnvironmentGrailsApplicationDiscoveryStrategy
import org.grails.web.servlet.context.GrailsConfigUtils
import org.springframework.beans.CachedIntrospectionResults
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigUtils
import org.springframework.util.ClassUtils

import java.lang.reflect.Modifier

/**
 * A TestPlugin for TestRuntime that builds the GrailsApplication instance for tests
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
class GrailsApplicationTestPlugin implements TestPlugin {
    protected static final boolean isServletApiPresent  = ClassUtils.isPresent("javax.servlet.ServletContext", GrailsApplicationTestPlugin.classLoader)

    String[] requiredFeatures = ['metaClassCleaner']
    String[] providedFeatures = ['grailsApplication']
    int ordinal = 0
    List<Closure> queuedBeanClosures = null

    protected void startQueuingDefineBeans(TestRuntime runtime) {
        queuedBeanClosures = []
    }
    
    protected void finishQueuingDefineBeans(TestRuntime runtime, RuntimeSpringConfiguration targetSpringConfig) {
        defineBeans(runtime, queuedBeanClosures, targetSpringConfig)
        queuedBeanClosures = null
    }
    
    void initGrailsApplication(final TestRuntime runtime, final Map callerInfo) {
        Object servletContext = createServletContext(runtime, callerInfo)
        runtime.putValue("servletContext", servletContext)

        ConfigurableApplicationContext mainContext = createMainContext(runtime, callerInfo, servletContext)

        GrailsApplication grailsApplication = (GrailsApplication)runtime.getValueIfExists("grailsApplication")

        if(isServletApiPresent && servletContext != null) {
            configureServletEnvironment(servletContext, grailsApplication, mainContext)
        }

        if(!grailsApplication.isInitialised()) {
            grailsApplication.initialise()
        }

        applicationInitialized(runtime, grailsApplication)
    }

    @CompileDynamic
    protected void configureServletEnvironment(servletContext, GrailsApplication grailsApplication, ConfigurableApplicationContext mainContext) {
        Holders.setServletContext(servletContext);
        Holders.addApplicationDiscoveryStrategy(new ServletEnvironmentGrailsApplicationDiscoveryStrategy(servletContext));
        GrailsConfigUtils.configureServletContextAttributes(servletContext, grailsApplication, mainContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager), mainContext)
    }

    protected ConfigurableApplicationContext createMainContext(final TestRuntime runtime, final Map callerInfo, final servletContext) {
        ConfigurableApplicationContext context

        if(isServletApiPresent && servletContext != null) {
            context = (ConfigurableApplicationContext)ClassUtils.forName("org.springframework.web.context.support.GenericWebApplicationContext").newInstance( servletContext);
        }
        else {
            context = (ConfigurableApplicationContext)ClassUtils.forName("org.springframework.context.support.GenericApplicationContext").newInstance();
        }


        ConfigurableBeanFactory beanFactory = context.getBeanFactory()

        prepareContext(context, beanFactory, runtime, callerInfo);
        customizeContext(context, beanFactory, runtime, callerInfo);
        context.refresh();
        context.registerShutdownHook();

        return context
    }

    protected void prepareContext(ConfigurableApplicationContext applicationContext, ConfigurableBeanFactory beanFactory, TestRuntime runtime, Map callerInfo) {
        registerGrailsAppPostProcessorBean(applicationContext, beanFactory, runtime, callerInfo)

        AnnotationConfigUtils.registerAnnotationConfigProcessors((BeanDefinitionRegistry)beanFactory);

        ConfigFileApplicationContextInitializer contextInitializer = new ConfigFileApplicationContextInitializer();
        contextInitializer.initialize(applicationContext);
    }

    @CompileDynamic
    protected void registerGrailsAppPostProcessorBean(ConfigurableApplicationContext applicationContext, ConfigurableBeanFactory beanFactory, TestRuntime runtime, Map callerInfo) {
        Closure doWithSpringClosure = {
            GrailsApplication grailsApplication = (GrailsApplication)runtime.getValueIfExists("grailsApplication")

            startQueuingDefineBeans(runtime)
            registerBeans(runtime, grailsApplication)
            finishQueuingDefineBeans(runtime, springConfig)

            startQueuingDefineBeans(runtime)
            executeDoWithSpringCallback(runtime, callerInfo)
            finishQueuingDefineBeans(runtime, springConfig)
        }

        Closure customizeGrailsApplicationClosure = { grailsApplication ->
            customizeGrailsApplication(grailsApplication, runtime, callerInfo)
            runtime.putValue("grailsApplication", grailsApplication)
        }

        RootBeanDefinition beandef = new RootBeanDefinition(TestRuntimeGrailsApplicationPostProcessor.class);
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues()
        constructorArgumentValues.addIndexedArgumentValue(0, doWithSpringClosure)
        constructorArgumentValues.addIndexedArgumentValue(1, (resolveTestCallback(callerInfo, "includePlugins") ?: TestRuntimeGrailsApplicationPostProcessor.DEFAULT_INCLUDED_PLUGINS) as Set )
        beandef.setConstructorArgumentValues(constructorArgumentValues)
        beandef.setPropertyValues(new MutablePropertyValues().add("loadExternalBeans", resolveTestCallback(callerInfo, "loadExternalBeans") as boolean).add("customizeGrailsApplicationClosure", customizeGrailsApplicationClosure))
        beandef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        beanFactory.registerBeanDefinition("grailsApplicationPostProcessor", beandef);
    }

    protected void customizeContext(ConfigurableApplicationContext applicationContext, ConfigurableBeanFactory beanFactory, TestRuntime runtime, Map callerInfo) {

    }

    protected Object createServletContext(final TestRuntime runtime, final Map callerInfo) {
        if(isServletApiPresent) {
            return ClassUtils.forName("org.springframework.mock.web.MockServletContext").newInstance()
        }
    }

    protected void customizeGrailsApplication(final GrailsApplication grailsApplication, final TestRuntime runtime, final Map callerInfo) {
        if(!grailsApplication.metadata[Metadata.APPLICATION_NAME]) {
            grailsApplication.metadata[Metadata.APPLICATION_NAME] = "GrailsUnitTestMixin"
        }
        executeDoWithConfigCallback(runtime, grailsApplication, callerInfo)
    }

    protected ClassLoader resolveClassLoader() {
        Thread.currentThread().getContextClassLoader()
    }

    void initialState() {
        Holders.clear()
        CachedIntrospectionResults.clearClassLoader(this.getClass().classLoader)
        CachedIntrospectionResults.clearClassLoader(resolveClassLoader())
        Promises.promiseFactory = new SynchronousPromiseFactory()
    }
    
    void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        runtime.publishEvent("registerBeans", [grailsApplication: grailsApplication], [immediateDelivery: true])
    }

    void executeDoWithSpringCallback(TestRuntime runtime, Map callerInfo) {
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
        }
        Holders.config = grailsApplication.config
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

    void applicationInitialized(TestRuntime runtime, GrailsApplication grailsApplication) {
        runtime.publishEvent("applicationInitialized", [grailsApplication: grailsApplication])
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
    
    void defineBeans(TestRuntime runtime, List<Closure> callables, RuntimeSpringConfiguration targetSpringConfig = null) {
        if(!callables) return
        def binding = new Binding()
        DefaultGrailsApplication grailsApplication = (DefaultGrailsApplication)runtime.getValue("grailsApplication")
        def bb = new BeanBuilder(null, targetSpringConfig, grailsApplication.getClassLoader())
        binding.setVariable "application", grailsApplication
        bb.setBinding binding
        for(Closure callable : callables) {
            bb.beans(callable)
        }
        if (targetSpringConfig == null) {
            bb.registerBeans((BeanDefinitionRegistry)grailsApplication.getMainContext())
        }
    }
    
    void resetGrailsApplication(TestRuntime runtime) {
        if(runtime.containsValueFor('grailsApplication')) {
            ((DefaultGrailsApplication)runtime.getValue('grailsApplication'))?.clear()
        }
        runtime.removeValue("loadedCodecs")
        if(ClassUtils.isPresent("org.grails.web.converters.configuration.ConvertersConfigurationHolder", getClass().classLoader)) {
            clearConvertersHolder()
        }
    }

    @CompileDynamic
    void clearConvertersHolder() {
        ClassUtils.forName("org.grails.web.converters.configuration.ConvertersConfigurationHolder", getClass().classLoader).clear()
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

            if(isServletApiPresent) {
                Holders.setServletContext null
            }
            runtime.removeValue("servletContext")
            
            Promises.promiseFactory = null
            
            Holders.clear()
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
            case 'resetGrailsApplication':
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
            case 'mockCodec':   
                mockCodec(runtime, (Class)event.arguments.codecClass)
                break
        }
    }
    
    
    public void close(TestRuntime runtime) {
        
    }

    static class TestRuntimeGrailsApplicationPostProcessor extends GrailsApplicationPostProcessor {
        static final Set DEFAULT_INCLUDED_PLUGINS = ['core', 'eventBus'] as Set
        Closure customizeGrailsApplicationClosure
        Set includedPlugins = DEFAULT_INCLUDED_PLUGINS

        TestRuntimeGrailsApplicationPostProcessor(Closure doWithSpringClosure, Set includedPlugins = DEFAULT_INCLUDED_PLUGINS) {
            super([doWithSpring: { -> doWithSpringClosure }] as GrailsApplicationLifeCycle, null, null)
            loadExternalBeans = false
            reloadingEnabled = false
            this.includedPlugins = includedPlugins
        }

        @Override
        protected void customizePluginManager(GrailsPluginManager grailsApplication) {
            pluginManager.pluginFilter = new IncludingPluginFilter(includedPlugins)
        }

        @Override
        protected void customizeGrailsApplication(GrailsApplication grailsApplication) {
            customizeGrailsApplicationClosure?.call(grailsApplication)
        }
    }
}

