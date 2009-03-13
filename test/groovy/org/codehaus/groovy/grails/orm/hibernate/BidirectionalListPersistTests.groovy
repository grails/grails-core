package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Session

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 12, 2008
*/
class BidirectionalListPersistTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
class TestFaqSection
{
    Long id
    Long version
    String title
    List elements
    static hasMany = [elements:TestFaqElement]
}
class TestFaqElement
{
    Long id
    Long version
    String question
    String answer
    TestFaqSection section
}
'''
    }

      void testListPersisting() {
        def sectionClass = ga.getDomainClass("TestFaqSection")
          def section = sectionClass.newInstance()

        section.title = "foo"
        def element = ga.getDomainClass("TestFaqElement").newInstance()
        element.question = "question 1"
        element.answer = "the answer"
        section.elements = [element]



        session.save section

        session.flush()

        session.clear()

        section = session.get(sectionClass.getClazz(),1L)

        assert section
        assertEquals 1, section.elements.size()
    }
    
}