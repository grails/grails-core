package grails.compiler
import grails.persistence.Entity

import org.codehaus.groovy.control.MultipleCompilationErrorsException

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification


class GrailsTypeCheckedCompilationErrorsSpec extends Specification {

    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling valid dynamic finder calls'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsTypeChecked invokes valid dynamic finders'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
class SomeClass {

    def someMethod() {
        List<Company> people = Company.findAllByName('William')
        people = Company.listOrderByName('William')
        int number = Company.countByName('William')
        Company company = Company.findByName('William')
        company = Company.findOrCreateByName('William')
        company = Company.findOrSaveByName('William')
    }
}
''')
        then: 'no errors are thrown'
        c
    }

    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling invalid dynamic finder calls'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsTypeChecked invokes dynamic finders on a non-domain class'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
class SomeClass {

    def someMethod() {
        List<SomeClass> people = SomeClass.findAllByName('William')
        people = SomeClass.listOrderByName('William')
        int number = SomeClass.countByName('William')
        SomeClass company = SomeClass.findByName('William')
        company = SomeClass.findOrCreateByName('William')
        company = SomeClass.findOrSaveByName('William')
    }
}
''')
        then: 'errors are thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#findAllByName'
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#listOrderByName'
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#countByName'
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#findByName'
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#findOrCreateByName'
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#findOrSaveByName'
    }

    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling invalid dynamic finder calls in a method marked with TypeCheckingMode.SKIP'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsTypeChecked invokes dynamic finders on a non-domain class inside of a method marked with TypeCheckingMode.SKIP'
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsTypeChecked
class SomeClass {

    @GrailsTypeChecked(TypeCheckingMode.SKIP)
    def someMethod() {
        List<SomeClass> people = SomeClass.findAllByName('William')
        people = SomeClass.listOrderByName('William')
        int number = SomeClass.countByName('William')
        SomeClass company = SomeClass.findByName('William')
        company = SomeClass.findOrCreateByName('William')
        company = SomeClass.findOrSaveByName('William')
    }
}
''')
        then: 'no errors are thrown'
        c
    }
    
    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling Validateable'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsTypeChecked invokes dynamic finders on a non-domain class inside of a method marked with TypeCheckingMode.SKIP'
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsTypeChecked
class SomeClass implements grails.validation.Validateable {
    String name
    static constraints = {
        name matches: /[A-Z].*/
    }
}
''')
        then: 'no errors are thrown'
        c
    }
    
    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling Validateable which contains unrelated type checking error'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
class SomeClass implements grails.validation.Validateable {
    String name

    def someMethod() {
        someDynamicMethod()
    }

    static constraints = {
        name matches: /[A-Z].*/
    }
}
''')
        then: 'errors are thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#someDynamicMethod'

    }
    
    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling Validateable which attempts to constrain a non existent property'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
class SomeClass implements grails.validation.Validateable {
    String name

    static constraints = {
        name matches: /[A-Z].*/
        age range: 1..99
    }
}
''')
        then: 'errors are thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#age'

    }
    
    
    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling Validateable which attempts to constrain an inherited property'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
class SomeClass implements grails.validation.Validateable {
    String name
}

@GrailsTypeChecked
class SomeSubClass extends SomeClass implements grails.validation.Validateable {
    static constraints = {
        name matches: /[A-Z].*/
    }
}
''')
        then: 'no errors are thrown'
        c
    }
    
    @Issue(['GRAILS-11056', 'GRAILS-11204'])
    void 'Test compiling a class which invokes a criteria query on a domain class'() {
        given:
        def gcl = new GroovyClassLoader()
        
        when:
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsTypeChecked
class SomeClass {

    def someMethod() {

        Company.withCriteria {
            cache true
            eq 'name', 'Anakin'
        }
     
        def crit = Company.createCriteria()

        def listResults = crit.list {
            cache true
            eq 'name', 'Anakin'
        }

        def paginatedListResults = crit.list(max: 4, offset: 2) {
            cache true
            eq 'name', 'Anakin'
        }

        def listDistinctResults = crit.listDistinct {
            cache true
            eq 'name', 'Anakin'
        }

        def scrollResults = crit.scroll {
            cache true
            eq 'name', 'Anakin'
        }

        def getResults = crit.get {
            cache true
            eq 'name', 'Anakin'
        }
    }
}
''')
        then: 'no errors are thrown'
        c
        
    }
    
    void 'Test compiling a domain class with a mapping block'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a domain class marked with @GrailsTypeChecked contains a mapping block'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
@grails.persistence.Entity
class SomeClass {

    String name
    static mapping = {
        table 'name'
    }
}
''')
        then: 'no errors are thrown'
        c
    }

    void 'Test compiling a domain class with a namedQuery block'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a domain class marked with @GrailsTypeChecked contains a mapping block'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
@grails.persistence.Entity
class SomeClass {

    String name

    static namedQueries = {
        findByFirstName { String name ->
            eq('name', name)
        }
    }
}
''')
        then: 'no errors are thrown'
        c
    }
    
    void 'Test compiling a domain class with a mapping block and unrelated dynamic code'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a domain class marked with @GrailsTypeChecked contains a mapping block and unrelated dynamic code'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
@grails.persistence.Entity
class SomeClass {

    String name
    static mapping = {
        table 'name'
    }

    def someMethod() {
       someDynamicMethodCall()
    }
}
''')

        then: 'errors are thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method grails.compiler.SomeClass#someDynamicMethodCall'
    }
    
    @Issue('GRAILS-11571')
    void 'test calling relationship management methods'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
class SomeClass {
    def someMethod() {
        def c = new Company()
        c.addToCodes('code1')
        c.removeFromCodes('code2')
    }
}
''')
        then: 'no errors are thrown'
        c

    }
    
    @Issue('GRAILS-11571')
    void 'test calling relationship management methods with invalid name'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsTypeChecked
class SomeClass {
    def someMethod() {
        def c = new Company()
        c.addToNames('code1')
    }
}
''')
        
        then: 'errors are thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method grails.compiler.Company#addToNames'
    }
}

@Entity
class Company {
    String name
    static hasMany = [codes: String]
}
