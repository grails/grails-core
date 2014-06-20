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

import grails.test.mixin.support.GroovyPageUnitTestResourceLoader
import grails.test.mixin.support.LazyTagLibraryLookup
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import grails.web.HyphenatedUrlConverter
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import javax.servlet.ServletContext

import grails.core.GrailsApplication
import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin
import org.codehaus.groovy.grails.plugins.codecs.DefaultCodecLookup
import org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin
import org.codehaus.groovy.grails.plugins.converters.ConvertersPluginSupport
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.grails.plugins.web.ServletsGrailsPluginSupport;
import org.grails.plugins.web.api.ControllersMimeTypesApi
import org.grails.plugins.web.api.RequestMimeTypesApi
import org.grails.plugins.web.api.ResponseMimeTypesApi
import org.grails.plugins.web.mime.MimeTypesFactoryBean
import org.grails.plugins.web.mime.MimeTypesGrailsPlugin
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.UrlMappingsHolderFactoryBean
import grails.web.mime.MimeType
import org.grails.web.pages.FilteringCodecsByContentTypeSettings
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateRenderer
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolverImpl
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.plugins.web.rest.api.ControllersRestApi
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.springframework.context.ApplicationContext
import org.springframework.util.ClassUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.multipart.commons.CommonsMultipartResolver

/**
 * Controller TestPlugin for TestRuntime
 * - adds beans and state management for supporting controller related unit tests
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
class ControllerTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication', 'coreBeans']
    String[] providedFeatures = ['controller']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        Map<String, String> groovyPages = [:]
        runtime.putValue("groovyPages", groovyPages)
        
        defineBeans(runtime, new MimeTypesGrailsPlugin().doWithSpring)
        defineBeans(runtime, new ConvertersGrailsPlugin().doWithSpring)
        def config = grailsApplication.config
        defineBeans(runtime) {
            instanceControllersApi(ControllersApi)
            rendererRegistry(DefaultRendererRegistry) {
                modelSuffix = config.flatten().get('grails.scaffolding.templates.domainSuffix') ?: ''
            }
            instanceControllersRestApi(ControllersRestApi, ref("rendererRegistry"), ref("instanceControllersApi"), new ControllersMimeTypesApi())
            instanceControllerTagLibraryApi(ControllerTagLibraryApi)

            def urlConverterType = config?.grails?.web?.url?.converter
            "${grails.web.UrlConverter.BEAN_NAME}"('hyphenated' == urlConverterType ? HyphenatedUrlConverter : CamelCaseUrlConverter)

            grailsLinkGenerator(DefaultLinkGenerator, config?.grails?.serverURL ?: "http://localhost:8080")

            final classLoader = ControllerUnitTestMixin.class.getClassLoader()
            if (ClassUtils.isPresent("UrlMappings", classLoader)) {
                grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, classLoader.loadClass("UrlMappings"))
            }
            multipartResolver(CommonsMultipartResolver)
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) {
                grailsApplication = grailsApplication
            }

            def lazyBean = { bean ->
                bean.lazyInit = true
            }
            jspTagLibraryResolver(TagLibraryResolverImpl, lazyBean)
            gspTagLibraryLookup(LazyTagLibraryLookup, lazyBean)
            groovyPageLocator(GrailsConventionGroovyPageLocator) {
                resourceLoader = new GroovyPageUnitTestResourceLoader(groovyPages)
            }
            groovyPagesTemplateEngine(GroovyPagesTemplateEngine) { bean ->
                bean.lazyInit = true
                tagLibraryLookup = ref("gspTagLibraryLookup")
                jspTagLibraryResolver = ref("jspTagLibraryResolver")
                groovyPageLocator = ref("groovyPageLocator")
            }

            groovyPagesTemplateRenderer(GroovyPagesTemplateRenderer) { bean ->
                bean.lazyInit = true
                groovyPageLocator = ref("groovyPageLocator")
                groovyPagesTemplateEngine = ref("groovyPagesTemplateEngine")
            }

            filteringCodecsByContentTypeSettings(FilteringCodecsByContentTypeSettings, grailsApplication)
        }
        defineBeans(runtime, new CodecsGrailsPlugin().doWithSpring)
    }
    
    protected void applicationInitialized(TestRuntime runtime, GrailsApplication grailsApplication) {
        mockDefaultCodecs(runtime)
        grailsApplication.mainContext.getBean(DefaultCodecLookup).reInitialize()
        ((ConvertersConfigurationInitializer)grailsApplication.mainContext.getBean("convertersConfigurationInitializer")).initialize(grailsApplication)
        ConvertersPluginSupport.enhanceApplication(grailsApplication, grailsApplication.mainContext)
        ServletsGrailsPluginSupport.enhanceServletApi(grailsApplication.config)
    }

    protected void mockDefaultCodecs(TestRuntime runtime) {
        new CodecsGrailsPlugin().providedArtefacts.each { Class codecClass ->
            mockCodec(runtime, codecClass)
        }
    }
    
    protected void mockCodec(TestRuntime runtime, Class codecClass) {
        runtime.publishEvent("mockCodec", [codecClass: codecClass], [immediateDelivery: true])
    }
    
    protected void bindGrailsWebRequest(TestRuntime runtime, GrailsApplication grailsApplication) {
        if(runtime.getValueIfExists("codecsChanged")) {
            grailsApplication.mainContext.getBean(DefaultCodecLookup).reInitialize()
            runtime.putValue("codecsChanged", false)
        }

        def applicationContext = grailsApplication.mainContext
        GrailsMockHttpServletRequest request = new GrailsMockHttpServletRequest((ServletContext)runtime.getValue("servletContext"))
        request.method = 'GET'
        request.requestMimeTypesApi = new TestRequestMimeTypesApi(grailsApplication: grailsApplication, applicationContext: applicationContext)
        GrailsMockHttpServletResponse response = new GrailsMockHttpServletResponse(responseMimeTypesApi: new TestResponseMimeTypesApi(grailsApplication: grailsApplication, applicationContext: applicationContext))
        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest(applicationContext, request, response)
        runtime.putValue("webRequest", webRequest)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void clearGrailsWebRequest(TestRuntime runtime) {
        RequestContextHolder.setRequestAttributes(null)
        runtime.getValue("groovyPages")?.clear()
        GrailsWebRequest webRequest = runtime.getValueIfExists("webRequest")
        def ctx = webRequest?.applicationContext
        if (ctx?.containsBean("groovyPagesTemplateEngine")) {
            ctx.groovyPagesTemplateEngine.clearPageCache()
        }
        if (ctx?.containsBean("groovyPagesTemplateRenderer")) {
            ctx.groovyPagesTemplateRenderer.clearCache()
        }
        runtime.removeValue("webRequest")
    }
    
    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }
    
    GrailsApplication getGrailsApplication(TestEvent event) {
        (GrailsApplication)event.runtime.getValue("grailsApplication")
    }

    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'before':
                bindGrailsWebRequest(event.runtime, getGrailsApplication(event))
                break
            case 'after':
                clearGrailsWebRequest(event.runtime)
                break
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'applicationInitialized':
                applicationInitialized(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'mockCodec':
                event.runtime.putValue("codecsChanged", true)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        
    }
}

@CompileStatic
class TestResponseMimeTypesApi extends ResponseMimeTypesApi {

    ApplicationContext applicationContext

    @Override
    MimeType[] getMimeTypes() {
        loadConfig()
        def factory = new MimeTypesFactoryBean()
        factory.applicationContext = applicationContext
        return factory.getObject()
    }
    
    
}

@CompileStatic
class TestRequestMimeTypesApi extends RequestMimeTypesApi {

    ApplicationContext applicationContext

    @Override
    MimeType[] getMimeTypes() {
        def factory = new MimeTypesFactoryBean()
        factory.applicationContext = applicationContext
        return factory.getObject()
    }
}

