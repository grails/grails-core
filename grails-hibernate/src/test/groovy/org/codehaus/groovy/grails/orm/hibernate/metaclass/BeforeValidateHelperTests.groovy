package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.mock.interceptor.MockFor 
import org.codehaus.groovy.grails.orm.hibernate.metaclass.BeforeValidateHelper;


class BeforeValidateHelperTests extends GroovyTestCase {

	def beforeValidateHelper
	
	void setUp() {
		super.setUp()
		beforeValidateHelper = new BeforeValidateHelper()	
	}
	
	void testNoArgBeforeValidate() {
		def obj = new ClassWithNoArgBeforeValidate()
		assertEquals 'wrong initial counter value', 0, obj.noArgCounter
		beforeValidateHelper.invokeBeforeValidate(obj, null)
		assertEquals 'wrong counter value', 1, obj.noArgCounter
		beforeValidateHelper.invokeBeforeValidate(obj, [])
		assertEquals 'wrong counter value', 2, obj.noArgCounter
		beforeValidateHelper.invokeBeforeValidate(obj, ['name', 'age', 'town'])
		assertEquals 'wrong counter value', 3, obj.noArgCounter
	}
	
	void testListArgBeforeValidate() {
		def obj = new ClassWithListArgBeforeValidate()
		assertEquals 'wrong initial counter value', 0, obj.listArgCounter
		beforeValidateHelper.invokeBeforeValidate(obj, null)
		assertEquals 'wrong counter value', 1, obj.listArgCounter
		beforeValidateHelper.invokeBeforeValidate(obj, [])
		assertEquals 'wrong counter value', 2, obj.listArgCounter
		beforeValidateHelper.invokeBeforeValidate(obj, ['name', 'age', 'town'])
		assertEquals 'wrong counter value', 3, obj.listArgCounter
	}
	
	void testOverloadedBeforeValidate() {
		def obj = new ClassWithOverloadedBeforeValidate()
		assertEquals 'wrong initial list arg counter value', 0, obj.listArgCounter
		assertEquals 'wrong initial no arg counter value', 0, obj.noArgCounter
		
		beforeValidateHelper.invokeBeforeValidate(obj, null)
		assertEquals 'wrong list arg counter value', 0, obj.listArgCounter
		assertEquals 'wrong no arg counter value', 1, obj.noArgCounter
		
		beforeValidateHelper.invokeBeforeValidate(obj, [])
		assertEquals 'wrong list arg counter value', 1, obj.listArgCounter
		assertEquals 'wrong no arg counter value', 1, obj.noArgCounter

		beforeValidateHelper.invokeBeforeValidate(obj, ['name', 'age', 'town'])
		assertEquals 'wrong list arg counter value', 2, obj.listArgCounter
		assertEquals 'wrong no arg counter value', 1, obj.noArgCounter
	}
}

class ClassWithNoArgBeforeValidate {
	def noArgCounter = 0
	def beforeValidate(){
		++noArgCounter
	}
}

class ClassWithListArgBeforeValidate {
	def listArgCounter = 0
	def beforeValidate(List properties) {
		++listArgCounter
	}
}

class ClassWithOverloadedBeforeValidate {
	def noArgCounter = 0
	def listArgCounter = 0
	def beforeValidate(){
		++noArgCounter
	}
	def beforeValidate(List properties) {
		++listArgCounter
	}
}
