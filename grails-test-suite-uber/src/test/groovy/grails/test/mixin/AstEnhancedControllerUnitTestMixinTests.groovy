package grails.test.mixin

import grails.testing.web.controllers.ControllerUnitTest
import org.grails.plugins.testing.GrailsMockMultipartFile
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class AstEnhancedControllerUnitTestMixinTests extends Specification implements ControllerUnitTest<AnotherController> {

    void setup() {
        messageSource.addMessage("foo.bar", request.locale, "World")
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderText() {
        when:
        controller.renderText()
        
        then:
        response.contentAsString == "good"
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testSimpleControllerRedirect() {
        when:
        controller.redirectToController()

        then:
        response.redirectedUrl == '/bar'
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderView() {        
        when:
        controller.renderView()

        then:
        "/another/foo" == controller.modelAndView.viewName
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderXml() {        
        when:
        controller.renderXml()

        then:
        "<book title='Great'/>" == controller.response.contentAsString
        "Great" == controller.response.xml.@title.text()
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderJson() {
        
        when:
        controller.renderJson()

        then:
        '{"book":"Great"}' == controller.response.contentAsString
        "Great" == controller.response.json.book
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderAsJson() {
        
        when:
        controller.renderAsJson()

        then:
        '{"foo":"bar"}' == controller.response.contentAsString
        "bar" == controller.response.json.foo
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderState() {
        when:
        params.foo = "bar"
        request.bar = "foo"
        controller.renderState()
        def xml = response.xml

        then:
        xml.parameter.find { it.@name == 'foo' }.@value.text() == 'bar'
        xml.attribute.find { it.@name == 'bar' }.@value.text() == 'foo'
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testInjectedProperties() {
        expect:
        request != null
        response != null
        servletContext != null
        params != null
        grailsApplication != null
        applicationContext != null
        webRequest != null
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testControllerAutowiring() {
        when:
        controller.renderMessage()

        then:
        'World' == controller.response.contentAsString
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderWithFormatXml() {        
        when:
        response.format = 'xml'
        controller.renderWithFormat()

        then:
        '<?xml version="1.0" encoding="UTF-8"?><map><entry key="foo">bar</entry></map>' == response.contentAsString
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderWithFormatHtml() {        
        when:
        response.format = 'html'
        def model = controller.renderWithFormat()

        then:
        model?.foo == 'bar'
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testWithFormTokenSynchronization() {
        when:
        controller.renderWithForm()

        then:
        "Bad" == response.contentAsString

        when:
        def holder = SynchronizerTokensHolder.store(session)
        def token = holder.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
        params[SynchronizerTokensHolder.TOKEN_KEY] = token
        response.reset()
        controller.renderWithForm()

        then:
        "Good" == response.contentAsString
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testFileUpload() {        
        when:
        final file = new GrailsMockMultipartFile("myFile", "foo".bytes)
        request.addFile(file)
        controller.uploadFile()

        then:
        file.targetFileLocation.path == "${File.separatorChar}local${File.separatorChar}disk${File.separatorChar}myFile"
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderBasicTemplateNoTags() {
        given:
        def templateName = 'testRenderBasicTemplateNoTags'

        when:
        groovyPages["/another/_${templateName}.gsp" as String] = 'Hello <%= 10 %>'
        controller.renderTemplate(templateName)

        then:
        response.contentAsString == "Hello 10"
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderBasicTemplateWithTags() {
        given:
        def templateName = 'testRenderBasicTemplateWithTags'

        when:
        groovyPages["/another/_${templateName}.gsp" as String] = 'Hello <g:message code="foo.bar" />'
        controller.renderTemplate(templateName)

        then:
        response.contentAsString == "Hello World"
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testRenderBasicTemplateWithLinkTag() {
        given:
        def templateName = 'testRenderBasicTemplateWithLinkTag'

        when:
        groovyPages["/another/_${templateName}.gsp" as String] = 'Hello <g:createLink controller="bar" />'
        controller.renderTemplate(templateName)

        then:
        response.contentAsString == "Hello /bar"
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testInvokeTagLibraryMethod() {
        when:
        controller.renderTemplateContents()

        then:
        response.contentAsString == "/foo"
    }

    @Ignore("PluginAwareResourceBundleMessageSource instead of StaticMessageSource which supports .addMessage()")
    void testInvokeTagLibraryMethodViaNamespace() {
        when:
        groovyPages['/another/_bar.gsp'] = 'Hello <g:message code="foo.bar" />'

        controller.renderTemplateContentsViaNamespace()

        then:
        response.contentAsString == "Hello World"
    }
}


