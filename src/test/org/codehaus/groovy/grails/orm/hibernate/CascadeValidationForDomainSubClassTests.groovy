package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class CascadeValidationForDomainSubClassTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Book {
   String title
   Integer pages
   static belongsTo = [Author]
   static constraints = { pages(range: 0..100) }
}

@Entity
class Author {
   String name
   Book book
}

@Entity
class Novelist extends Author {
}
''')
    }

    void testCascadingValidation() {
      def Book = ga.getDomainClass("Book").clazz
      def Novelist = ga.getDomainClass("Novelist").clazz

      def b = Book.newInstance(title:'War & Peace', pages:9999) // pages violates range constraint
      def a = Novelist.newInstance(name:'Tolstoy', book:b)
      assert a.validate() == false : "Should have failed validation for subclass!"        
    }

}