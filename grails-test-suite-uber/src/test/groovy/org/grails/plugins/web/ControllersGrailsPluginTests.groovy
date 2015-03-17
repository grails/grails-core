package org.grails.plugins.web

import grails.core.DefaultGrailsApplication
import grails.spring.BeanBuilder
import grails.util.GrailsWebMockUtil

import org.grails.plugins.web.controllers.ControllersGrailsPlugin

class ControllersGrailsPluginTests extends AbstractGrailsPluginTests {

    protected void onSetUp() {
        gcl.parseClass """
@grails.artefact.Artefact('Controller')
class TestController {
   def list = {}
}
"""

        gcl.parseClass """
abstract class BaseController {
    def index = {}
}
"""

        gcl.parseClass """
class SubController extends BaseController {
   def list = {}
}
"""

        gcl.parseClass """
@grails.artefact.Artefact('Controller')
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
"""

        gcl.parseClass """
import grails.gsp.TagLib

@TagLib
class FormTagLib {
    Closure form = {  attrs, body ->
        out << 'dummy form tag'
    }
}
"""

        pluginsToLoad << gcl.loadClass("org.grails.plugins.CoreGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.grails.plugins.i18n.I18nGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.grails.plugins.web.controllers.ControllersGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.grails.plugins.web.GroovyPagesGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
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

    void testBeansWhenNotWarDeployedAndDevelopmentEnv() {
        try {
            System.setProperty("grails.env", "development")
            def plugin = new GroovyPagesGrailsPlugin() {
                @Override
                protected boolean isDevelopmentMode() {
                    return true
                }
            }

            plugin.grailsApplication = new DefaultGrailsApplication() {
                @Override
                boolean isWarDeployed() {
                    false
                }
            }
            plugin.grailsApplication.initialise()
            def beans = plugin.doWithSpring()
            def bb = new BeanBuilder()
            bb.setBinding(new Binding(manager:mockManager))
            bb.beans(beans)
            def beanDef = bb.getBeanDefinition('groovyPageResourceLoader')
            assertEquals "org.grails.gsp.GroovyPageResourceLoader", beanDef.beanClassName
            assertNotNull beanDef.getPropertyValues().getPropertyValue('baseResource')

            assertEquals "file:${new File(".").absolutePath}/".toString(), beanDef.getPropertyValues().getPropertyValue('baseResource').getValue()

            beanDef = bb.getBeanDefinition("groovyPagesTemplateEngine")
            assertEquals "groovyPageLocator", beanDef.getPropertyValues().getPropertyValue("groovyPageLocator").getValue()?.beanName

        }
        finally {
            System.setProperty("grails.env", "")
        }
    }

    void testBeansWhenWarDeployedAndDevelopmentEnv() {
        try {
            System.setProperty("grails.env", "development")
            def plugin = new ControllersGrailsPlugin()
            plugin.grailsApplication = new DefaultGrailsApplication() {
                @Override
                boolean isWarDeployed() {
                    return super.isWarDeployed()
                }
            }
            plugin.grailsApplication.initialise()
            def beans = plugin.doWithSpring()
            def bb = new BeanBuilder()
            bb.setBinding(new Binding(manager:mockManager))
            bb.beans(beans)
            assertNull bb.getBeanDefinition('groovyPageResourceLoader')
        }
        finally {
            System.setProperty("grails.env", "")
        }
    }

    void testTagLibCallResolution() {
        def webRequest = GrailsWebMockUtil.bindMockWebRequest(appCtx)
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
        assert ("<html>  <head>    <title>Log in</title>  </head>  <body>    <h1>Hello</h1>  </body></html>".trim() ==
                     webRequest.currentResponse.contentAsString.replaceAll('[\r\n]', '').trim())
    }

    Class parseTestBean() {
        gcl.parseClass """
        class TestDomainObject {
           String name
           int pages = 0
           String address
        }
        """
    }
}
