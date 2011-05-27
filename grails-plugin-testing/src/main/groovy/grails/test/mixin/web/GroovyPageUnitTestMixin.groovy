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

package grails.test.mixin.web

import grails.artefact.Enhanced
import groovy.text.Template
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter
import org.codehaus.groovy.grails.plugins.web.api.TagLibraryApi

/**
 * <p>A unit testing mixing that add behavior to support the testing of tag libraries
 * and GSP pages. Can be used in combination with
 * {@link grails.test.mixin.domain.DomainClassUnitTestMixin} to support the testing of
 * tag libraries and GSPs</p>
 *
 * <p>Views and templates can be mocked via the {@link ControllerUnitTestMixin#groovyPages}
 * map where the keys are the names of the pages and the values are the contents of the GSP page</p>
 *
 * <p>Alternatively, within a Grails project the unit test will fallback to loading views and templates
 * from grails-app/views</p>
 *
 * <p>For mocking tag libraries the {@link GroovyPageUnitTestMixin#mockTagLib(Class) } method allows you
 * to mock custom tag libraries. There is no need to mock core tag libraries like <g:createLink /> as these
 * will be loaded on demand </p>
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class GroovyPageUnitTestMixin extends ControllerUnitTestMixin{

    /**
     * Mocks a tag library, making it available to subsequent calls to controllers mocked via
     * {@link #mockController(Class) } and GSPs rendered via {@link #applyTemplate(String, Map) }
     *
     * @param tagLibClass The tag library class
     * @return The tag library instance
     */

    def mockTagLib(Class tagLibClass) {
        GrailsTagLibClass tagLib = grailsApplication.addArtefact(TagLibArtefactHandler.TYPE, tagLibClass)

        if (tagLibClass.getAnnotation(Enhanced)) {
            defineBeans {
                instanceTagLibraryApi(TagLibraryApi)
            }
        }
        else {
            MetaClassEnhancer enhancer = new MetaClassEnhancer()
            enhancer.addApi(new TagLibraryApi())
            enhancer.enhance(tagLibClass.metaClass)
        }

        defineBeans {

            "${tagLib.fullName}"(tagLibClass) { bean ->
                bean.autowire = true
            }
        }

        final tagLookup = applicationContext.getBean(TagLibraryLookup)
        tagLookup.registerTagLib(tagLib)

        return applicationContext.getBean(tagLib.fullName)
    }

    /**
     * Mimics the behavior of the render method in controllers but returns the rendered contents directly
     *
     * @param args The same arguments as the controller render method accepts
     * @return The resulting rendering GSP
     */
    String render(Map args) {
        def uri = null
        final attributes = webRequest.attributes
        if (args.template) {
            uri = attributes.getTemplateUri(args.template, request)
        }
        else if (args.view) {
            uri = attributes.getViewUri(args.view, request)
        }
        if (uri != null) {
            def engine = applicationContext.getBean(GroovyPagesTemplateEngine)
            final t = engine.createTemplate(uri)
            if (t != null) {
                def sw = new StringWriter()
                renderTemplateToStringWriter(sw, t, args.model ?: [:])
                return sw.toString()
            }
        }
        return null
    }
    /**
     * Renders a template for the given contents and model
     *
     * @param contents The contents
     * @param model The model
     * @return The rendered template
     */
    String applyTemplate(String contents, Map model = [:]) {
        def sw = new StringWriter()
        applyTemplate sw, contents, model
        return sw.toString()
    }

    protected void applyTemplate(StringWriter sw, template, params = [:]) {
        def engine = applicationContext.getBean(GroovyPagesTemplateEngine)

        def t = engine.createTemplate(template, "test_" + System.currentTimeMillis())
        renderTemplateToStringWriter(sw, t, params)
    }

    private renderTemplateToStringWriter(StringWriter sw, Template t, params) {
        def w = t.make(params)
        def previousOut = webRequest.out
        try {
            def out = new GrailsPrintWriter(sw)
            webRequest.out = out
            w.writeTo(out)

        }
        finally {
            webRequest.out = previousOut
        }
    }
}
