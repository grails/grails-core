package grails.validation

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import junit.framework.TestCase
import org.grails.datastore.mapping.reflect.FieldEntityAccess
import org.springframework.validation.BindException
import org.springframework.validation.Errors

class ConstraintsBuilderTests extends TestCase {
    GrailsApplication ga

    void testPrimitiveIntAndMinConstraint() {
        def bookClass = ga.getDomainClass("ConstraintsBuilderTestsBook")
        def book = bookClass.newInstance()
        book.title = "foo"

        def bookMetaClass = new ExpandoMetaClass(bookClass.clazz)

        def errorsProp = null
        def setter = { Object obj -> errorsProp = obj }

        bookMetaClass.setErrors = setter
        bookMetaClass.initialize()
        book.metaClass = bookMetaClass
        def bookValidator = ((GrailsDomainClass)bookClass).getValidator()

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
        def validator = ((GrailsDomainClass)theClass).getValidator()

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

    @Override
    protected void tearDown() throws Exception {
        FieldEntityAccess.clearReflectors()
    }

    @Override
    protected void setUp() {
        super.setUp()
        GroovyClassLoader gcl = new GroovyClassLoader()
        gcl.parseClass('''
@grails.persistence.Entity
class ConstraintsBuilderTestsBook {
    Long id
    Long version
    String title
    int totalSales
    static constraints = {
       title(blank:false, size:1..255)
       totalSales(min:0)

    }
}
@grails.persistence.Entity
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
        ga = new DefaultGrailsApplication(gcl.loadedClasses)
        ga.initialise()
        new MappingContextBuilder(ga).build(gcl.loadedClasses)
    }

}
