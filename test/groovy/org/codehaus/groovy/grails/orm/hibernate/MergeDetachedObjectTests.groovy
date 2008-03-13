package org.codehaus.groovy.grails.orm.hibernate

import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.orm.hibernate3.SessionFactoryUtils

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 13, 2008
*/
class MergeDetachedObjectTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
class DetachedQuestion {
    Long id
    Long version
    String name
    Set answers
    static hasMany = [answers:DetachedAnswer]
}
class DetachedAnswer {
    Long id
    Long version
    String name
}
'''
    }


    void testMergeDetachedObject() {

        def questionClass = ga.getDomainClass("DetachedQuestion").clazz

        def question = questionClass
                            .newInstance(name:"What is the capital of France?")
                            .addToAnswers(name:"London")
                            .addToAnswers(name:"Paris")
                            .save(flush:true)


        assert question

        session.clear()

        question = questionClass.get(1)

        TransactionSynchronizationManager.unbindResource(this.sessionFactory);
        SessionFactoryUtils.releaseSession(session, this.sessionFactory);



        session = sessionFactory.openSession()
        TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(session))

        question = question.merge()

        assertEquals 2, question.answers.size()

        question.name = "changed"

        question.save(flush:true)

    }
    

}