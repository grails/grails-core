package grails.compiler

import grails.persistence.Entity
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

class GrailsCompileStaticCompilationErrorsSpec extends Specification {

    @Issue('GRAILS-11056')
    void 'Test compiling valid dynamic finder calls'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes valid dynamic finders'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
class SomeClass {

    def someMethod() {
        List<Person> people = Person.findAllByName('William')
        people = Person.listOrderByName('William')
        int number = Person.countByName('William')
        Person person = Person.findByName('William')
        person = Person.findOrCreateByName('William')
        person = Person.findOrSaveByName('William')
    }
}
''')
        then: 'no errors are thrown'
        c
    }

    @Issue('GRAILS-9996')
    void 'Test compiling where query call'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes a where query'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
class SomeOtherNewClass {
   def someMethod() {
      Person.where {
           name == 'Guido'
      }
   }

}
''')
        then: 'no errors are thrown'
        c
    }

    @Issue('GRAILS-9996')
    void 'Test compiling where query call which refers to an invalid property'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes a where query which refers to an invalid property'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
class SomeOtherNewClass {
   def someMethod() {
      Person.where {
           town == 'Brooklyn'
      }
   }

}
''')
        then: 'an error is thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot query on property "town"'
    }

    @Ignore
    @Issue(['GRAILS-11056', 'GRAILS-11057'])
    void 'Test comping a dynmaic finder call with the wrong number of arguments'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes a dynamic finder with the wrong number of arguments'
        gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
class SomeClass {
    def someMethod() {
        Person.findAllByName('Hugh', 'Howey')
    }
}''')

        then: 'an error is thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method grails.compiler.Person#findAllByName'

    }

    @Issue('GRAILS-11056')
    void 'Test compiling invalid dynamic finder calls'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes dynamic finders on a non-domain class'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
class SomeClass {

    def someMethod() {
        List<SomeClass> people = SomeClass.findAllByName('William')
        people = SomeClass.listOrderByName('William')
        int number = SomeClass.countByName('William')
        SomeClass person = SomeClass.findByName('William')
        person = SomeClass.findOrCreateByName('William')
        person = SomeClass.findOrSaveByName('William')
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

    @Issue('GRAILS-11056')
    void 'Test compiling invalid dynamic finder calls in a method marked with TypeCheckingMode.SKIP'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes dynamic finders on a non-domain class inside of a method marked with TypeCheckingMode.SKIP'
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsCompileStatic
class SomeClass {

    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    def someMethod() {
        List<SomeClass> people = SomeClass.findAllByName('William')
        people = SomeClass.listOrderByName('William')
        int number = SomeClass.countByName('William')
        SomeClass person = SomeClass.findByName('William')
        person = SomeClass.findOrCreateByName('William')
        person = SomeClass.findOrSaveByName('William')
    }
}
''')
        then: 'no errors are thrown'
        c
    }
    
    @Issue('GRAILS-11242')
    void 'Test compiling Validateable'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes dynamic finders on a non-domain class inside of a method marked with TypeCheckingMode.SKIP'
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsCompileStatic
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

    @Issue('GRAILS-11242')
    void 'Test compiling Validateable with inner class'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes dynamic finders on a non-domain class inside of a method marked with TypeCheckingMode.SKIP'
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsCompileStatic
class SomeClass implements grails.validation.Validateable {

    enum TestKind {
        BIG,
        SMALL
    }
    
    String name
    static constraints = {
        name matches: /[A-Z].*/
    }
}
''')
        then: 'no errors are thrown'
        c
    }
    
    @Issue('GRAILS-11242')
    void 'Test compiling Validateable which contains unrelated type checking error'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
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
    
    @Issue('GRAILS-11242')
    void 'Test compiling Validateable which attempts to constrain a non existent property'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
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
    
    
    @Issue('GRAILS-11242')
    void 'Test compiling Validateable which attempts to constrain an inherited property'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
class SomeClass implements grails.validation.Validateable {
    String name
}

@GrailsCompileStatic
class SomeSubClass extends SomeClass implements grails.validation.Validateable {
    static constraints = {
        name matches: /[A-Z].*/
    }
}
''')
        then: 'no errors are thrown'
        c
    }
    
    @Issue('GRAILS-11255')
    void 'Test compiling a class which invokes a criteria query on a domain class'() {
        given:
        def gcl = new GroovyClassLoader()
        
        when:
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsCompileStatic
class SomeClass {

    def someMethod() {
        Person.withCriteria {
            cache true
            eq 'name', 'Anakin'
        }
     
        def crit = Person.createCriteria()

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

        when: 'a domain class marked with @GrailsCompileStatic contains a mapping block'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
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

    @Issue('https://github.com/grails/grails-core/issues/643')
    void 'Test that a controller marked with @GrailsCompileStatic may reference dynamic request properties'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
@grails.web.Controller
class SomeController {

    void someAction() {
        if(request.post || request.get || request.xhr) {
            render 'yep'
        } else {
            render 'nope'
        }
    }
}
''')
        then:
        c

    }

    
    void 'Test compiling a domain class with a mapping block and unrelated dynamic code'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a domain class marked with @GrailsCompileStatic contains a mapping block and unrelated dynamic code'
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
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

@GrailsCompileStatic
class SomeClass {
    def someMethod() {
        def p = new Person()
        p.addToTowns('STL')
        p.removeFromTowns('ORD')
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

@GrailsCompileStatic
class SomeClass {
    def someMethod() {
        def p = new Person()
        p.addToCodes('STL')
    }
}
''')
        
        then: 'errors are thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method grails.compiler.Person#addToCodes'
    }

    void 'test GrailsCompileStatic on a method in a class marked with Transactional'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
package demo

@grails.transaction.Transactional
class SomeService {
    @grails.compiler.GrailsCompileStatic
    def someMethod() {
        int x = 'Jeff'.lastName()
    }
}
''')

        then: 'an error is thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Cannot find matching method java.lang.String#lastName()'

    }

    @Issue('grails/grails-core#10157')
    void 'Test constraints block which imports from a non-existent class'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic imports constraints from a non-existent class'
        gcl.parseClass('''
package grails.compiler

import grails.validation.Validateable

@GrailsCompileStatic
class SomeValidateableClassWithInvalidImport implements Validateable {
    String name

    static constraints = {
        importFrom SomeNonExistentClass
    }
}
''')
        then: 'an error is thrown'
        thrown(MultipleCompilationErrorsException)
    }

    @Issue('grails/grails-core#10157')
    void 'Test constraints block which imports constraints'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic imports constraints from a non-existent class'
        def c = gcl.parseClass('''
package grails.compiler

import grails.validation.Validateable

@GrailsCompileStatic
class SomeValidateableClassWithValidImport implements Validateable {
    String name

    static constraints = {
        importFrom SomeOtherValidateableClass
    }
}

class SomeOtherValidateableClass implements Validateable {
    String name
    static constraints = {
        name size: 3..15
    }
}
''')

        then: 'the constraints were properly imported'
        gcl.loadClass('grails.compiler.SomeValidateableClassWithValidImport').constraintsMap['name'].getAppliedConstraint('size').range == 3..15
    }
}

@Entity
class Person {
    String name
    static hasMany = [towns: String]
}
