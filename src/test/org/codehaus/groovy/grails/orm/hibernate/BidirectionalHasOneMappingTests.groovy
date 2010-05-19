package org.codehaus.groovy.grails.orm.hibernate

class BidirectionalHasOneMappingTests extends AbstractGrailsHibernateTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class BidirectionalHasOneFoo {
    static hasOne = [bar:BidirectionalHasOneBar]

    static constraints = {
        bar(nullable:true)
    }
}

@Entity
class BidirectionalHasOneBar {
    BidirectionalHasOneFoo foo
}
'''
    }

    // test for GRAILS-5581
    void testRefreshHasOneAssociation() {
        def Foo = ga.getDomainClass("BidirectionalHasOneFoo")
        def foo = Foo.newInstance()
        foo.save(failOnError:true)
        foo.refresh()
    }
}
