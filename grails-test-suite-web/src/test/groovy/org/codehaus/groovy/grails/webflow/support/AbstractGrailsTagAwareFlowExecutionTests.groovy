package org.codehaus.groovy.grails.webflow.support

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication

import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.context.support.StaticMessageSource

import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.webflow.test.execution.AbstractFlowExecutionTests
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockHttpServletResponse
import javax.servlet.ServletContext
import org.springframework.webflow.engine.builder.support.FlowBuilderServices
import org.springframework.webflow.mvc.builder.MvcViewFactoryCreator
import org.springframework.binding.convert.service.DefaultConversionService
import org.springframework.webflow.expression.DefaultExpressionParserFactory
import org.springframework.webflow.definition.registry.FlowDefinitionRegistryImpl
import org.springframework.webflow.context.ExternalContext
import org.springframework.webflow.context.servlet.ServletExternalContext
import org.springframework.webflow.test.MockExternalContext
import org.springframework.webflow.definition.FlowDefinition
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder
import org.springframework.webflow.engine.builder.FlowAssembler
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry
import org.springframework.webflow.engine.builder.DefaultFlowHolder
import org.springframework.webflow.definition.registry.FlowDefinitionLocator

/**
 * @author Graeme Rocher
 * @since 0.4
 */
abstract class AbstractGrailsTagAwareFlowExecutionTests extends AbstractFlowExecutionTests {

    ServletContext servletContext
    GrailsWebRequest webRequest
    FlowBuilderServices flowBuilderServices
    FlowDefinitionRegistry flowDefinitionRegistry = new FlowDefinitionRegistryImpl()
    MockHttpServletRequest request
    MockHttpServletResponse response
    def ctx
    def originalHandler
    ApplicationContext appCtx
    GrailsApplication ga
    def mockManager
    def gcl = new GroovyClassLoader()

    GrailsApplication grailsApplication
    StaticMessageSource messageSource

    final void setUp() throws Exception {
        originalHandler =     GroovySystem.metaClassRegistry.metaClassCreationHandle

        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()

        grailsApplication = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga = grailsApplication
        grailsApplication.initialise()
        mockManager = new MockGrailsPluginManager(grailsApplication)
        mockManager.registerProvidedArtefacts(grailsApplication)
        onInit()

        def mockControllerClass = gcl.parseClass("class MockController {  def index = {} } ")
        ctx = new MockApplicationContext()

        grailsApplication.setApplicationContext(ctx)

        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication)
        grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, mockControllerClass)

        messageSource = new StaticMessageSource()
        ctx.registerMockBean("manager", mockManager)
        ctx.registerMockBean("messageSource", messageSource)
        ctx.registerMockBean("grailsApplication",grailsApplication)
        ctx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())

        def dependantPluginClasses = []
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")

        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, grailsApplication)}

        dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.registerProvidedArtefacts(grailsApplication)
        def springConfig = new WebRuntimeSpringConfiguration(ctx)

        servletContext =  ctx.servletContext

        springConfig.servletContext = servletContext

        dependentPlugins*.doWithRuntimeConfiguration(springConfig)

        appCtx = springConfig.getApplicationContext()
        grailsApplication.mainContext = appCtx

        flowBuilderServices = new FlowBuilderServices()
        MvcViewFactoryCreator viewCreator = new MvcViewFactoryCreator()
        viewCreator.viewResolvers = [appCtx.getBean('jspViewResolver')]
        viewCreator.applicationContext = appCtx
        flowBuilderServices.viewFactoryCreator = viewCreator
        flowBuilderServices.conversionService = new DefaultConversionService()
        flowBuilderServices.expressionParser = DefaultExpressionParserFactory.getExpressionParser()

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        mockManager.applicationContext = appCtx
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        GroovySystem.metaClassRegistry.removeMetaClass(String)
        GroovySystem.metaClassRegistry.removeMetaClass(Object)
       //grailsApplication.tagLibClasses.each { tc -> GroovySystem.metaClassRegistry.removeMetaClass(tc.clazz)}
        mockManager.doDynamicMethods()

        webRequest = GrailsWebUtil.bindMockWebRequest(appCtx)
        request = webRequest.currentRequest
        request.characterEncoding = "utf-8"
        response = webRequest.currentResponse

        assert appCtx.grailsUrlMappingsHolder
    }

    final void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
        GroovySystem.metaClassRegistry.setMetaClassCreationHandle(originalHandler)
        onDestroy()
    }

    protected void startFlow() {
        super.startFlow(new ServletExternalContext(servletContext, request, response))
    }

    protected ExternalContext signalEvent(String eventId) {
        MockExternalContext context = new MockExternalContext()
        context.setNativeRequest request
        context.setNativeResponse response
        context.setNativeContext servletContext
        context.setEventId(eventId)
        resumeFlow(context)
        return context
    }

    FlowDefinition registerFlow(String flowId, Closure flowClosure) {
        FlowBuilder builder = new FlowBuilder(flowId, flowClosure, flowBuilderServices, getFlowDefinitionRegistry())
        builder.viewPath = "/"
        builder.applicationContext = appCtx
        FlowAssembler assembler = new FlowAssembler(builder, builder.getFlowBuilderContext())
        getFlowDefinitionRegistry().registerFlowDefinition(new DefaultFlowHolder(assembler))
        return getFlowDefinitionRegistry().getFlowDefinition(flowId)
    }

    FlowDefinition getFlowDefinition() {
        return registerFlow(getFlowId(), getFlowClosure())
    }

    protected void onInit() {}

    protected void onDestroy() {}

    String getFlowId() { 'testFlow' }

    abstract Closure getFlowClosure()
}
