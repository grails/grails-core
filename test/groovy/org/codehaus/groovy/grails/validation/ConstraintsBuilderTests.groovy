package org.codehaus.groovy.grails.validation

import  org.codehaus.groovy.grails.commons.test.*
import  org.codehaus.groovy.grails.commons.metaclass.*
import org.springframework.validation.BindException;

class ConstraintsBuilderTests extends AbstractGrailsMockTests {

    void testPrimitiveIntAndMinConstraint() {
         def bookClass = ga.getDomainClass("Book")
         def book = bookClass.newInstance()
         book.title = "foo"

         def bookMetaClass = new ExpandoMetaClass(bookClass.clazz)

         
         def errorsProp = null
         def setter = { Object obj -> errorsProp = obj }

         bookMetaClass.setErrors = setter
         bookMetaClass.initialize()
         book.metaClass = bookMetaClass

        def bookValidator = new GrailsDomainClassValidator()
        

        bookValidator.domainClass = bookClass
        bookValidator.messageSource = createMessageSource()
        bookClass.validator = bookValidator
        
        def errors = new BindException(book, book.class.name)

        bookValidator.validate(book, errors, true)


         assert !errors.hasErrors()
         book.totalSales = -10
        errors = new BindException(book, book.class.name)
        bookValidator.validate(book, errors, true)
         assert errors.hasErrors()
         book.totalSales = 10

        errors = new BindException(book, book.class.name)
        bookValidator.validate(book, errors, true)

         assert !errors.hasErrors()
    }

    public void onSetUp() {
        gcl.parseClass('''
class Book {
    Long id
    Long version
    String title
    int totalSales
    static constraints = {
       title(blank:false, size:1..255)
       totalSales(min:0)

    }
}
        ''')
    }
}