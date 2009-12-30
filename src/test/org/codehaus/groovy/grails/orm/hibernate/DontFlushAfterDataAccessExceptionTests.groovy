package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FlushMode
import org.springframework.dao.DataAccessException

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class DontFlushAfterDataAccessExceptionTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
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
''')
    }


    void testDontFlushAfterDataAccessException() {
        def Author = ga.getDomainClass("DontFlushAfterDataAccessExceptionAuthor").clazz

        session.setFlushMode(FlushMode.AUTO)
        assert Author.newInstance(name:"bob")
                      .addToBooks(name:"my story")
                      .save(flush:true)

        assert session.getFlushMode() == FlushMode.AUTO : "should be a flush mode of auto"

        session.clear()

        def author = Author.get(1)

        shouldFail(DataAccessException) {
            author.delete(flush:true)
        }

        assert session.getFlushMode() == FlushMode.MANUAL : "should be a flush mode of manual after exception!"

    }
}