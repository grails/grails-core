package org.codehaus.groovy.grails.commons.metaclass;

import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.mock.web.MockHttpServletRequest

class DynamicConstructorTests extends GroovyTestCase {

	void testDynamicConstructor() {
		def gcl = new GroovyClassLoader()
		def clz = gcl.parseClass("""
class Test {
	byte[] bytes	
	URL url		
}
				""")
		
	    def go = clz.newInstance()
	    def pmc = ProxyMetaClass.getInstance(clz)
		
		def i = new GroovyDynamicMethodsInterceptor(go)
		pmc.interceptor = i
		i.addDynamicConstructor( new org.codehaus.groovy.grails.web.metaclass.DataBindingDynamicConstructor() )
		
		def registry = GroovySystem.metaClassRegistry
		registry.setMetaClass(clz, pmc)
		
		def script = gcl.parseClass("""
t = new Test(params)				
	    """).newInstance()
	    script.params = [bytes: 'blah',
						 url: 'http://grails.org']
	    
	    def t = script.run()
	    assert t != null
	    assert t.url == new URL('http://grails.org')
	    assert t.bytes != null
		
	}

}
