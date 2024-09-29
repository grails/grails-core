package grails.web.databinding

import grails.databinding.SimpleMapDataBindingSource;
import grails.testing.gorm.DataTest
import spock.lang.Specification


/**
 * This spec is for testing configuration settings in GrailsWebDataBinder.  These
 * tests are kept separate from GrailsWebDataBinderSpec as the test methods in that
 * spec are sharing an instance of GrailsWebDataBinder so mutating the binder in
 * one test causes problems for other tests.
 *
 */
class GrailsWebDataBinderConfigurationSpec extends Specification implements DataTest {

    GrailsWebDataBinder binder

    void setupSpec() {
        mockDomains(Author, Team)
    }

    void setup() {
        binder = new GrailsWebDataBinder(grailsApplication)
    }

    void 'Test autoGrowCollectionLimit with Maps of String'() {
        given:
        def team = new Team()
        binder.autoGrowCollectionLimit = 2
        def bindingSource = [:]
        bindingSource['states[MO]'] = 'Missouri'
        bindingSource['states[IL]'] = 'Illinois'
        bindingSource['states[VA]'] = 'Virginia'
        bindingSource['states[CA]'] = 'California'

        when:
        binder.bind team, bindingSource as SimpleMapDataBindingSource

        then:
        team.states.size() == 2
        team.states.containsKey('MO')
        team.states.containsKey('IL')
        team.states.MO == 'Missouri'
        team.states.IL == 'Illinois'
    }

    void 'Test autoGrowCollectionLimit with Maps of domain objects'() {
        given:
        def team = new Team()
        binder.autoGrowCollectionLimit = 2
        def bindingSource = [:]
        bindingSource['members[jeff]'] = [name: 'Jeff Scott Brown']
        bindingSource['members[betsy]'] = [name: 'Sarah Elizabeth Brown']
        bindingSource['members[jake]'] = [name: 'Jacob Ray Brown']
        bindingSource['members[zack]'] = [name: 'Zachary Scott Brown']

        when:
        binder.bind team, bindingSource as SimpleMapDataBindingSource

        then:
        team.members.size() == 2
        team.members.containsKey('jeff')
        team.members.containsKey('betsy')
        team.members.jeff instanceof Author
        team.members.betsy instanceof Author
        team.members.jeff.name == 'Jeff Scott Brown'
        team.members.betsy.name == 'Sarah Elizabeth Brown'
    }

    void 'Test string trimming'() {
        given:
        def author = new Author()

        when:
        binder.bind author, [name: '   Jeff Scott Brown ', stringWithSpecialBinding: '   Jeff Scott Brown '] as SimpleMapDataBindingSource

        then:
        author.name == 'Jeff Scott Brown'
        author.stringWithSpecialBinding == 'Jeff Scott Brown'

        when:
        def actualName = 'Jeff Scott Brown'
        binder.bind author, [name: "   ${actualName} ", stringWithSpecialBinding: "   ${actualName} "] as SimpleMapDataBindingSource

        then:
        author.name == 'Jeff Scott Brown'
        author.stringWithSpecialBinding == 'Jeff Scott Brown'

        when:
        binder.trimStrings = false
        binder.bind author, [name: '  Jeff Scott Brown   ', stringWithSpecialBinding: '  Jeff Scott Brown   '] as SimpleMapDataBindingSource

        then:
        author.name == '  Jeff Scott Brown   '
        author.stringWithSpecialBinding == 'Jeff Scott Brown'

        when:
        binder.trimStrings = false
        binder.bind author, [name: "  ${actualName}   ", stringWithSpecialBinding: "  ${actualName}   "] as SimpleMapDataBindingSource

        then:
        author.name == '  Jeff Scott Brown   '
        author.stringWithSpecialBinding == 'Jeff Scott Brown'
    }
    
    void 'Test binding format code'() {
        given:
        messageSource.addMessage 'my.date.format', Locale.US, 'MMddyyyy'
        messageSource.addMessage 'my.date.format', Locale.UK, 'ddMMyyyy'
        def child = new Child()

        when:
        def binder = new GrailsWebDataBinder(grailsApplication)  {
            Locale getLocale() {
                Locale.US
            }
        }
        binder.messageSource = messageSource
        binder.bind child, [birthDate: '11151969'] as SimpleMapDataBindingSource
        def birthDate = child.birthDate

        then:
        Calendar.NOVEMBER == birthDate.month
        15 == birthDate.date
        69 == birthDate.year

        when:
        binder = new GrailsWebDataBinder(grailsApplication) {
            Locale getLocale() {
                Locale.UK
            }
        }
        binder.messageSource = messageSource
        child.birthDate = null
        binder.bind child, [birthDate: '15111969'] as SimpleMapDataBindingSource
        birthDate = child.birthDate

        then:
        Calendar.NOVEMBER == birthDate.month
        15 == birthDate.date
        69 == birthDate.year
    }
}
