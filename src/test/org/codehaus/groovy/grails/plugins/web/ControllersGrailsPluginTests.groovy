package org.codehaus.groovy.grails.plugins.web

import grails.spring.BeanBuilder
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.core.io.FileSystemResource
import org.springframework.web.multipart.commons.CommonsMultipartResolver

class ControllersGrailsPluginTests extends AbstractGrailsPluginTests {

    void onSetUp() {
        gcl.parseClass(
                """
class TestController {
   def list = {}			
}
""")

        gcl.parseClass("""\
abstract class BaseController {
    def index = {}
}
""")

        gcl.parseClass(
                """
class SubController extends BaseController {
   def list = {}
}
""")

        gcl.parseClass(
                """
class TagLibTestController {
    def list = {
        StringWriter w = new StringWriter()
        def builder = new groovy.xml.MarkupBuilder(w)
            builder.html{
                head{
                    title 'Log in'
                }
                body{
                h1 'Hello'
                form{
                }
            }
        }

        def html = w.toString()
        render html
    }
}
""")

        gcl.parseClass(
                """class FormTagLib {
    def form = {  attrs, body ->
        out << 'dummy form tag'
    }
}
""")

        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
    }

    void testControllersPlugin() {
        assert appCtx.containsBean("TestController")
    }

    void testOldBindDataMethodsDelegateToNewOnes() {
        Class testClass = parseTestBean()
        def controller = appCtx.getBean("TestController")
        def bean = testClass.newInstance()
        def params = [name: "beanName", pages: 3]
        controller.bindData(bean, params, ["pages"])
        assertEquals(0, bean.pages)
        assertEquals("beanName", bean.name)

        bean = testClass.newInstance()
        params = ['a.name': "beanName", 'b.address': "address", 'a.pages': 3]
        controller.bindData(bean, params, ["pages"], "a")
        assertEquals(0, bean.pages)
        assertEquals("beanName", bean.name)
        assertNull(bean.address)

    }

    void testBindDataConvertsSingleIncludeToListInternally() {
        Class testClass = parseTestBean()
        def bean = testClass.newInstance()
        def params = ['a.name': "beanName", 'b.address': "address", 'a.pages': 3]
        def controller = appCtx.getBean("TestController")
        controller.bindData(bean, params, [include: "name"], "a")
        assertEquals(0, bean.pages)
        assertEquals("beanName", bean.name)
        assertNull(bean.address)
    }

    void testCommonsMultipartCanBeDisabled() {
        tearDown()
        ConfigurationHolder.setConfig(null)

        this.gcl = new GroovyClassLoader()
        gcl.parseClass("grails.disableCommonsMultipart=true", 'Config.groovy')
        setUp()

        assertTrue ga.config.grails.disableCommonsMultipart
        try {
            appCtx.getBean(GrailsRuntimeConfigurator.MULTIPART_RESOLVER_BEAN)
            fail('Multipart was not disabled')
        } catch (NoSuchBeanDefinitionException e) {
            //expected
        }

        tearDown()
        ConfigurationHolder.setConfig(null)
        this.gcl = new GroovyClassLoader()
        setUp()

        assertTrue ga.config.grails.disableCommonsMultipart.size() == 0

        assertTrue appCtx.getBean(GrailsRuntimeConfigurator.MULTIPART_RESOLVER_BEAN) instanceof CommonsMultipartResolver
    }

    void testBeansWhenNotWarDeployedAndDevelopmentEnv() {
        try {
            System.setProperty("grails.env", "development")
            def mock = [application: [config: new ConfigObject(), warDeployed: false]]
            def plugin = new GroovyPagesGrailsPlugin()
            def beans = plugin.doWithSpring
            def bb = new BeanBuilder()
            bb.setBinding(new Binding(mock))
            bb.beans(beans)
            def beanDef = bb.getBeanDefinition('groovyPageResourceLoader')
            assertEquals "org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader", beanDef.beanClassName
            assertNotNull beanDef.getPropertyValues().getPropertyValue('baseResource')

            assertEquals new FileSystemResource("."), beanDef.getPropertyValues().getPropertyValue('baseResource').getValue()

            beanDef = bb.getBeanDefinition("groovyPagesTemplateEngine")
            assertEquals "groovyPageResourceLoader", beanDef.getPropertyValues().getPropertyValue("resourceLoader").getValue()?.beanName

            beanDef = bb.getBeanDefinition("jspViewResolver")
            assertEquals "groovyPageResourceLoader", beanDef.getPropertyValues().getPropertyValue("resourceLoader").getValue()?.beanName

        } finally {
            System.setProperty("grails.env", "")
        }
    }

    void testBeansWhenWarDeployedAndDevelopmentEnv() {
        try {
            System.setProperty("grails.env", "development")
            def mock = [application: [config: new ConfigObject(), warDeployed: true]]
            def plugin = new ControllersGrailsPlugin()
            def beans = plugin.doWithSpring
            def bb = new BeanBuilder()
            bb.setBinding(new Binding(mock))
            bb.beans(beans)
            assertNull bb.getBeanDefinition('groovyPageResourceLoader')
        } finally {
            System.setProperty("grails.env", "")
        }
    }

    void testTagLibCallResolution() {
        def webRequest = GrailsWebUtil.bindMockWebRequest(appCtx)
        def instance = appCtx.getBean('TagLibTestController')
        instance.list() // should not throw StackOverflow
        //TODO GRAILS-1765 get MarkupBuilder methodMissing to get called before Controller methodMissing
        /*  assertEquals('''<html>
        <head>
          <title>Log in</title>
        </head>
        <body>
          <h1>Hello</h1>
          <form>
            <input type='text' name='test' />
          </form>
        </body>
      </html>''', response.contentAsString) */
        String newLine = System.getProperty("line.separator")
        assertEquals("<html>${newLine}  <head>${newLine}    <title>Log in</title>${newLine}  </head>${newLine}  <body>${newLine}    <h1>Hello</h1>${newLine}  </body>${newLine}</html>".trim(), webRequest.currentResponse.contentAsString.trim())
    }

    Class parseTestBean() {
        return gcl.parseClass(
                """
        class TestDomainObject {
           String name
           int pages = 0
           String address
        }
        """)
    }


}
