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
    void 'Test compiling @Validateable'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @GrailsCompileStatic invokes dynamic finders on a non-domain class inside of a method marked with TypeCheckingMode.SKIP'
        def c = gcl.parseClass('''
package grails.compiler

import groovy.transform.TypeCheckingMode

@GrailsCompileStatic
@grails.validation.Validateable
class SomeClass {
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
    void 'Test compiling @Validateable which contains unrelated type checking error'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
@grails.validation.Validateable
class SomeClass {
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
    void 'Test compiling @Validateable which attempts to constrain a non existent property'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
@grails.validation.Validateable
class SomeClass {
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
    void 'Test compiling @Validateable which attempts to constrain an inherited property'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        def c = gcl.parseClass('''
package grails.compiler

@GrailsCompileStatic
@grails.validation.Validateable
class SomeClass {
    String name
}

@GrailsCompileStatic
@grails.validation.Validateable
class SomeSubClass extends SomeClass {
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
            eq 'name', 'Anakin'
        }
     
        Person.createCriteria {
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
}

@Entity
class Person {
    String name
    static hasMany = [towns: String]
}
