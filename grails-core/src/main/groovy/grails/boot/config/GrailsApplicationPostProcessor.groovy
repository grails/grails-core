package grails.boot.config

import grails.boot.GrailsApp
import grails.config.Settings
import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.core.GrailsApplicationLifeCycle
import grails.plugins.DefaultGrailsPluginManager
import grails.plugins.GrailsPlugin
import grails.plugins.GrailsPluginManager
import grails.spring.BeanBuilder
import grails.util.Environment
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.grails.config.NavigableMap
import org.grails.config.PrefixedMapPropertySource
import org.grails.config.PropertySourcesConfig
import org.grails.core.exceptions.GrailsConfigurationException
import org.grails.core.lifecycle.ShutdownOperations
import org.grails.dev.support.GrailsSpringLoadedPlugin
import org.grails.spring.DefaultRuntimeSpringConfiguration
import org.grails.spring.RuntimeSpringConfigUtilities
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ApplicationContextEvent
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.AbstractEnvironment
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.util.ClassUtils

/**
 * A {@link BeanDefinitionRegistryPostProcessor} that enhances any ApplicationContext with plugin manager capabilities
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@Commons
class GrailsApplicationPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware, ApplicationListener<ApplicationContextEvent> {
    static final boolean RELOADING_ENABLED = Environment.getCurrent().isReloadEnabled() && ClassUtils.isPresent("org.springsource.loaded.SpringLoaded", Thread.currentThread().contextClassLoader)

    final GrailsApplication grailsApplication
    final GrailsApplicationLifeCycle lifeCycle
    protected GrailsPluginManager pluginManager
    protected ApplicationContext applicationContext
    boolean loadExternalBeans = true
    boolean reloadingEnabled = RELOADING_ENABLED

    GrailsApplicationPostProcessor() {
        this(null, null, [] as Class[])
    }

    GrailsApplicationPostProcessor(Class...classes) {
        this(null, classes)
    }

    GrailsApplicationPostProcessor(ApplicationContext applicationContext, Class...classes) {
        this(null, applicationContext, classes)
    }

    GrailsApplicationPostProcessor(GrailsApplicationLifeCycle lifeCycle, ApplicationContext applicationContext, Class...classes) {
        this.lifeCycle = lifeCycle
        grailsApplication = new DefaultGrailsApplication((classes?:[]) as Class[])
        pluginManager = new DefaultGrailsPluginManager(grailsApplication)
        if(applicationContext != null) {
            initializeGrailsApplication(applicationContext)
        }
    }

    protected final void initializeGrailsApplication(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
        grailsApplication.applicationContext = applicationContext
        grailsApplication.mainContext = applicationContext
        customizePluginManager(pluginManager)
        pluginManager.loadPlugins()
        pluginManager.applicationContext = applicationContext
        loadApplicationConfig()
        customizeGrailsApplication(grailsApplication)
        performGrailsInitializationSequence()
    }

    protected void customizePluginManager(GrailsPluginManager grailsApplication) {

    }

    protected void customizeGrailsApplication(GrailsApplication grailsApplication) {

    }

    protected void performGrailsInitializationSequence() {
        pluginManager.doArtefactConfiguration()
        grailsApplication.initialise()
        pluginManager.registerProvidedArtefacts(grailsApplication)
    }

    protected void loadApplicationConfig() {
        org.springframework.core.env.Environment environment = applicationContext.getEnvironment()
        if(environment instanceof ConfigurableEnvironment) {
            if(environment instanceof AbstractEnvironment) {
                def cs = environment.getConversionService()
                cs.addConverter(new Converter<NavigableMap.NullSafeNavigator, String>() {
                    @Override
                    public String convert(NavigableMap.NullSafeNavigator source) {
                        return null;
                    }
                });
            }
            def propertySources = environment.getPropertySources()
            def plugins = pluginManager.allPlugins
            if(plugins) {
                for(GrailsPlugin plugin in plugins.reverse()) {
                    def pluginPropertySource = plugin.propertySource
                    if(pluginPropertySource) {
                        if(pluginPropertySource instanceof EnumerablePropertySource) {
                            propertySources.addLast( new PrefixedMapPropertySource( "grails.plugins.$plugin.name", (EnumerablePropertySource)pluginPropertySource ) )
                        }
                        propertySources.addLast pluginPropertySource
                    }
                }
            }
            ((DefaultGrailsApplication)grailsApplication).config = new PropertySourcesConfig(propertySources)
        }
    }

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        def springConfig = new DefaultRuntimeSpringConfiguration()


        def application = grailsApplication
        Holders.setGrailsApplication(application)

        // first register plugin beans
        pluginManager.doRuntimeConfiguration(springConfig)

        if(loadExternalBeans) {
            // now allow overriding via application

            def context = application.mainContext
            def beanResources = context.getResource(RuntimeSpringConfigUtilities.SPRING_RESOURCES_GROOVY)
            if (beanResources?.exists()) {
                def gcl = new GroovyClassLoader(application.classLoader)
                try {
                    RuntimeSpringConfigUtilities.reloadSpringResourcesConfig(springConfig, application, gcl.parseClass(new GroovyCodeSource(beanResources.URL)))
                } catch (Throwable e) {
                    log.error("Error loading spring/resources.groovy file: ${e.message}", e)
                    throw new GrailsConfigurationException("Error loading spring/resources.groovy file: ${e.message}", e)
                }
            }

            beanResources = context.getResource(RuntimeSpringConfigUtilities.SPRING_RESOURCES_XML)
            if (beanResources?.exists()) {
                try {
                    new BeanBuilder(null, springConfig, application.classLoader)
                            .importBeans(beanResources)
                } catch (Throwable e) {
                    log.error("Error loading spring/resources.xml file: ${e.message}", e)
                    throw new GrailsConfigurationException("Error loading spring/resources.xml file: ${e.message}", e)
                }
            }
        }

        if(lifeCycle) {
            def withSpring = lifeCycle.doWithSpring()
            if(withSpring) {
                def bb = new BeanBuilder(null, springConfig, application.classLoader)
                bb.beans withSpring
            }
        }

        springConfig.registerBeansWithRegistry(registry)
    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, grailsApplication)
        beanFactory.registerSingleton(GrailsPluginManager.BEAN_NAME, pluginManager)

        if(reloadingEnabled) {
            GrailsSpringLoadedPlugin.register(pluginManager)
        }
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(this.applicationContext != applicationContext) {
            initializeGrailsApplication(applicationContext)
        }

        if(applicationContext instanceof ConfigurableApplicationContext) {
            def configurable = (ConfigurableApplicationContext) applicationContext
            configurable.addApplicationListener(this)
            configurable.environment.addActiveProfile( grailsApplication.getConfig().getProperty(Settings.PROFILE, String, "web"))
        }
    }

    @Override
    void onApplicationEvent(ApplicationContextEvent event) {
        def context = event.applicationContext

        def lifeCycleBeans = context.getBeansOfType(GrailsApplicationLifeCycle).values()
        if(event instanceof ContextRefreshedEvent) {
            pluginManager.setApplicationContext(context)
            pluginManager.doDynamicMethods()
            for(GrailsApplicationLifeCycle lifeCycle in lifeCycleBeans) {
                lifeCycle.doWithDynamicMethods()
            }
            pluginManager.doPostProcessing(context)
            for(GrailsApplicationLifeCycle lifeCycle in lifeCycleBeans) {
                lifeCycle.doWithApplicationContext()
            }
            Holders.pluginManager = pluginManager
            Map<String,Object> eventMap = [:]
            eventMap.put('source', pluginManager)

            for(GrailsApplicationLifeCycle lifeCycle in lifeCycleBeans) {
                lifeCycle.onStartup(eventMap)
            }
        }
        else if(event instanceof ContextClosedEvent) {
            pluginManager.shutdown()
            Map<String,Object> eventMap = [:]
            eventMap.put('source', pluginManager)
            for(GrailsApplicationLifeCycle lifeCycle in lifeCycleBeans) {
                lifeCycle.onShutdown(eventMap)
            }
            ShutdownOperations.runOperations()
            Holders.clear()
            if(reloadingEnabled) {
                try {
                    GrailsSpringLoadedPlugin.unregister()
                } catch (Throwable e) {
                    // ignore
                }
            }

            GrailsApp.setDevelopmentModeActive(false)
        }
    }

}
