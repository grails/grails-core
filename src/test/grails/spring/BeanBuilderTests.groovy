/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *                                                              
 *      http://www.apache.org/licenses/LICENSE-2.0             s
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.spring

import org.springframework.aop.SpringProxy
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.mock.jndi.SimpleNamingContextBuilder
import org.springframework.stereotype.Component

/**
 * @author Graeme Rocher
 * @since 0.4
 *
 */

class BeanBuilderTests extends GroovyTestCase {

	void testImportSpringXml() {
		def bb = new grails.spring.BeanBuilder()

		bb.beans {
			importBeans "classpath:grails/spring/test.xml"
		}

		def ctx = bb.createApplicationContext()

		def foo = ctx.getBean("foo")
		assertEquals "hello", foo
	}

	void testImportBeansFromGroovy() {
		def bb = new grails.spring.BeanBuilder()

		bb.beans {
			importBeans "file:test/resources/spring/test.groovy"
		}

		def ctx = bb.createApplicationContext()

		def foo = ctx.getBean("foo")
		assertEquals "hello", foo		
	}

    void testInheritPropertiesFromAbstractBean() {
        def bb = new grails.spring.BeanBuilder()

        bb.beans {
            myB(Bean1){
                person = "wombat"
            }

            myAbstractA(Bean2){ bean ->
                bean.'abstract' = true
                age = 10
                bean1 = myB
            }
            myConcreteB {  
                it.parent = myAbstractA
            }
        }

        def ctx = bb.createApplicationContext()
        def bean = ctx.getBean("myConcreteB")

        assertEquals 10, bean.age  
        assertNotNull bean.bean1
    }

    void testContextComponentScanSpringTag() {
        def bb = new BeanBuilder()

        bb.beans {
            xmlns context:"http://www.springframework.org/schema/context"

            context.'component-scan'( 'base-package' :"grails.spring" )
        }

        def appCtx = bb.createApplicationContext()

        def p = appCtx.getBean("person")

        assertTrue( p instanceof AdvisedPerson )
        assertNotNull p
    }

    void testUseSpringNamespaceAsMethod() {
        def bb = new BeanBuilder()

        bb.beans {
            xmlns aop:"http://www.springframework.org/schema/aop"

            fred(AdvisedPerson) {
                name = "Fred"
                age = 45
            }
            birthdayCardSenderAspect(BirthdayCardSender)

            aop {
                config("proxy-target-class":true) {
                    aspect( id:"sendBirthdayCard",ref:"birthdayCardSenderAspect" ) {
                        after method:"onBirthday", pointcut: "execution(void grails.spring.AdvisedPerson.birthday()) and this(person)"
                    }
                }
            }
        }


        def appCtx = bb.createApplicationContext()
        def fred = appCtx.getBean("fred")
        assertTrue (fred instanceof SpringProxy )


        fred.birthday()

        BirthdayCardSender birthDaySender = appCtx.getBean("birthdayCardSenderAspect")

        assertEquals 1, birthDaySender.peopleSentCards.size()
        assertEquals "Fred", birthDaySender.peopleSentCards[0].name
    }

    void testUseTwoSpringNamespaces() {
       def bb = new BeanBuilder()

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder()
        try {

            builder.bind("bar", "success")
            builder.activate()
            TestScope scope = new TestScope()

            GenericApplicationContext appCtx = bb.getSpringConfig().getUnrefreshedApplicationContext()
            appCtx.getBeanFactory().registerScope("test", scope)
            bb.beans {
                xmlns aop:"http://www.springframework.org/schema/aop"
                xmlns jee:"http://www.springframework.org/schema/jee"
                scopedList(ArrayList) { bean ->
                    bean.scope = "test"
                    aop.'scoped-proxy'()
                }

                jee.'jndi-lookup'(id:"foo", 'jndi-name':"bar")

            }

            appCtx = bb.createApplicationContext()

            assertEquals "success", appCtx.getBean("foo")

            assertNotNull appCtx.getBean("scopedList")
            assertNotNull appCtx.getBean("scopedList").size()
            assertNotNull appCtx.getBean("scopedList").size()

            // should only be true because bean not initialized until proxy called
            assertEquals 2, scope.instanceCount

            bb = new BeanBuilder()

            appCtx = bb.getSpringConfig().getUnrefreshedApplicationContext()
            appCtx.getBeanFactory().registerScope("test", scope)
            bb.beans {
                xmlns aop:"http://www.springframework.org/schema/aop",
                      jee:"http://www.springframework.org/schema/jee"
                scopedList(ArrayList) { bean ->
                    bean.scope = "test"
                    aop.'scoped-proxy'()
                }

                jee.'jndi-lookup'(id:"foo", 'jndi-name':"bar")

            }
            appCtx = bb.createApplicationContext()

            assertEquals "success", appCtx.getBean("foo")

            assertNotNull appCtx.getBean("scopedList")
            assertNotNull appCtx.getBean("scopedList").size()
            assertNotNull appCtx.getBean("scopedList").size()

            // should only be true because bean not initialized until proxy called
            assertEquals 4, scope.instanceCount


        }
        finally {
            builder.deactivate()
        }
    }

    void testSpringAOPSupport() {


        def bb = new BeanBuilder()

        bb.beans {
            xmlns aop:"http://www.springframework.org/schema/aop"

            fred(AdvisedPerson) {
                name = "Fred"
                age = 45
            }
            birthdayCardSenderAspect(BirthdayCardSender)

            aop.config("proxy-target-class":true) {
                aspect( id:"sendBirthdayCard",ref:"birthdayCardSenderAspect" ) {
                    after method:"onBirthday", pointcut: "execution(void grails.spring.AdvisedPerson.birthday()) and this(person)"
                }                
            }
        }


        def appCtx = bb.createApplicationContext()
        def fred = appCtx.getBean("fred")
        assertTrue (fred instanceof SpringProxy )


        fred.birthday()

        BirthdayCardSender birthDaySender = appCtx.getBean("birthdayCardSenderAspect")

        assertEquals 1, birthDaySender.peopleSentCards.size()
        assertEquals "Fred", birthDaySender.peopleSentCards[0].name

    }

    void testSpringScopedProxyBean() {
        def bb = new BeanBuilder()

        GenericApplicationContext appCtx = bb.getSpringConfig().getUnrefreshedApplicationContext()
        TestScope scope = new TestScope()
        appCtx.getBeanFactory().registerScope("test", scope)
        bb.beans {
            xmlns aop:"http://www.springframework.org/schema/aop"
            scopedList(ArrayList) { bean ->
                bean.scope = "test"
                aop.'scoped-proxy'()
            }
        }

        appCtx = bb.createApplicationContext()


        assertNotNull appCtx.getBean("scopedList")
        assertNotNull appCtx.getBean("scopedList").size()
        assertNotNull appCtx.getBean("scopedList").size()

        // should only be true because bean not initialized until proxy called
        assertEquals 2, scope.instanceCount
    }

    void testSpringNamespaceBean() {
        def bb = new grails.spring.BeanBuilder()

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder()
        try {

            builder.bind("bar", "success")
            builder.activate()

            bb.beans {
                xmlns jee:"http://www.springframework.org/schema/jee"
                jee.'jndi-lookup'(id:"foo", 'jndi-name':"bar")
            }

            ApplicationContext appCtx = bb.createApplicationContext()

            assertEquals "success", appCtx.getBean("foo")            
        }
        finally {
            builder.deactivate()
        }
    }

    void testNamedArgumentConstructor() {
        def bb = new grails.spring.BeanBuilder()
        bb.beans {
            holyGrail(HolyGrailQuest)
            knights(KnightOfTheRoundTable, "Camelot", leader:"lancelot", quest: holyGrail)
        }


        def ctx = bb.createApplicationContext()

        KnightOfTheRoundTable knights = ctx.getBean("knights")
        HolyGrailQuest quest = ctx.getBean("holyGrail")

        assertEquals "Camelot", knights.name
        assertEquals "lancelot", knights.leader
        assertEquals quest, knights.quest
    }

    void testAbstractBeanDefinition() {      
          def bb = new grails.spring.BeanBuilder()
          bb.beans {
              abstractBean {
                  leader = "Lancelot"
              }
              quest(HolyGrailQuest)
              knights(KnightOfTheRoundTable, "Camelot") { bean ->
                  bean.parent = abstractBean
                  quest = quest
              }
          }
          def ctx = bb.createApplicationContext()

          def knights = ctx.knights   
          assert knights
          shouldFail(org.springframework.beans.factory.BeanIsAbstractException) {
              ctx.abstractBean
          }
          assertEquals "Lancelot", knights.leader
    }

    void testAbstractBeanDefinitionWithClass() {
          def bb = new grails.spring.BeanBuilder()
          bb.beans {                                          
              abstractBean(KnightOfTheRoundTable) { bean ->
                  bean.'abstract' = true                  
                  leader = "Lancelot"
              }
              quest(HolyGrailQuest)
              knights("Camelot") { bean ->
                  bean.parent = abstractBean
                  quest = quest
              }
          }
          def ctx = bb.createApplicationContext()

          shouldFail(org.springframework.beans.factory.BeanIsAbstractException) {
              ctx.abstractBean                                                         
          }                                
          def knights = ctx.knights
          assert knights
          assertEquals "Lancelot", knights.leader
    }


    void testScopes() {
        def bb = new BeanBuilder()
        bb.beans {
            myBean(ScopeTest) { bean ->
                bean.scope = "prototype"
            }
            myBean2(ScopeTest)
        }
        def ctx = bb.createApplicationContext()

        def b1 = ctx.myBean
        def b2 = ctx.myBean

        assert b1 != b2

        b1 = ctx.myBean2
        b2 = ctx.myBean2

        assertEquals b1, b2
    }

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
	
	void testBeanWithParentRef() {
		def bb = new BeanBuilder()
		bb.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:true, height:"1.8m"]
				children = ["bart", "lisa"]				         
			}
		}
		bb = new BeanBuilder(bb.createApplicationContext())
		bb.beans {
			bart(Bean2) {
				person = "bart"
				parent = ref("homer", true)
			}
		}
		
		def ctx = bb.createApplicationContext()
        assert ctx != null		
		assert ctx.containsBean("bart")
		def bart = ctx.getBean("bart")
		assertEquals "homer",bart.parent?.person
	}
	
	void testWithAnonymousInnerBean() {
		def bb = new BeanBuilder()
		bb.beans {
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9				
			}			
			marge(Bean2) {
				person = "marge"
				bean1 =  { Bean1 b ->
				     	    person = "homer"
						    age = 45
						    props = [overweight:true, height:"1.8m"]
						    children = ["bart", "lisa"] }
				children = [bart, lisa]
			}
		}
		
		def ctx  = bb.createApplicationContext()
		
		def marge = ctx.getBean("marge")
		
		assertEquals "homer", marge.bean1.person
	}
	
	void testWithUntypedAnonymousInnerBean() {
		def bb = new BeanBuilder()
		bb.beans {
			homer(Bean1Factory) 
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9				
			}			
			marge(Bean2) {
				person = "marge"
				bean1 =  { bean -> 
							bean.factoryBean = "homer"
							bean.factoryMethod = "newInstance"
							person = "homer" }
				children = [bart, lisa]
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
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9				
			}
			marge(Bean2) {
				person = "marge"
				bean1 = homer
				children = [bart, lisa]
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
	
	void testBeanWithFactoryMethodUsingClosureArgs() {
		def bb = new BeanBuilder()
		bb.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			marge(Bean4) { bean ->
			    bean.factoryMethod = "getInstance"
				person = "marge"				
			}
		}	
		def ctx  = bb.createApplicationContext()
		
		def marge = ctx.getBean("marge")
		
		assert "marge", marge.person		
	}

    void testGetBeanDefinitions() {
        def bb = new BeanBuilder()
        bb.beans {
            jeff(Bean1) {
                person = 'jeff'
            }
            graeme(Bean1) {
                person = 'graeme'
            }
            guillaume(Bean1) {
                person = 'guillaume'
            }
        }

        def beanDefinitions = bb.beanDefinitions
        assertNotNull 'beanDefinitions was null', beanDefinitions
        assertEquals 'beanDefinitions was the wrong size', 3, beanDefinitions.size()
        assertNotNull 'beanDefinitions did not contain jeff', beanDefinitions['jeff']
        assertNotNull 'beanDefinitions did not contain guillaume', beanDefinitions['guillaume']
        assertNotNull 'beanDefinitions did not contain graeme', beanDefinitions['graeme']
    }

    void testBeanWithFactoryBean() {
		def bb = new BeanBuilder()
		bb.beans {
			myFactory(Bean1Factory)
			
			homer(myFactory) { bean ->
				bean.factoryMethod = "newInstance"
				person = "homer"
				age = 45
			}
		}
		
		def ctx  = bb.createApplicationContext()
		
		def homer = ctx.getBean("homer")
		
		assertEquals "homer", homer.person
	}
	
	void testBeanWithFactoryBeanAndMethod() {
		def bb = new BeanBuilder()
		bb.beans {
			myFactory(Bean1Factory)
			
			homer(myFactory:"newInstance") { bean ->
				person = "homer"
				age = 45
			}
		}
		
		def ctx  = bb.createApplicationContext()
		
		def homer = ctx.getBean("homer")
		
		assertEquals "homer", homer.person
	}	
	
	void testLoadExternalBeans() {
		def pr = new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
		def r = pr.getResource("grails/spring/resources1.groovy")
		
		def bb = new BeanBuilder()
		bb.loadBeans(r)
		
		def ctx = bb.createApplicationContext()
		
		assert ctx.containsBean("dataSource")
		
		def dataSource = ctx.getBean("dataSource")
		
	}
	
	void testHolyGrailWiring() {

		def bb = new grails.spring.BeanBuilder()

		bb.beans {
		 quest(HolyGrailQuest) 

		 knight(KnightOfTheRoundTable, "Bedivere") {
		   quest = ref("quest")
		 }
		}

		def ctx = bb.createApplicationContext()

		def knight = ctx.getBean("knight")

		knight.embarkOnQuest()
		
	}

    void testAbstractBeanSpecifyingClass() {
        def bb = new BeanBuilder()

        bb.beans {
            abstractKnight(KnightOfTheRoundTable) { bean ->
                bean.'abstract' = true
                leader = "King Arthur"
            }

            lancelot("lancelot") { bean ->
                bean.parent = ref("abstractKnight")
            }

            abstractPerson(Bean1) { bean ->
                bean.'abstract'=true
                age = 45
            }
            homerBean { bean ->
                bean.parent = ref("abstractPerson")
                person = "homer"
            }
        }


        def ctx = bb.createApplicationContext()

        def lancelot = ctx.getBean("lancelot")
        assertEquals "King Arthur", lancelot.leader
        assertEquals "lancelot", lancelot.name

        def homerBean = ctx.getBean("homerBean")

        assertEquals 45, homerBean.age
        assertEquals "homer", homerBean.person 
    }

	void testBeanBuilderWithScript() {
        def script = '''
def bb = new grails.spring.BeanBuilder()

bb.beans {
quest(grails.spring.HolyGrailQuest) {}

knight(grails.spring.KnightOfTheRoundTable, "Bedivere") { quest = quest }
}
bb.createApplicationContext()
 '''                                                                                
        def ctx = new GroovyShell().evaluate(script)

        def knight = ctx.getBean('knight')
        knight.embarkOnQuest()
    }

    // test for GRAILS-5057
    void testRegisterBeans() {
        def bb = new BeanBuilder()

        bb.beans {
           personA(AdvisedPerson) {
               name = "Bob"
           }
        }

        def appCtx = bb.createApplicationContext()

        assertEquals "Bob", appCtx.getBean("personA").name

        bb = new BeanBuilder()
        bb.beans {
            personA(AdvisedPerson) {
                name = "Fred"
            }
        }
        bb.registerBeans(appCtx)

        assertEquals "Fred", appCtx.getBean("personA").name
    }

}
class HolyGrailQuest {
	   void start() { println "lets begin" }
}
class KnightOfTheRoundTable {
   String name
   String leader
   KnightOfTheRoundTable(String n) {
      this.name = n
   }
   HolyGrailQuest quest

   void embarkOnQuest() {
       quest.start()
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
    int age
	String person
	Bean1 bean1
	Properties props
	List children
	Bean1 parent
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
// a factory bean
class Bean1Factory {
	Bean1 newInstance() {
		return new Bean1()
	}
}
class ScopeTest {}
class TestScope implements Scope {

    int instanceCount


    public Object remove(String name) {
         // do nothing
    }

    public void registerDestructionCallback(String name, Runnable callback) {
    }

    public String getConversationId() {
        return "mock"
    }

    public Object get(String name, ObjectFactory<?> objectFactory) {
        instanceCount++
        objectFactory.getObject()

    }

    public Object resolveContextualObject(String s) {
        return null;  // noop
    }
}
class BirthdayCardSender {
   List peopleSentCards = []
   public void onBirthday(AdvisedPerson person) {
      peopleSentCards << person
   }
}
@Component(value = "person")
public class AdvisedPerson {
 int age;
 String name;

 public void birthday() {
      ++age;
 }
}