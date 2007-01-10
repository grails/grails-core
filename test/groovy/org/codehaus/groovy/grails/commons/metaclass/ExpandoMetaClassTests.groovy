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
package org.codehaus.groovy.grails.commons.metaclass;

/**
 * @author Graeme Rocher
 */

class ExpandoMetaClassTests extends GroovyTestCase {
	
	void testAllowAdditionOfProperties() {
		 def metaClass = new ExpandoMetaClass(Test.class)
		 metaClass.allowChangesAfterInit = true
		 
		 metaClass.getOne << {->
			 "testme"
		 }
		 metaClass.initialize()
		 try {
			 metaClass.getTwo << {->
				 "testagain"
			 }			 
		 }
		 catch(RuntimeException e) {
			 fail("Should have allowed addition of new method")	
		 }
		 
		 def t = new Test()
		 t.metaClass = metaClass
		 
		 assertEquals "testme", t.one
		 assertEquals "testagain", t.two
		
	}
	
	void testAllowAdditionOfMethods() {
		 def metaClass = new ExpandoMetaClass(Test.class)
		 metaClass.allowChangesAfterInit = true
		 
		 metaClass.myMethod << {->
			 "testme"
		 }
		 metaClass.initialize()
		 try {
			 metaClass.mySecondMethod << {->
				 "testagain"
			 }			 
		 }
		 catch(RuntimeException e) {
			 fail("Should have allowed addition of new method")	
		 }
		 
		 def t = new Test()
		 t.metaClass = metaClass
		 
		 assertEquals "testme", t.myMethod()
		 assertEquals "testagain", t.mySecondMethod()
	}
	

	
	void testForbiddenAdditionOfMethods() {
		 def metaClass = new ExpandoMetaClass(Test.class)
		 
		 metaClass.myMethod << {
			 "testme"
		 }
		 
		 def t = new Test()
		 metaClass.initialize()

		 
		 try {
			 metaClass.mySecondMethod << {
				 "testagain"
			 }
			 fail("Should have thrown exception")
		 }
		 catch(RuntimeException e) {
			 // expected
		 }			 
	}	
	
	void testPropertyGetterWithClosure() {
	   	 def metaClass = new ExpandoMetaClass(Test.class)
		 
		 metaClass.getSomething = {-> "testme" }
	   	 
	   	metaClass.initialize()
		 
		 def t = new Test()
	   	 t.metaClass = metaClass

		 assertEquals "testme", t.getSomething()    	
		 assertEquals "testme", t.something 
	   	 
	   	 
	}
	
	void testPropertySetterWithClosure() {
	   	 def metaClass = new ExpandoMetaClass(Test.class)
		 
	   	 def testSet = null
		 metaClass.setSomething = {String txt-> testSet = txt }
	   	 
	   	metaClass.initialize()
		 
		 def t = new Test()
	   	 t.metaClass = metaClass

	   	 t.something = "testme"
		 assertEquals "testme", testSet
		 
		 t.setSomething("test2")
		 assertEquals "test2", testSet

	}
	
	void testMethodOverloading() {
	   	 def metaClass = new ExpandoMetaClass(Test.class)
		 
		metaClass.overloadMe << { String txt -> txt } << { Integer i -> i }
	   	 
	   	metaClass.initialize()
		 
		 def t = new Test()
	   	 t.metaClass = metaClass
	   	 
	   	 assertEquals "test", t.overloadMe("test")
	   	 assertEquals 10, t.overloadMe(10)
		
	}
	
	void testNewPropertyMethod() {
	   	 def metaClass = new ExpandoMetaClass(Test.class)
		 
		 metaClass.something = "testme"
	   	 
	   	metaClass.initialize()
		 
		 def t = new Test()
	   	 t.metaClass = metaClass
	   	 
		 assertEquals "testme", t.getSomething()    	
		 assertEquals "testme", t.something 
		 
		 t.something = "test2"
		 assertEquals "test2", t.something
		 assertEquals "test2", t.getSomething()
		 
		 def t2 = new Test()
	   	 t2.metaClass = metaClass
	   	 // now check that they're not sharing the same property!
	   	 assertEquals "testme", t2.something
	   	 assertEquals "test2", t.something
	   	 
	   	 t2.setSomething("test3")
	   	 
	   	 assertEquals "test3", t2.something
	}
	
	void testCheckFailOnExisting() {
		def metaClass = new ExpandoMetaClass(Test.class)
		 try {
			 metaClass.existing << { ->
				 "should fail. already exists!"
			 }			 
			 fail("Should have thrown exception when method already exists")			 
		 }
		 catch(Exception e) {
			 // expected
		 }

	}
	
	void testCheckFailOnExistingConstructor() {
		def metaClass = new ExpandoMetaClass(Test.class)
		 try {
			 metaClass.ctor << { ->
				 "should fail. already exists!"
			 }			 
			 fail("Should have thrown exception when method already exists")			 
		 }
		 catch(Exception e) {
			 // expected
		 }
		
	}
	
	void testCheckFailOnExistingStaticMethod() {
		def metaClass = new ExpandoMetaClass(Test.class)
		 try {
			 metaClass.'static'.existingStatic << { ->
				 "should fail. already exists!"
			 }			 
			 fail("Should have thrown exception when method already exists")			 
		 }
		 catch(Exception e) {
			 // expected
		 }

	}	
	
    void testNewStaticMethod() {
    	 def metaClass = new ExpandoMetaClass(Test.class, true)
    	 
    	 metaClass.'static'.myStaticMethod << { String txt ->
    		 "testme"
    	 }
    	 metaClass.initialize()
    	 
    	 assertEquals "testme", Test.myStaticMethod("blah")
    	 
    }
    
    void testReplaceStaticMethod() {
	   	 def metaClass = new ExpandoMetaClass(Test.class, true)
		 
		 metaClass.'static'.existingStatic = { ->
			 "testme"
		 }
		 metaClass.initialize()
		 
		 assertEquals "testme", Test.existingStatic()
    	
    }
    
    void testNewZeroArgumentStaticMethod() {
	   	 def metaClass = new ExpandoMetaClass(Test.class, true)
		 
		 metaClass.'static'.myStaticMethod = { ->
			 "testme"
		 }
	   	metaClass.initialize()
		 
		 assertEquals "testme", Test.myStaticMethod()    	
    }
    
	void testNewInstanceMethod() {
		 def metaClass = new ExpandoMetaClass(Test.class)
		 
		 metaClass.myMethod << {
			 "testme"
		 }
		 
		 def t = new Test()
		 metaClass.initialize()
		 t.metaClass = metaClass
		 
		 assertEquals "testme", t.myMethod()
		 		 
	}
	

	void testNewConstructor() {
	   	 def metaClass = new ExpandoMetaClass(Test.class, true)
		 
		 metaClass.ctor << { String txt ->
			 def t = Test.class.newInstance()
			 t.name = txt
			 return t
		 }
	   	 
	   	metaClass.initialize()
		 
		 def t = new Test("testme")
		 assert t
		 assertEquals "testme", t.name    	
		
	}
	
	void testReplaceConstructor() {
	   	 def metaClass = new ExpandoMetaClass(Test.class, true)
		 
		 metaClass.ctor = { ->
			 def t = Test.class.newInstance()
			 t.name = 'testme'
			 return t
		 }
		 
	   	metaClass.initialize()
	   	
		 def t = new Test()
		 assert t
		 assertEquals "testme", t.name    	
		
	}
	

	
	void testReplaceInstanceMethod() {
		 def metaClass = new ExpandoMetaClass(Test.class)
		 
		 metaClass.existing2 = { Object i ->
			 "testme"
		 }
		 metaClass.initialize()
		 
		 def t = new Test()
		 t.metaClass = metaClass
		
		 def var = 1
		 assertEquals "testme", t.existing2(var)
	}
	
	void testBorrowMethodFromAnotherClass() {
		 def metaClass = new ExpandoMetaClass(Test.class)
		 
		 def a = new Another()
		 metaClass.borrowMe = a.&another
		 
		 def t = new Test()
		 t.metaClass = metaClass
		 metaClass.initialize()
		 
		 assertEquals "mine blah!", t.borrowMe("blah")
		
	}
	
	void testBorrowByName() {
		 def metaClass = new ExpandoMetaClass(Test.class)
		 
		 def a = new Another()
		 metaClass.borrowMe = a.&'another'
		 
		 def t = new Test()
		 t.metaClass = metaClass
		 metaClass.initialize()
		 
		 assertEquals "mine blah!", t.borrowMe("blah")
		
	}
	

}

class Test {
	String name
	
	def existing2(obj) {
		"hello2!"
	}
	def existing() {
		"hello!"
	}
	
	static existingStatic() {
		"I exist"
	}
}
class Another {
   def another(txt) {
	   "mine ${txt}!"
   }
}