package org.codehaus.groovy.grails.compiler.web

import grails.spring.BeanBuilder
import grails.util.ClosureToMapPopulator
import grails.util.GrailsWebUtil

import java.util.Calendar

import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.ConstraintsEvaluatorFactoryBean
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.ContextLoader
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ControllerActionTransformerCommandObjectSpec extends Specification {

    static controllerClass
    def controller

    void setupSpec() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new ControllerActionTransformer() {
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        def transformer2 = new ControllerTransformer() {
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        gcl.classInjectors = [transformer, transformer2]as ClassInjector[]
        controllerClass = gcl.parseClass('''
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
        }

        class PersonCommand {
            String name
            def theAnswer

            static constraints = {
                name matches: /[A-Z]+/
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
        ''')
    }

    def setup() {
        initRequest()
        controller = controllerClass.newInstance()
    }

    def cleanup() {
        ContextLoader.@currentContext = null
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
        ContextLoader.@currentContext = appCtx

        def request = new MockHttpServletRequest();
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        
        def servletContext = webRequest.servletContext
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
    }

    void "Test binding to multiple command objects"() {
        when:
            controller.params.name = 'Emerson'
            def model = controller.methodActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Emerson'
            model.person.name == 'Emerson'

        when:
            controller.params.name = 'Emerson'
            model = controller.closureActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Emerson'
            model.person.name == 'Emerson'
    }

    void "Test binding to multiple command objects with param name prefixes"() {
        when:
            controller.params.person = [name: 'Emerson']
            controller.params.artist = [name: 'Lake']
            def model = controller.methodActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Lake'
            model.person.name == 'Emerson'

        when:
            controller.params.person = [name: 'Emerson']
            controller.params.artist = [name: 'Lake']
            model = controller.closureActionWithMultipleCommandObjects()

        then:
            model.person
            model.artist
            model.artist.name == 'Lake'
            model.person.name == 'Emerson'
    }

    void "Test clearErrors"() {
        when:
        def model = controller.methodActionWithArtist()

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
            def model = controller.methodActionWithArtist()
            def nameErrorCodes = model.artist?.errors?.getFieldError('name')?.codes?.toList()

        then:
            model.artist
            model.artist.name == null
            nameErrorCodes
            'artistCommand.name.nullable.error' in nameErrorCodes

        when:
            model = controller.closureActionWithArtist()
            nameErrorCodes = model.artist?.errors?.getFieldError('name')?.codes?.toList()

        then:
            model.artist
            model.artist.name == null
            nameErrorCodes
            'artistCommand.name.nullable.error' in nameErrorCodes
    }

    void "Test command object gets autowired"() {
        when:
            def model = controller.methodAction()

        then:
            model.person.theAnswer == 42

        when:
            model = controller.closureAction()

        then:
            model.person.theAnswer == 42
    }

    void "Test validation"() {
        when:
            controller.params.name = 'JFK'
            def model = controller.methodAction()

        then:
            !model.person.hasErrors()
            model.person.name == 'JFK'

        when:
            controller.params.name = 'JFK'
            model = controller.closureAction()

        then:
            !model.person.hasErrors()
            model.person.name == 'JFK'

        when:
            controller.params.name = 'Maynard'
            model = controller.closureAction()

        then:
            model.person.hasErrors()
            model.person.name == 'Maynard'

        when:
            controller.params.name = 'Maynard'
            model = controller.methodAction()

        then:
            model.person.hasErrors()
            model.person.name == 'Maynard'
    }

    void "Test validation with inherited constraints"() {

        when:
            controller.params.name = 'Emerson'
            controller.params.bandName = 'Emerson Lake and Palmer'
            def model = controller.closureActionWithArtistSubclass()

        then:
            model.artist
            model.artist.name == 'Emerson'
            model.artist.bandName == 'Emerson Lake and Palmer'
            !model.artist.hasErrors()

        when:
            controller.params.name = 'Emerson'
            controller.params.bandName = 'Emerson Lake and Palmer'
            model = controller.methodActionWithArtistSubclass()

        then:
            model.artist
            model.artist.name == 'Emerson'
            model.artist.bandName == 'Emerson Lake and Palmer'
            !model.artist.hasErrors()

        when:
            controller.params.clear()
            model = controller.closureActionWithArtistSubclass()

        then:
            model.artist
            model.artist.hasErrors()
            model.artist.errors.errorCount == 2

        when:
            model = controller.methodActionWithArtistSubclass()

        then:
            model.artist
            model.artist.hasErrors()
            model.artist.errors.errorCount == 2
    }

    void "Test validation with shared constraints"() {
        when:
            controller.params.name = 'Emerson'
            def model = controller.closureActionWithArtist()

        then:
            model.artist
            !model.artist.hasErrors()

        when:
            controller.params.name = 'Emerson'
            model = controller.methodActionWithArtist()

        then:
            model.artist
            !model.artist.hasErrors()

        when:
            controller.params.name = 'Hendrix'
            model = controller.closureActionWithArtist()

        then:
            model.artist
            model.artist.hasErrors()

        when:
            controller.params.name = 'Hendrix'
            model = controller.methodActionWithArtist()

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
            controller.params.birthday = "struct"
            controller.params.birthday_day = "03"
            controller.params.birthday_month = "05"
            controller.params.birthday_year = "1973"
            def model = controller.closureActionWithDate()
            def birthday = model.command?.birthday

        then:
            model.command
            !model.command.hasErrors()
            birthday
            expectedDate == birthday

        when:
            controller.params.birthday = "struct"
            controller.params.birthday_day = "03"
            controller.params.birthday_month = "05"
            controller.params.birthday_year = "1973"
            model = controller.methodActionWithDate()
            birthday = model.command?.birthday

        then:
            model.command
            !model.command.hasErrors()
            birthday
            expectedDate == birthday
    }

    def cleanupSpec() {
        RequestContextHolder.setRequestAttributes(null)
    }
}

