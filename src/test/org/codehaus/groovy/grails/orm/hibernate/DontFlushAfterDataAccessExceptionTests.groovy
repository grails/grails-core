package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FlushMode
import org.springframework.dao.DataAccessException

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DontFlushAfterDataAccessExceptionTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class DontFlushAfterDataAccessExceptionAuthor {

   static hasMany = [books: DontFlushAfterDataAccessExceptionBook]

   String name

   static mapping = {
       columns {
           books cascade: 'save-update'
       }
   }
}

@Entity
class DontFlushAfterDataAccessExceptionBook {

   static belongsTo = [author: DontFlushAfterDataAccessExceptionAuthor]

   String name
}
'''
    }

    void testDontFlushAfterDataAccessException() {
        def Author = ga.getDomainClass("DontFlushAfterDataAccessExceptionAuthor").clazz

        session.setFlushMode(FlushMode.AUTO)
        assertNotNull Author.newInstance(name:"bob")
                            .addToBooks(name:"my story")
                            .save(flush:true)

        assertEquals FlushMode.AUTO, session.getFlushMode()

        session.clear()

        def author = Author.get(1)

        shouldFail(DataAccessException) {
            author.delete(flush:true)
        }

        assertEquals FlushMode.MANUAL, session.getFlushMode()
    }
}
