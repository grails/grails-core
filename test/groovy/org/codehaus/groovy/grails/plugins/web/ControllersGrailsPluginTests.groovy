package org.codehaus.groovy.grails.plugins.web

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.web.plugins.*

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