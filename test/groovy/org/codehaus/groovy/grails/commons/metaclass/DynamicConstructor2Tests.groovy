package org.codehaus.groovy.grails.commons.metaclass;

import org.codehaus.groovy.grails.orm.hibernate.*

class DynamicConstructor2Tests extends AbstractGrailsHibernateTests {

	void onSetUp() {
		gcl.parseClass('''
class Test {
  String name
  static constraints = {
	  name(size:5..15)
  }
}    
''')

	}
	
	void testDynamicConstructor() {
		def script = gcl.parseClass("""
t = new Test(params)				
	    """).newInstance()
	    script.params = [name: 'blah']
	    
	    def t = script.run()
	    assertEquals "blah", t.name
		
	}

	void onTearDown() {
		
	}
}
