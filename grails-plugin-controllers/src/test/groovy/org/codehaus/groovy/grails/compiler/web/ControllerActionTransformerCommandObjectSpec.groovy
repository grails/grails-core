package org.codehaus.groovy.grails.compiler.web

import grails.spring.BeanBuilder
import grails.util.ClosureToMapPopulator
import grails.util.GrailsWebUtil
import grails.validation.Validateable

import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.ConstraintsEvaluatorFactoryBean
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ControllerActionTransformerCommandObjectSpec extends Specification {

    static testControllerClass
    static subclassControllerClass
    def testController

    void setupSpec() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new ControllerActionTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }
        def transformer2 = new ControllerTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }
        gcl.classInjectors = [transformer, transformer2] as ClassInjector[]
        testControllerClass = gcl.parseClass('''
        class TestController {

            def closureAction = { PersonCommand p ->
                [person: p]
            }

            def methodAction(PersonCommand p) {
                [person: p]
            }

            def closureActionWithArtist = { ArtistCommand a ->
                [artist: a]
            }

            def methodActionWithArtist(ArtistCommand a) {
                [artist: a]
            }

            def methodActionWithArtistSubclass(ArtistSubclass a) {
                [artist: a]
            }

            def closureActionWithArtistSubclass = { ArtistSubclass a ->
                [artist: a]
            }

            def closureActionWithNonValidateableCommandObjectWithAValidateMethod = { org.codehaus.groovy.grails.compiler.web.ClassWithNoValidateMethod co ->
                [co: co]
            }

            def closureActionWithNonValidateableCommandObject = { org.codehaus.groovy.grails.compiler.web.NonValidateableCommand co ->
                [commandObject: co]
            }

            def methodActionWithDate(DateComamndObject co) {
                [command: co]
            }

            def closureActionWithDate = { DateComamndObject co ->
                [command: co]
            }

            def closureActionWithMultipleCommandObjects = { PersonCommand p, ArtistCommand a ->
                [person: p, artist: a]
            }

            def methodActionWithMultipleCommandObjects(PersonCommand p, ArtistCommand a)  {
                [person: p, artist: a]
            }

            def methodActionWithValidateableParam(org.codehaus.groovy.grails.compiler.web.SomeValidateableClass svc) {
                [commandObject: svc]
            }

            def methodActionWithNonValidateableCommandObjectWithAValidateMethod(org.codehaus.groovy.grails.compiler.web.ClassWithNoValidateMethod co) {
                [co: co]
            }

            def methodActionWithNonValidateableCommandObject(org.codehaus.groovy.grails.compiler.web.NonValidateableCommand co) {
                [commandObject: co]
            }

            def methodActionWithSomeCommand(SomeCommand co) {
                [commandObject: co]
            }

            def methodActionWithWidgetCommand(WidgetCommand co) {
                [widget: co]
            }
        }

        class PersonCommand {
            String name
            def theAnswer
            def beforeValidateCounter = 0

            String city
            String state

            def beforeValidate() {
                ++beforeValidateCounter
            }

            static constraints = {
                name matches: /[A-Z]+/
                bindable: false
                city nullable: true, bindable: false
                state nullable: true
            }
        }

        class WidgetCommand {
            Integer width
            Integer height

            static constraints = {
                height range: 1..10
            }
        }

        class SomeCommand {
            private String someFieldWithNoSetter

            void setSomeValue(String val) {
                someFieldWithNoSetter = val
            }

            String getSomeValue() {
                someFieldWithNoSetter
            }
        }

        class ArtistCommand {
            String name
            static constraints = {
                name shared: 'isProg'
            }
        }

        class ArtistSubclass extends ArtistCommand {
            String bandName
            static constraints = {
                bandName matches: /[A-Z].*/
            }
        }
        class DateComamndObject {
            Date birthday
        }
        abstract class MyAbstractController {
            def index = {
                [name: 'Abstract Parent Controller']
            }
        }
        class SubClassController extends MyAbstractController {
            def index = {
                [name: 'Subclass Controller']
            }
        }
        ''')

        // Make sure this parent controller is compiled before the subclass.  This is relevant to GRAILS-8268
        gcl.parseClass('''
        abstract class MyAbstractController {
            def index = {
                [name: 'Abstract Parent Controller']
            }
        }
''')
        subclassControllerClass = gcl.parseClass('''
        class SubClassController extends MyAbstractController {
            def index = {
                [name: 'Subclass Controller']
            }
        }
''')
    }

    def setup() {
        initRequest()
        testController = testControllerClass.newInstance()
        testController.instanceControllersApi = new ControllersApi()
    }

    def initRequest() {
        def appCtx = new GrailsWebApplicationContext()
        def bb = new BeanBuilder()
        def beans = bb.beans {
            theAnswer(Integer, 42)
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                def constraintsClosure = {
                    isProg inList: ['Emerson', 'Lake', 'Palmer']
                }
                defaultConstraints = new ClosureToMapPopulator().populate(constraintsClosure)
            }
        }
        beans.registerBeans(appCtx)

        def request = new MockHttpServletRequest()
        def webRequest = GrailsWebUtil.bindMockWebRequest()

        def servletContext = webRequest.servletContext
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
    }

    void 'Test that rejected binding value survives validation'() {
        when:
            testController.params.width = 'some bad value'
            testController.params.height = 42
            def model = testController.methodActionWithWidgetCommand()
            def widget = model.widget
            def err = widget.errors

        then:
            widget.height == 42
            widget.width == null
            widget.errors.errorCount == 2
            widget.errors.getFieldError('width').rejectedValue == 'some bad value'
    }

    void 'Test non validateable command object'() {
        when:
            testController.params.name = 'Beardfish'
            def model = testController.methodActionWithNonValidateableCommandObject()

        then:
            model.commandObject.name == 'Beardfish'

        when:
            testController.params.name = "Spock's Beard"
            model = testController.closureActionWithNonValidateableCommandObject()

        then:
            model.commandObject.name == "Spock's Beard"
    }

    void 'Test binding to a command object setter property'() {
        when:
        testController.params.someValue = 'My Value'
        def model = testController.methodActionWithSomeCommand()

        then:
        model.commandObject.someValue == 'My Value'
    }

    void "Test binding to multiple command objects"() {
        when:
            testController.params.name = 'Emerson'
            def model = testController.methodActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Emerson'
            model.person.name == 'Emerson'

        when:
            testController.params.name = 'Emerson'
            model = testController.closureActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Emerson'
            model.person.name == 'Emerson'
    }

    void "Test binding to multiple command objects with param name prefixes"() {
        when:
            testController.params.person = [name: 'Emerson']
            testController.params.artist = [name: 'Lake']
            def model = testController.methodActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Lake'
            model.person.name == 'Emerson'

        when:
            testController.params.person = [name: 'Emerson']
            testController.params.artist = [name: 'Lake']
            model = testController.closureActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Lake'
            model.person.name == 'Emerson'
    }

    void "Test clearErrors"() {
        when:
        def model = testController.methodActionWithArtist()

        then:
            model.artist
            model.artist.name == null
            model.artist.hasErrors()
            model.artist.errors.errorCount == 1

        when:
            model.artist.clearErrors()

        then:
            !model.artist.hasErrors()
            model.artist.errors.errorCount == 0
    }

    void "Test nullability"() {
        when:
            def model = testController.methodActionWithArtist()
            def nameErrorCodes = model.artist?.errors?.getFieldError('name')?.codes?.toList()

        then:
            model.artist
            model.artist.name == null
            nameErrorCodes
            'artistCommand.name.nullable.error' in nameErrorCodes

        when:
            model = testController.closureActionWithArtist()
            nameErrorCodes = model.artist?.errors?.getFieldError('name')?.codes?.toList()

        then:
            model.artist
            model.artist.name == null
            nameErrorCodes
            'artistCommand.name.nullable.error' in nameErrorCodes
    }

    void 'Test beforeValidate gets invoked'() {
        when:
        def model = testController.methodAction()
        def person = model.person

        then:
        1 == person.beforeValidateCounter
    }

    void 'Test constraints property'() {
        when:
            def model = testController.methodAction()
            def person = model.person
            def constrainedProperties = person.constraints
            def nameConstrainedProperty = constrainedProperties.name
            def matchesProperty = nameConstrainedProperty.matches

        then:
            /[A-Z]+/ == matchesProperty
    }

    void "Test command object gets autowired"() {
        when:
            def model = testController.methodAction()

        then:
            model.person.theAnswer == 42

        when:
            model = testController.closureAction()

        then:
            model.person.theAnswer == 42
    }

    void 'Test bindable command object constraint'() {
        when:
            testController.params.name = 'JFK'
            testController.params.city = 'STL'
            testController.params.state = 'Missouri'
            def model = testController.methodAction()

        then:
            !model.person.hasErrors()
            model.person.name == 'JFK'
            model.person.state == 'Missouri'
            model.person.city == null
    }

    void 'Test subscript operator on command object errors'() {
        when:
            testController.params.name = 'Maynard'
            def model = testController.closureAction()

        then:
            model.person.hasErrors()
            model.person.name == 'Maynard'
            'matches.invalid.name' in model.person.errors['name'].codes
    }

    void "Test validation"() {
        when:
            testController.params.name = 'JFK'
            def model = testController.methodAction()

        then:
            !model.person.hasErrors()
            model.person.name == 'JFK'

        when:
            testController.params.name = 'JFK'
            model = testController.closureAction()

        then:
            !model.person.hasErrors()
            model.person.name == 'JFK'

        when:
            testController.params.name = 'Maynard'
            model = testController.closureAction()

        then:
            model.person.hasErrors()
            model.person.name == 'Maynard'

        when:
            testController.params.name = 'Maynard'
            model = testController.methodAction()

        then:
            model.person.hasErrors()
            model.person.name == 'Maynard'
    }

    void "Test validation with inherited constraints"() {

        when:
            testController.params.name = 'Emerson'
            testController.params.bandName = 'Emerson Lake and Palmer'
            def model = testController.closureActionWithArtistSubclass()

        then:
            model.artist
            model.artist.name == 'Emerson'
            model.artist.bandName == 'Emerson Lake and Palmer'
            !model.artist.hasErrors()

        when:
            testController.params.name = 'Emerson'
            testController.params.bandName = 'Emerson Lake and Palmer'
            model = testController.methodActionWithArtistSubclass()

        then:
            model.artist
            model.artist.name == 'Emerson'
            model.artist.bandName == 'Emerson Lake and Palmer'
            !model.artist.hasErrors()

        when:
            testController.params.clear()
            model = testController.closureActionWithArtistSubclass()

        then:
            model.artist
            model.artist.hasErrors()
            model.artist.errors.errorCount == 2

        when:
            model = testController.methodActionWithArtistSubclass()

        then:
            model.artist
            model.artist.hasErrors()
            model.artist.errors.errorCount == 2
    }

    void "Test validation with shared constraints"() {
        when:
            testController.params.name = 'Emerson'
            def model = testController.closureActionWithArtist()

        then:
            model.artist
            !model.artist.hasErrors()

        when:
            testController.params.name = 'Emerson'
            model = testController.methodActionWithArtist()

        then:
            model.artist
            !model.artist.hasErrors()

        when:
            testController.params.name = 'Hendrix'
            model = testController.closureActionWithArtist()

        then:
            model.artist
            model.artist.hasErrors()

        when:
            testController.params.name = 'Hendrix'
            model = testController.methodActionWithArtist()

        then:
            model.artist
            model.artist.hasErrors()
    }

    void "Test command object with date binding"() {
        setup:
            def expectedCalendar = Calendar.instance
            expectedCalendar.clear()
            expectedCalendar.set Calendar.DAY_OF_MONTH, 3
            expectedCalendar.set Calendar.MONTH, Calendar.MAY
            expectedCalendar.set Calendar.YEAR, 1973
            def expectedDate = expectedCalendar.time

        when:
            testController.params.birthday = "struct"
            testController.params.birthday_day = "03"
            testController.params.birthday_month = "05"
            testController.params.birthday_year = "1973"
            def model = testController.closureActionWithDate()
            def birthday = model.command?.birthday

        then:
            model.command
            !model.command.hasErrors()
            birthday
            expectedDate == birthday

        when:
            testController.params.birthday = "struct"
            testController.params.birthday_day = "03"
            testController.params.birthday_month = "05"
            testController.params.birthday_year = "1973"
            model = testController.methodActionWithDate()
            birthday = model.command?.birthday

        then:
            model.command
            !model.command.hasErrors()
            birthday
            expectedDate == birthday
    }

    void 'Test command object that is a precompiled @Validatable'() {
        when:
            def model = testController.methodActionWithValidateableParam()

        then:
            model.commandObject.hasErrors()

        when:
            testController.params.name = 'mixedCase'
            model = testController.methodActionWithValidateableParam()

        then:
            model.commandObject.hasErrors()

        when:
            testController.params.name = 'UPPERCASE'
            model = testController.methodActionWithValidateableParam()

        then:
            !model.commandObject.hasErrors()
    }

    void 'Test overriding closure actions in subclass'() {
        given:
            def subclassController = subclassControllerClass.newInstance()
            subclassController.instanceControllersApi = new ControllersApi()

        when:
            def model = subclassController.index()

        then:
            'Subclass Controller' == model.name
    }

    void "Test a command object that does not have a validate method at compile time but does at runtime"() {
        when:
            def model = testController.methodActionWithNonValidateableCommandObjectWithAValidateMethod()

        then:
            0 == model.co.validationCounter

        when:
            model = testController.closureActionWithNonValidateableCommandObjectWithAValidateMethod()

        then:
            0 == model.co.validationCounter

        when:
            ClassWithNoValidateMethod.metaClass.validate = { ->
                ++ delegate.validationCounter
            }
            model = testController.methodActionWithNonValidateableCommandObjectWithAValidateMethod()

        then:
            1 == model.co.validationCounter

        when:
            model = testController.closureActionWithNonValidateableCommandObjectWithAValidateMethod()

        then:
            1 == model.co.validationCounter
    }

    def cleanupSpec() {
        RequestContextHolder.setRequestAttributes(null)
    }
}

@Validateable
class SomeValidateableClass {
    String name

    static constraints = {
        name matches: /[A-Z]*/
    }
}

class ClassWithNoValidateMethod {
    int validationCounter = 0
}

class NonValidateableCommand {
    String name
}
