/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 11, 2008
 */
package org.codehaus.groovy.grails.orm.hibernate
class CustomCascadeMappingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class CustomCascadeMappingOne {
    Long id
    Long version

    Set foos = new HashSet()
    Set bars = new HashSet()
    static hasMany = [foos:CustomCascadeMappingTwo, bars:CustomCascadeMappingTwo]

    static mapping = {
        foos cascade:'none', joinTable:'foos'
        bars cascade:'all', joinTable:'bars'
    }
}

class CustomCascadeMappingTwo {
    Long id
    Long version

    String name
}
'''
    }


    void testCascadingBehaviour() {
        def oneClass = ga.getDomainClass("CustomCascadeMappingOne").clazz
        def twoClass = ga.getDomainClass("CustomCascadeMappingTwo").clazz

        def one = oneClass.newInstance()

        shouldFail {
            one.addToFoos(name:"foo1")
                    .addToFoos(name:"foo2")
                    .save(flush:true)
        }
        one.foos.clear()
       one.addToBars(name:"bar1")
            .addToBars(name:"bar2")
            .save(flush:true)


        session.clear()

       one = oneClass.get(1)

            assertEquals 0, one.foos.size()
            assertEquals 2, one.bars.size()
    }
}