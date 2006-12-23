package org.codehaus.groovy.grails.commons.metaclass;



class ThreadManagedMetaBeanPropertyTests extends GroovyTestCase {
	
	void testGetterName() {
		def bp = new ThreadManagedMetaBeanProperty(ThreadManagedMetaBeanPropertyTests.class,"myProp", String.class, "hello")
		
		assertEquals "getMyProp", bp.getter.name
	}
	
	void testSetterName() {
		def bp = new ThreadManagedMetaBeanProperty(ThreadManagedMetaBeanPropertyTests.class,"myProp", String.class, "hello")
		
		assertEquals "setMyProp", bp.setter.name
		
	}
	
	void testGetter() {
		def bp = new ThreadManagedMetaBeanProperty(ThreadManagedMetaBeanPropertyTests.class,"myProp", String.class, "hello")
		
		assertEquals "hello", bp.getter.invoke(this, [] as Object[])
		
	}

	void testSetter() {
		def bp = new ThreadManagedMetaBeanProperty(ThreadManagedMetaBeanPropertyTests.class,"myProp", String.class, "hello")
		bp.setter.invoke(this, ["goodbye"] as Object[])
		assertEquals "goodbye", bp.getter.invoke(this, [] as Object[])
		
	}
	
}