/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.web.taglib

import com.opensymphony.module.sitemesh.RequestConstants
import com.opensymphony.module.sitemesh.html.util.CharArray
import com.opensymphony.module.sitemesh.parser.HTMLPageParser
import com.opensymphony.module.sitemesh.parser.TokenizedHTMLPage
import grails.util.GrailsUtil
import org.grails.buffer.FastStringWriter
import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.grails.core.io.MockStringResourceLoader
import org.grails.gsp.GroovyPageBinding
import org.grails.web.sitemesh.FactoryHolder
import org.grails.web.sitemesh.GSPSitemeshPage
import org.grails.web.sitemesh.GrailsLayoutDecoratorMapper
import org.grails.taglib.GrailsTagException
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.web.servlet.support.RequestContextUtils as RCU

/**
 * Tests for the RenderTagLib.groovy file which contains tags for rendering.
 *
 * @author Marcel Overdijk
 */
class RenderTagLibTests extends AbstractGrailsTagTests {
    // test for GRAILS-5376
    void testPaginateTag() {
         def template = '<g:paginate controller="book" total="" offset="" />'
         applyTemplate(template)
     }

    void testPaginateOmissionAttributes() {
        def template = '<g:paginate next="Forward" prev="Backward" max="5" total="20" offset="10" controller="book" action="list"/>'
        assertOutputEquals '<a href="/book/list?offset=5&amp;max=5" class="prevLink">Backward</a><a href="/book/list?offset=0&amp;max=5" class="step">1</a><a href="/book/list?offset=5&amp;max=5" class="step">2</a><span class="currentStep">3</span><a href="/book/list?offset=15&amp;max=5" class="step">4</a><a href="/book/list?offset=15&amp;max=5" class="nextLink">Forward</a>', template

        template = '<g:paginate next="Forward" prev="Backward" max="5" total="20" offset="10" controller="book" action="list" omitPrev="true"/>'
        assertOutputNotContains 'Backward', template
        assertOutputContains 'Forward', template

        template = '<g:paginate next="Forward" prev="Backward" max="5" total="20" offset="10" controller="book" action="list" omitPrev="false"/>'
        assertOutputContains 'Backward', template
        assertOutputContains 'Forward', template

        template = '<g:paginate next="Forward" prev="Backward" max="5" total="20" offset="10" controller="book" action="list" omitNext="true"/>'
        assertOutputContains 'Backward', template
        assertOutputNotContains 'Forward', template

        template = '<g:paginate next="Forward" prev="Backward" max="5" total="20" offset="10" controller="book" action="list" omitNext="false"/>'
        assertOutputContains 'Backward', template
        assertOutputContains 'Forward', template

        template = '<g:paginate  max="2" total="20" offset="10" maxsteps="3" controller="book" action="list" omitPrev="true" omitNext="true" omitFirst="true" />'
        assertOutputNotContains '<a href="/book/list?offset=0&amp;max=2" class="step">1</a>', template
        assertOutputContains '<a href="/book/list?offset=18&amp;max=2" class="step">10</a>', template

        template = '<g:paginate  max="2" total="20" offset="2" maxsteps="3" controller="book" action="list" omitPrev="true" omitNext="true" omitFirst="true" />'
        assertOutputContains '<a href="/book/list?offset=0&amp;max=2" class="step">1</a>', template

        template = '<g:paginate  max="2" total="20" offset="10" maxsteps="3" controller="book" action="list" omitPrev="true" omitNext="true" omitFirst="false" />'
        assertOutputContains '<a href="/book/list?offset=0&amp;max=2" class="step">1</a>', template

        template = '<g:paginate  max="2" total="20" offset="10" maxsteps="3" controller="book" action="list" omitPrev="true" omitNext="true" omitLast="true" />'
        assertOutputContains '<a href="/book/list?offset=0&amp;max=2" class="step">1</a>', template
        assertOutputNotContains '<a href="/book/list?offset=18&amp;max=2" class="step">10</a>', template

        template = '<g:paginate  max="2" total="20" offset="16" maxsteps="3" controller="book" action="list" omitPrev="true" omitNext="true" omitLast="true" />'
        assertOutputContains '<a href="/book/list?offset=18&amp;max=2" class="step">10</a>', template

        template = '<g:paginate  max="2" total="20" offset="10" maxsteps="3" controller="book" action="list" omitPrev="true" omitNext="true" omitLast="false" />'
        assertOutputContains '<a href="/book/list?offset=18&amp;max=2" class="step">10</a>', template
    }

    void testPaginateGap() {
        def template = '<g:paginate  max="2" total="20" offset="10" maxsteps="3" controller="book" action="list" />'
        assertOutputContains '<a href="/book/list?offset=0&amp;max=2" class="step">1</a><span class="step gap">..</span><a href="/book/list?offset=8&amp;max=2" class="step">5</a>', template
        assertOutputContains '<a href="/book/list?offset=12&amp;max=2" class="step">7</a><span class="step gap">..</span><a href="/book/list?offset=18&amp;max=2" class="step">10</a>', template

        template = '<g:paginate  max="2" total="20" offset="4" maxsteps="3" controller="book" action="list" />'
        assertOutputContains '<a href="/book/list?offset=0&amp;max=2" class="step">1</a><a href="/book/list?offset=2&amp;max=2" class="step">2</a>', template

        template = '<g:paginate  max="2" total="20" offset="14" maxsteps="3" controller="book" action="list" />'
        assertOutputContains '<a href="/book/list?offset=16&amp;max=2" class="step">9</a><a href="/book/list?offset=18&amp;max=2" class="step">10</a>', template
    }

    protected void onInit() {
        if (name in ['testPaginateMappingAndAction', 'testPaginateNamespace']) {
            def mappingClass = gcl.parseClass('''
    class TestUrlMappings {
        static mappings = {
            name claimTab: "/claim/$id/$action" {
                controller = 'Claim'
                constraints { id(matches: /\\d+/) }
            }
            "/userAdmin/$id?" {
                controller = 'admin'
                namespace = 'users'
            }
            "/reportAdmin/$id?" {
                controller = 'admin'
                namespace = 'reports'
            }
        }
    }
            ''')

            grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, mappingClass)
        }
    }

    void testPaginateMappingAndAction() {
        def template = '<g:paginate next="Forward" prev="Back" maxsteps="8" max="10" id="1" mapping="claimTab" total="12" action="documents"/>'
        assertOutputEquals '<span class="currentStep">1</span><a href="/claim/1/documents?offset=10&amp;max=10" class="step">2</a><a href="/claim/1/documents?offset=10&amp;max=10" class="nextLink">Forward</a>', template
    }

    void testPaginateNamespace() {
        def template = '<g:paginate next="Forward" prev="Back" maxsteps="8" max="10" id="1" total="12" namespace="users" controller="admin" action="index"/>'
        assertOutputEquals '<span class="currentStep">1</span><a href="/userAdmin/1?offset=10&amp;max=10" class="step">2</a><a href="/userAdmin/1?offset=10&amp;max=10" class="nextLink">Forward</a>', template
        
        template = '<g:paginate next="Forward" prev="Back" maxsteps="8" max="10" id="1" total="12" namespace="reports" controller="admin" action="index"/>'
        assertOutputEquals '<span class="currentStep">1</span><a href="/reportAdmin/1?offset=10&amp;max=10" class="step">2</a><a href="/reportAdmin/1?offset=10&amp;max=10" class="nextLink">Forward</a>', template
    }

    void testPageProperty() {

        def template = '<g:pageProperty name="foo.bar" />'

        def head = ""
        TokenizedHTMLPage page = new TokenizedHTMLPage([] as char[], new CharArray(0), new CharArray(0))
        request[RequestConstants.PAGE] = page

        page.addProperty("foo.bar", "good")

        assertOutputEquals "good", template

        template = '<g:pageProperty name="foo.bar" writeEntireProperty="true" />'
        assertOutputEquals " bar=\"good\"", template
    }

    void testIfPageProperty() {
        def template = '<g:ifPageProperty name="foo.bar">Hello</g:ifPageProperty>'

        TokenizedHTMLPage page = new TokenizedHTMLPage([] as char[], new CharArray(0), new CharArray(0))
        request[RequestConstants.PAGE] = page

        page.addProperty("foo.bar", "true")

        assertOutputEquals "Hello", template

        template = '<g:ifPageProperty name="page.contentbuffer">Hello 2</g:ifPageProperty>'

        GSPSitemeshPage smpage = new GSPSitemeshPage()
        request[RequestConstants.PAGE] = smpage

        FastStringWriter sw=new FastStringWriter()
        sw.write("true")
        smpage.setContentBuffer("contentbuffer", sw.getBuffer())

        assertOutputEquals "Hello 2", template
    }

    void testTemplateNamespace() {
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/table/_tableRow.gsp', '<tr><td class="prop">${label}</td><td class="value">${value}</td></tr>')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        webRequest.controllerName = "table"

        def template = '<tmpl:tableRow label="one" value="two" encodeAs="raw" />'

        assertOutputEquals '<tr><td class="prop">one</td><td class="value">two</td></tr>', template

        // now test method call

        template = '${tmpl.tableRow(label:"one", value:"two", encodeAs:"raw")}'

        assertOutputEquals '<tr><td class="prop">one</td><td class="value">two</td></tr>', template
        // execute twice to make sure methodMissing works
        assertOutputEquals '<tr><td class="prop">one</td><td class="value">two</td></tr>', template
    }

    void testRenderWithNonExistantTemplate() {
        def template = '<g:render template="bad" />'

        try {
            applyTemplate(template)
            fail "Should have thrown exception"
        }
        catch (GrailsTagException e) {
            assert e.message.contains("Template not found for name [bad]") : "error message should have contained template name"
        }
    }

    void testRenderTagWithContextPath() {
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/amazon/book/_book.gsp', 'content ${foo}: ${body()}')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        def template = '<g:render contextPath="/amazon" template="/book/book" model="[foo: \'bar\']">hello</g:render>'

        assertOutputEquals 'content bar: hello', template

        resourceLoader.registerMockResource('/foo/book/_book.gsp', 'foo ${foo}: ${body()}')
        resourceLoader.registerMockResource("/plugins/controllers-${GrailsUtil.grailsVersion}/foo/book/_book.gsp".toString(), 'plugin foo ${foo}: ${body()}')
        resourceLoader.registerMockResource("/plugins/controllers-${GrailsUtil.grailsVersion}/foo/book/_two.gsp".toString(), 'plugin foo ${foo}: ${body()}')

        template = '<g:render plugin="controllers" template="/foo/book/book" model="[foo: \'bar\']">hello</g:render>'
        assertOutputEquals 'plugin foo bar: hello', template

        template = '<g:render contextPath="" template="/foo/book/book" model="[foo: \'bar\']">hello</g:render>'

        assertOutputEquals 'foo bar: hello', template

        request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, new GroovyPageBinding("/plugins/controllers-${GrailsUtil.grailsVersion}"))
        assertOutputEquals 'foo bar: hello', template // application template should be able to override plugin template
        template = '<g:render contextPath="" template="/foo/book/two" model="[foo: \'bar\']">hello</g:render>'

        request.removeAttribute GrailsApplicationAttributes.PAGE_SCOPE
    }

    void testRenderTagWithBody() {
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/book/_book.gsp', 'content ${foo}: ${body()}')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        def template = '<g:render template="/book/book" model="[foo: \'bar\']">hello</g:render>'
        assertOutputEquals 'content bar: hello', template
    }

    void testRenderTagCollectionAndModel() {
        appCtx.groovyPagesTemplateRenderer.clearCache()
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/book/_book.gsp', '[book = ${string} it=${it} foo=${foo}]')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        def template = '<g:render template="/book/book" collection="${books}" model="[foo: \'bar\']" />'

        assertOutputEquals '[book = The Stand it=The Stand foo=bar][book = The Shining it=The Shining foo=bar]', template, [books:['The Stand', 'The Shining']]
    }

    void testRenderTagBeforeAndAfterModel() {

        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource("/foo/_part.gsp", "test")
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)
        webRequest.controllerName = "foo"
        def template = '''<p>id: ${foo1.id},name: ${foo1.name}</p><g:render template="part" model="['foo1':foo2]" /><p>id: ${foo1.id},name: ${foo1.name}</p>'''

        assertOutputEquals('<p>id: 1,name: foo</p>test<p>id: 1,name: foo</p>', template, [foo1:[id:1, name:'foo'], foo2:[id:2, name:'bar']])
    }

    void testSortableColumnTag() {
        final StringWriter sw = new StringWriter()
        final PrintWriter pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Title')
    }

    void testSortableColumnTagWithTitleKey() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        // test message not resolved; title property will be used (when provided)

        // without (default) title property provided
        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", titleKey:"book.title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'book.title')

        sw = new StringWriter()
        pw = new PrintWriter(sw)

        // with (default) title property provided
        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", title:"Title", titleKey:"book.title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Title')

        // test message resolved

        sw = new StringWriter()
        pw = new PrintWriter(sw)

        messageSource.addMessage("book.title", RCU.getLocale(request), "Book Title")

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", title:"Title", titleKey:"book.title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Book Title')
    }

    void testSortableColumnTagWithAction() {
        final StringWriter sw = new StringWriter()
        final PrintWriter pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([action:"list2", property:"title", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Title')
    }

    void testSortableColumnTagWithDefaultOrder() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        // default order: desc

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", defaultOrder:"desc", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'desc', 'Title')

        // default order: asc

        sw = new StringWriter()
        pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", defaultOrder:"asc", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Title')

        // invalid default order

        sw = new StringWriter()
        pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", defaultOrder:"invalid", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Title')
    }

    void testSortableColumnTagWithAdditionalAttributes() {
        final StringWriter sw = new StringWriter()
        final PrintWriter pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // use sorted map to be able to predict the order in which tag attributes are generated
            // adding the class property is a dirty hack to predict the order; it will be overridden in the tag anyway
            def attrs = new TreeMap([property:"title", title:"Title", class:"other", style:"width: 200px;"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'other sortable', 'asc', 'Title', ' style="width: 200px;"')
    }

    void testSortableColumnTagSorted() {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        // column sorted asc

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // set request params
            webRequest.getParams().put("sort", "title")
            webRequest.getParams().put("order", "asc")
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable sorted asc', 'desc', 'Title')

        // column sorted desc

        sw = new StringWriter()
        pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // set request params
            webRequest.getParams().put("sort", "title")
            webRequest.getParams().put("order", "desc")
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable sorted desc', 'asc', 'Title')

        // other column sorted

        sw = new StringWriter()
        pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // set request params
            webRequest.getParams().put("sort", "price")
            webRequest.getParams().put("order", "desc")
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", title:"Title"])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Title')

        // sort in params attribute

        sw = new StringWriter()
        pw = new PrintWriter(sw)

        withTag("sortableColumn", pw) { tag ->
            webRequest.controllerName = "book"
            // set request params
            webRequest.getParams().put("sort", "price")
            webRequest.getParams().put("order", "desc")
            // use sorted map to be able to predict the order in which tag attributes are generated
            def attrs = new TreeMap([property:"title", title:"Title", params:[sort:"id"]])
            tag.call(attrs)

        }
        checkTagOutput(sw.toString(), 'sortable', 'asc', 'Title')
    }

    /**
     * Checks that the given output matches what is expected from the
     * tag, based on the given parameters. It ensures that the order
     * of the query parameters in the generated anchor's 'href' attribute
     * is not significant. If the output does not match the expected
     * text, an assertion is thrown.
     * @param output The output to check (String).
     * @param expectedClassValue The expected contents of the 'class'
     * attribute in the tag's output (String).
     * @param expectedOrder The expected sort order generated by the
     * tag (either 'asc' or 'desc').
     * @param expectedContent The expected content of the generated
     * anchor tag (String).
     */
    void checkTagOutput(output, expectedClassValue, expectedOrder, expectedContent) {
        // Check the output of the tag. The query parameters are not
        // guaranteed to be in any particular order, so we extract
        // them with a regular expression.
        def p = ~"<th class=\"${expectedClassValue}\" ><a href=\"\\S+?(\\w+=\\w+)&amp;(\\w+=\\w+)\">${expectedContent}</a></th>"
        def m = p.matcher(output)

        // First step: check the output as a whole matches what we
        // expect.
        assertTrue "Output [$output] doesn't match expected pattern", m.matches()

        // Now make sure the expected query parameters are there,
        // regardless of their order.
        if (m.group(1) == 'sort=title') {
            assertEquals m.group(2), "order=${expectedOrder}"
        }
        else {
            assertEquals m.group(1), "order=${expectedOrder}"
            assertEquals m.group(2), 'sort=title'
        }
    }

    /**
     * Checks that the given output matches what is expected from the
     * tag, based on the given parameters. It ensures that the order
     * of the query parameters in the generated anchor's 'href' attribute
     * is not significant. If the output does not match the expected
     * text, an assertion is thrown.
     * @param output The output to check (String).
     * @param expectedClassValue The expected contents of the 'class'
     * attribute in the tag's output (String).
     * @param expectedOrder The expected sort order generated by the
     * tag (either 'asc' or 'desc').
     * @param expectedContent The expected content of the generated
     * anchor tag (String).
     * @param otherAttrs Any additional attributes that will be passed
     * through by the tag. This string takes the form of the literal
     * text that will appear in the generated HTML (e.g.
     * ' style="width: 100%"'). Note that the string should normally
     * begin with a space (' ').
     */
    void checkTagOutput(output, expectedClassValue, expectedOrder, expectedContent, otherAttrs) {
        // Check the output of the tag. The query parameters are not
        // guaranteed to be in any particular order, so we extract
        // them with a regular expression.
        def p = ~"<th class=\"${expectedClassValue}\"${otherAttrs} ><a href=\"\\S+?(\\w+=\\w+)&amp;(\\w+=\\w+)\">${expectedContent}</a></th>"
        def m = p.matcher(output)

        // First step: check the output as a whole matches what we
        // expect.
        assert m.matches()

        // Now make sure the expected query parameters are there,
        // regardless of their order.
        if (m.group(1) == 'sort=title') {
            assertEquals m.group(2), "order=${expectedOrder}"
        }
        else {
            assertEquals m.group(1), "order=${expectedOrder}"
            assertEquals m.group(2), 'sort=title'
        }
    }

    void testMultipleRender() {
        appCtx.groovyPagesTemplateRenderer.clearCache()
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/_test.gsp', '[hello ${name}] ${request.someattribute}')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        def g = appCtx.gspTagLibraryLookup.lookupNamespaceDispatcher('g')
        request.setAttribute('someattribute', '1')
        assertEquals g.render(template:'/test', model: [name: 'world']), '[hello world] 1'
        request.setAttribute('someattribute', '2')
        assertEquals g.render(template:'/test', model: [name: 'world']), '[hello world] 2'
        request.setAttribute('someattribute', '3')
        def template = '<g:render template="/test" model="[name: \'world\']" />'
        assertOutputEquals '[hello world] 3', template
    }

    void testGRAILS7887failsBeforeFixing() {
        appCtx.groovyPagesTemplateRenderer.clearCache()
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/_test.gsp', '[hello ${name}] ${params.someparam}')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        def g = appCtx.gspTagLibraryLookup.lookupNamespaceDispatcher('g')
        webRequest.params.someparam = '1'
        assertEquals g.render(template:'/test', model: [name: 'world']), '[hello world] 1'
        webRequest.params.someparam = '2'
        def template = '<g:render template="/test" model="[name: \'world\']" />'
        assertOutputEquals '[hello world] 2', template
    }

    void testGRAILS7887okBeforeFixing() {
        appCtx.groovyPagesTemplateRenderer.clearCache()
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/_test.gsp', '[hello ${name}] ${params.someparam}')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        webRequest.params.someparam = '1'
        def template = '<g:render template="/test" model="[name: \'world\']" />'
        assertOutputEquals '[hello world] 1', template
    }

    void testGRAILS7871() {
        appCtx.groovyPagesTemplateRenderer.clearCache()
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/_test.gsp', '[hello ${name}] ${params.someparam}')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        webRequest.params.someparam = '1'
        def template = '<g:render template="/test" model="[name: \'world\']" />'
        assertOutputEquals '[hello world] 1', template

        def g = appCtx.gspTagLibraryLookup.lookupNamespaceDispatcher('g')
        webRequest.params.someparam = '2'
        assertEquals g.render(template:'/test', model: [name: 'world']), '[hello world] 2'
    }

    void testGspContentTypeSetting() {
        appCtx.groovyPagesTemplateRenderer.clearCache()
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/_test.gsp', 'hello')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)

        def g = appCtx.gspTagLibraryLookup.lookupNamespaceDispatcher('g')
        assertEquals g.render(template:'/test'), 'hello'

        def template = '<%@ page contentType="my/contenttype" %>hello world'
        assertOutputEquals 'hello world', template

        assertEquals 'my/contenttype', response.getContentType()
    }

    void testGspContentTypeSetting2() {
        assertEquals null, response.getContentType()
        def template = '<%@ page contentType="my/contenttype" %>hello world'
        assertOutputEquals 'hello world', template
        assertEquals 'my/contenttype', response.getContentType()
    }

    void testApplyLayout() {
        GrailsLayoutDecoratorMapper decoratorMapper=new GrailsLayoutDecoratorMapper()
        decoratorMapper.groovyPageLayoutFinder = appCtx.groovyPageLayoutFinder
        FactoryHolder.setFactory([getDecoratorMapper: { -> decoratorMapper}] as com.opensymphony.module.sitemesh.Factory)
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/layouts/layout.gsp', '<layoutapplied><g:layoutTitle /> - <g:layoutBody/></layoutapplied>')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)
        def template='<g:applyLayout name="layout"><html><head><title>title here</title></head><body>Hello world!</body></html></g:applyLayout>'
        assertOutputEquals '<layoutapplied>title here - Hello world!</layoutapplied>', template
    }

    void testApplyLayoutParse() {
        GrailsLayoutDecoratorMapper decoratorMapper=new GrailsLayoutDecoratorMapper()
        decoratorMapper.groovyPageLayoutFinder = appCtx.groovyPageLayoutFinder
        FactoryHolder.setFactory([getDecoratorMapper: { -> decoratorMapper}, getPageParser: { String contentType -> new HTMLPageParser() }] as com.opensymphony.module.sitemesh.Factory)
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/layouts/layout.gsp', '<layoutapplied><g:layoutTitle /> - <g:layoutBody/></layoutapplied>')
        appCtx.groovyPagesTemplateEngine.groovyPageLocator.addResourceLoader(resourceLoader)
        def template='<g:applyLayout name="layout" parse="${true}"><html><head><${"title"}>title here</${"title"}></head><body>Hello world!</body></html></g:applyLayout>'
        assertOutputEquals '<layoutapplied>title here - Hello world!</layoutapplied>', template

        template='<g:applyLayout name="layout" parse="false"><html><head><${"title"}>title here</${"title"}></head><body>Hello world!</body></html></g:applyLayout>'
        assertOutputEquals '<layoutapplied> - Hello world!</layoutapplied>', template
    }
}
