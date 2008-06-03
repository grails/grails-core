package org.codehaus.groovy.grails.plugins.web

import grails.spring.BeanBuilder
import org.springframework.core.io.FileSystemResource

class ControllersGrailsPluginTests extends AbstractGrailsPluginTests {
	
	
	void onSetUp() {
		gcl.parseClass(
"""
class TestController {
   def list = {}			
}
""")

        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")

	}
	
	void testControllersPlugin() {		
		assert appCtx.containsBean("TestControllerTargetSource")
		assert appCtx.containsBean("TestControllerProxy")
		assert appCtx.containsBean("TestControllerClass")
		assert appCtx.containsBean("TestController")
	}

	void testOldBindDataMethodsDelegateToNewOnes() {
	    Class testClass = parseTestBean()
	    def controller = appCtx.getBean("TestController")
	    def bean = testClass.newInstance()
	    def params = [name:"beanName", pages:3]
	    controller.bindData(bean, params, ["pages"])
	    assertEquals(0, bean.pages)
	    assertEquals("beanName", bean.name)

	    bean = testClass.newInstance()
	    params = ['a.name':"beanName", 'b.address':"address", 'a.pages':3]
	    controller.bindData(bean, params, ["pages"], "a")
	    assertEquals(0, bean.pages)
	    assertEquals("beanName", bean.name)
	    assertNull(bean.address)

	    }

    void testBindDataConvertsSingleIncludeToListInternally() {
	    Class testClass = parseTestBean()
	    def bean = testClass.newInstance()
	    def params = ['a.name':"beanName", 'b.address':"address", 'a.pages':3]
	    def controller = appCtx.getBean("TestController")
	    controller.bindData(bean, params, [include:"name"], "a")
	    assertEquals(0, bean.pages)
	    assertEquals("beanName", bean.name)
	    assertNull(bean.address)
	}

	void testBeansWhenNotWarDeployedAndDevelopmentEnv() {
        try {
            System.setProperty("grails.env", "development")
            def mock = [application: [config: new ConfigObject(), warDeployed: false]]
            def plugin = new ControllersGrailsPlugin()
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

	Class parseTestBean(){
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
