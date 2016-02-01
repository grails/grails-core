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

import grails.config.Settings
import grails.core.GrailsApplication
import grails.test.mixin.support.GroovyPageUnitTestResourceLoader
import grails.test.mixin.support.LazyTagLibraryLookup
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.util.GrailsWebMockUtil
import grails.web.CamelCaseUrlConverter
import grails.web.HyphenatedUrlConverter
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.servlet.view.GroovyPageViewResolver
import org.grails.web.util.GrailsApplicationAttributes

import javax.servlet.ServletContext

import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.grails.plugins.CodecsGrailsPlugin
import org.grails.plugins.codecs.DefaultCodecLookup
import org.grails.plugins.converters.ConvertersGrailsPlugin
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.plugins.web.mime.MimeTypesGrailsPlugin
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.grails.web.pages.FilteringCodecsByContentTypeSettings
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.web.gsp.GroovyPagesTemplateRenderer
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator
import org.grails.gsp.jsp.TagLibraryResolverImpl
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.util.ClassUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.SessionLocaleResolver

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
        
        defineBeans(runtime, new MimeTypesGrailsPlugin().doWithSpring())
        defineBeans(runtime, new ConvertersGrailsPlugin().doWithSpring)
        def config = grailsApplication.config
        defineBeans(runtime) {
            rendererRegistry(DefaultRendererRegistry) {
                modelSuffix = config.getProperty('grails.scaffolding.templates.domainSuffix', '')
            }
            String urlConverterType = config.getProperty(Settings.WEB_URL_CONVERTER)
            "${grails.web.UrlConverter.BEAN_NAME}"('hyphenated' == urlConverterType ? HyphenatedUrlConverter : CamelCaseUrlConverter)

            grailsLinkGenerator(DefaultLinkGenerator, config?.grails?.serverURL ?: "http://localhost:8080")

            final classLoader = ControllerUnitTestMixin.class.getClassLoader()
            if (ClassUtils.isPresent("UrlMappings", classLoader)) {
                grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, classLoader.loadClass("UrlMappings"))
            }

            def urlMappingsClass = "${config.getProperty('grails.codegen.defaultPackage', 'null')}.UrlMappings"
            if (ClassUtils.isPresent(urlMappingsClass, classLoader)) {
                grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, classLoader.loadClass(urlMappingsClass))
            }

            localeResolver(SessionLocaleResolver)
            multipartResolver(StandardServletMultipartResolver)
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) {
                grailsApplication = grailsApplication
            }

            "${CompositeViewResolver.BEAN_NAME}"(CompositeViewResolver)

            if(ClassUtils.isPresent("org.grails.plugins.web.GroovyPagesGrailsPlugin", classLoader)) {
                def lazyBean = { bean ->
                    bean.lazyInit = true
                }
                jspTagLibraryResolver(TagLibraryResolverImpl, lazyBean)
                gspTagLibraryLookup(LazyTagLibraryLookup, lazyBean)
                groovyPageUnitTestResourceLoader(GroovyPageUnitTestResourceLoader, groovyPages)
                groovyPageLocator(GrailsConventionGroovyPageLocator) {
                    resourceLoader = ref('groovyPageUnitTestResourceLoader')
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

                // Configure a Spring MVC view resolver
                jspViewResolver(GroovyPageViewResolver) { bean ->
                    prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
                    suffix = GroovyPageViewResolver.GSP_SUFFIX
                    templateEngine = groovyPagesTemplateEngine
                    groovyPageLocator = groovyPageLocator
                }
            }
            filteringCodecsByContentTypeSettings(FilteringCodecsByContentTypeSettings, grailsApplication)
            
            localeResolver(SessionLocaleResolver)
        }
        defineBeans(runtime, new CodecsGrailsPlugin().doWithSpring())
    }
    
    protected void applicationInitialized(TestRuntime runtime, GrailsApplication grailsApplication) {
        mockDefaultCodecs(runtime)
        grailsApplication.mainContext.getBean(DefaultCodecLookup).reInitialize()
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
        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, grailsApplication.mainContext.getBean('localeResolver'))
        request.method = 'GET'
        GrailsMockHttpServletResponse response = new GrailsMockHttpServletResponse()
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest(applicationContext, request, response)
        runtime.putValue("webRequest", webRequest)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void clearGrailsWebRequest(TestRuntime runtime) {
        RequestContextHolder.resetRequestAttributes()
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





