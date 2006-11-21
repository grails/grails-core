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
package grails.spring;
/**
 * @author Graeme Rocher
 * @since 0.4
 *
 */
class BeanBuilderTests extends GroovyTestCase {

	void testSimpleBean() {
		def bb = new BeanBuilder()
		bb.beans {
			bean1(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:true, height:"1.8m"]
				children = ["bart", "lisa"]				         
			}
		}
		def ctx  = bb.createApplicationContext()
		
		assert ctx.containsBean("bean1")
		def bean1 = ctx.getBean("bean1")
		
		assertEquals "homer", bean1.person
		assertEquals 45, bean1.age
		assertEquals true, bean1.props?.overweight
		assertEquals "1.8m", bean1.props?.height
		assertEquals(["bart", "lisa"], bean1.children)
		
	}
	
	void testWithAnonymousInnerBean() {
		def bb = new BeanBuilder()
		bb.beans {
			marge(Bean2) {
				person = "marge"
				bean1 =  { Bean1 b ->
				     	    person = "homer"
						    age = 45
						    props = [overweight:true, height:"1.8m"]
						    children = ["bart", "lisa"] }
				children = [bart, lisa]
			}
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9				
			}
		}
		
		def ctx  = bb.createApplicationContext()
		
		def marge = ctx.getBean("marge")
		
		assertEquals "homer", marge.bean1.person
	}
	
	void testBeanReferences() {
		def bb = new BeanBuilder()
		bb.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:true, height:"1.8m"]
				children = ["bart", "lisa"]				         
			}
			marge(Bean2) {
				person = "marge"
				bean1 = homer
				children = [bart, lisa]
			}
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9				
			}
		}
		def ctx  = bb.createApplicationContext()
		
		def homer = ctx.getBean("homer")
		def marge = ctx.getBean("marge")
		def bart = ctx.getBean("bart")
		def lisa = ctx.getBean("lisa")
		
		assertEquals homer, marge.bean1
		assertEquals 2, marge.children.size()
		
		assertTrue marge.children.contains(bart)
		assertTrue marge.children.contains(lisa)
	}
	
	void testBeanWithConstructor() {
		def bb = new BeanBuilder()
		bb.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			marge(Bean3, "marge", homer) {
				age = 40				
			}
		}	
		def ctx  = bb.createApplicationContext()
		
		def marge = ctx.getBean("marge")
		
		assertEquals "marge", marge.person
		assertEquals "homer", marge.bean1.person
		assertEquals 40, marge.age
	}
	
	void testBeanWithFactoryMethod() {
		def bb = new BeanBuilder()
		bb.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			def marge = marge(Bean4) {
				person = "marge"				
			}
			marge.factoryMethod = "getInstance"
		}	
		def ctx  = bb.createApplicationContext()
		
		def marge = ctx.getBean("marge")
		
		assert "marge", marge.person

	}
	
	void testLoadExternalBeans() {
		def pr = new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
		def r = pr.getResource("grails/spring/resources1.groovy")
		
		def bb = new BeanBuilder(r)
		
		def ctx = bb.createApplicationContext()
		
		assert ctx.containsBean("dataSource")
		
		def dataSource = ctx.getBean("dataSource")
		
	}

}
// simple bean
class Bean1 {
	String person
	int age
	Properties props
	List children
}
// bean referencing other bean
class Bean2 {
	String person
	Bean1 bean1
	Properties props
	List children	
}
// bean with constructor args
class Bean3 {
	Bean3(String person, Bean1 bean1) {
		this.person = person
		this.bean1 = bean1
	}
	String person
	Bean1 bean1
	int age
}
// bean with factory method
class Bean4 {
	private Bean4() {}
	static Bean4 getInstance() {
		return new Bean4()
	}
	String person
}