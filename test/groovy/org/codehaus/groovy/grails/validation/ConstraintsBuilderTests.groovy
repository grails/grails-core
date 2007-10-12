package org.codehaus.groovy.grails.validation

import  org.codehaus.groovy.grails.commons.test.*
import  org.codehaus.groovy.grails.commons.metaclass.*
import org.springframework.validation.BindException
import org.springframework.validation.Errors;

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


    void testURLValidation() {
        def theClass = ga.getDomainClass("Site")

        def instance = theClass.newInstance()
        def validator = configureValidator(theClass, instance)

        instance.anotherURL = "http://grails.org"
        def errors = validateInstance(instance, validator)
        assert !errors.hasErrors()
        
        instance.anotherURL = "a_bad_url"
        errors = validateInstance(instance, validator)
        assert errors.hasErrors()

        instance.anotherURL = "http://grails.org"
        errors = validateInstance(instance, validator)

        assert !errors.hasErrors()

        instance.url = new URL("http://grails.org")
        errors = validateInstance(instance, validator)
        assert !errors.hasErrors()

        instance.url = new URL("http://localhost:8080/tau_gwi_00/clif/cb/19")
        errors = validateInstance(instance, validator)
        assert !errors.hasErrors()



                      
    }

    Errors validateInstance(instance, validator) {
        def errors = new BindException(instance, instance.class.name)
        validator.validate(instance, errors, true)
        return errors
    }

    GrailsDomainClassValidator configureValidator(theClass, instance) {
        def metaClass = new ExpandoMetaClass(theClass.clazz)
        def errorsProp = null
        def setter = { Object obj -> errorsProp = obj }
        metaClass.setErrors = setter
        metaClass.initialize()
        instance.metaClass = metaClass
        def validator = new GrailsDomainClassValidator()

        validator.domainClass = theClass
        validator.messageSource = createMessageSource()
        theClass.validator = validator
        return validator
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
class Site {
    Long id
    Long version
    URL url
    String anotherURL
    static constraints = {
        url(url:true, nullable:true)
        anotherURL(url:true, nullable:true)
    }
}
        ''')
    }
}