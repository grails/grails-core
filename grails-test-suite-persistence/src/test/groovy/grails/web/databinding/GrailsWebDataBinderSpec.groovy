/* Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.web.databinding

import grails.databinding.BindUsing
import grails.databinding.BindingFormat
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.databinding.errors.BindingError
import grails.databinding.events.DataBindingListenerAdapter
import grails.persistence.Entity
import grails.testing.gorm.DataTest
import grails.validation.DeferredBindingActions
import grails.validation.Validateable
import groovy.transform.Sortable
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class GrailsWebDataBinderSpec extends Specification implements DataTest {

    private static Locale defaultLocale = Locale.getDefault()

    GrailsWebDataBinder binder

    void setupSpec() {
        mockDomains(
            AssociationBindingAuthor, AssociationBindingBook, AssociationBindingPage, Author, Child,
            CollectionContainer, DataBindingBook, Fidget, Foo, Parent, Publication, Publisher, Team, Widget
        )
    }

    void setup() {
        binder = grailsApplication.mainContext.getBean(DataBindingUtils.DATA_BINDER_BEAN_NAME) as GrailsWebDataBinder
    }
    
    void cleanup() {
        Locale.setDefault(defaultLocale)
    }

    void 'Test binding an invalid String to an object reference does not result in an empty instance being bound'() {
        // GRAILS-3159
        given:
        def publication = new Publication()

        when:
        binder.bind publication, new SimpleMapDataBindingSource([author: '42'])

        then:
        publication.author == null
    }

    void 'Test binding empty and blank String'() {
        given:
        def obj = new Author()

        when:
        binder.bind obj, new SimpleMapDataBindingSource([name: '', stringWithSpecialBinding:''])

        then:
        obj.name == null
        obj.stringWithSpecialBinding == ''

        when:
        binder.bind obj, new SimpleMapDataBindingSource([name: '  ', stringWithSpecialBinding: '  '])

        then:
        obj.name == null
        obj.stringWithSpecialBinding == ''

        when:
        def emptyString = ''
        binder.bind obj, new SimpleMapDataBindingSource([name: "${emptyString}", stringWithSpecialBinding: "${emptyString}"])

        then:
        obj.name == null
        obj.stringWithSpecialBinding == ''

        when:
        binder.bind obj, new SimpleMapDataBindingSource([name: "  ${emptyString}  ", stringWithSpecialBinding: "  ${emptyString}  "])

        then:
        obj.name == null
        obj.stringWithSpecialBinding == ''
    }

    @Unroll
    void 'Test binding to primitives from Strings when locale is #locale'() {
        given:
        Locale.setDefault(locale)
        def obj = new PrimitiveContainer()

        when:
        binder.bind(obj, new SimpleMapDataBindingSource([someBoolean: 'true',
            someByte: '1',
            someChar: 'a',
            someShort: '2',
            someInt: '3',
            someLong: '4',
            someFloat: '5.5'.replace('.', decimalSeparator),
            someDouble: '6.6'.replace('.', decimalSeparator)]))

        then:
        obj.someBoolean
        obj.someByte == 1 as byte
        obj.someChar == ('a' as char)
        obj.someShort == 2 as short
        obj.someInt == 3
        obj.someLong == 4
        obj.someFloat == 5.5f
        obj.someDouble == 6.6d
        where:
        locale << [new Locale("fi", "FI", ""), new Locale("en", "US", "")]
        decimalSeparator << [',', '.']
    }
    
    void 'Test binding to primitive numbers from malformed Strings when locale is #locale'() {
        given:
        Locale.setDefault(locale)
        def obj = new PrimitiveContainer()

        when:
        binder.bind(obj, new SimpleMapDataBindingSource([
            someShort: '2x',
            someInt: '3x',
            someLong: '4x',
            someFloat: '5.5x'.replace('.', decimalSeparator),
            someDouble: '6.6x'.replace('.', decimalSeparator)]))

        then:
        obj.someShort == 0 as short
        obj.someInt == 0
        obj.someLong == 0
        obj.someFloat == 0
        obj.someDouble == 0
        obj.errors.getFieldError('someShort').defaultMessage == 'Unable to parse number [2x]'
        obj.errors.getFieldError('someShort').rejectedValue == '2x'
        obj.errors.getFieldError('someInt').defaultMessage == 'Unable to parse number [3x]'
        obj.errors.getFieldError('someInt').rejectedValue == '3x'
        obj.errors.getFieldError('someLong').defaultMessage == 'Unable to parse number [4x]'
        obj.errors.getFieldError('someLong').rejectedValue == '4x'
        obj.errors.getFieldError('someFloat').defaultMessage == 'Unable to parse number [5' + decimalSeparator + '5x]'
        obj.errors.getFieldError('someFloat').rejectedValue == '5' + decimalSeparator + '5x'
        obj.errors.getFieldError('someDouble').defaultMessage == 'Unable to parse number [6' + decimalSeparator + '6x]'
        obj.errors.getFieldError('someDouble').rejectedValue == '6' + decimalSeparator + '6x'

        where:
        locale << [new Locale("fi", "FI", ""), new Locale("en", "US", "")]
        decimalSeparator << [',', '.']
    }

    void 'Test binding null to id of element nested in a List'() {
        given:
        def obj = new CollectionContainer()
        def map = [:]

        map['listOfWidgets[0]'] = [isBindable: 'Is Uno (List)', isNotBindable: 'Is Not Uno (List)']
        map['listOfWidgets[1]'] = [isBindable: 'Is Dos (List)', isNotBindable: 'Is Not Dos (List)']
        map['listOfWidgets[2]'] = [isBindable: 'Is Tres (List)', isNotBindable: 'Is Not Tres (List)']

        when:
        binder.bind obj, new SimpleMapDataBindingSource(map)
        def listOfWidgets = obj.listOfWidgets

        then:
        listOfWidgets instanceof List
        listOfWidgets.size() == 3
        listOfWidgets[0].isBindable == 'Is Uno (List)'
        listOfWidgets[0].isNotBindable == null
        listOfWidgets[1].isBindable == 'Is Dos (List)'
        listOfWidgets[1].isNotBindable == null
        listOfWidgets[2].isBindable == 'Is Tres (List)'
        listOfWidgets[2].isNotBindable == null

        when:
        map = ['listOfWidgets[1]': [id: 'null']]
        binder.bind obj, new SimpleMapDataBindingSource(map)

        listOfWidgets = obj.listOfWidgets

        then:
        listOfWidgets instanceof List
        listOfWidgets.size() == 2
        listOfWidgets[0].isBindable == 'Is Uno (List)'
        listOfWidgets[0].isNotBindable == null
        listOfWidgets[1].isBindable == 'Is Tres (List)'
        listOfWidgets[1].isNotBindable == null
    }
    
    void 'Test binding null id to a domain class reference in a non-domain class'() {
        given:
        def nonDomainClass = new SomeNonDomainClass()
        
        when:
        binder.bind nonDomainClass, [publication:[id: null]] as SimpleMapDataBindingSource
        
        then:
        nonDomainClass.publication == null
        
        when:
        binder.bind nonDomainClass, [publication:[id: 'null']] as SimpleMapDataBindingSource
        
        then:
        nonDomainClass.publication == null
        
        when:
        binder.bind nonDomainClass, [publication:[id: '']] as SimpleMapDataBindingSource
        
        then:
        nonDomainClass.publication == null
    }

    void 'Test id binding'() {
        given:
        def author = new Author(name: 'David Foster Wallace').save(flush: true, failOnError:true)
        def publication = new Publication()

        when:
        binder.bind publication, new SimpleMapDataBindingSource([title: 'Infinite Jest', author: [id: author.id]])

        then:
        publication.title == 'Infinite Jest'
        publication.author.name == 'David Foster Wallace'

        when:
        binder.bind publication, new SimpleMapDataBindingSource([author: [id: 'null']])

        then:
        publication.author == null

        when:
        publication.title = null
        binder.bind publication, new SimpleMapDataBindingSource([title: 'Infinite Jest', author: [id: author.id]]), [], ['author']

        then:
        publication.title == 'Infinite Jest'
        publication.author == null

        when:
        publication.author = null
        binder.bind publication, new SimpleMapDataBindingSource([title: 'Infinite Jest 2', author: [id: author.id]])

        then:
        publication.author.name == 'David Foster Wallace'

        when:
        binder.bind publication, new SimpleMapDataBindingSource([author: [id: '']])

        then:
        publication.author == null

        when:
        binder.bind publication, new SimpleMapDataBindingSource([author: [id: null]])

        then:
        publication.author == null

        when:
        publication.author = new Author()
        binder.bind publication, new SimpleMapDataBindingSource([author: [id: null]])

        then:
        publication.author == null

        when:
        publication.author = new Author()
        binder.bind publication, new SimpleMapDataBindingSource([author: [id: 'null']])

        then:
        publication.author == null

        when:
        publication.author = new Author()
        binder.bind publication, new SimpleMapDataBindingSource([author: [id: '']])

        then:
        publication.author == null

        when:
        publication.author = new Author()
        binder.bind publication, new SimpleMapDataBindingSource([author: 'null'])

        then:
        publication.author == null

        when:
        publication.author = new Author()
        binder.bind publication, new SimpleMapDataBindingSource([author: ''])

        then:
        publication.author == null

        when:
        publication.author = new Author()
        binder.bind publication, new SimpleMapDataBindingSource([author: null])

        then:
        publication.author == null
    }

    void 'Test id binding with a non dataSource aware binding source'() {
        given:
        def author = new Author(name: 'David Foster Wallace').save(flush: true)
        def publication = new Publication()

        when:
        def bindingSource = new SimpleMapDataBindingSource([title: 'Infinite Jest', author: [id: author.id]])
        bindingSource.dataSourceAware = false
        binder.bind publication, bindingSource

        then:
        publication.title == 'Infinite Jest'
        publication.author.name == null

        when:
        bindingSource.dataSourceAware = true
        binder.bind publication, bindingSource

        then:
        publication.title == 'Infinite Jest'
        publication.author.name == 'David Foster Wallace'
    }

    void 'Test binding to the one side of a one to many'() {
        given:
        def author = new Author(name: 'Graeme').save()
        def pub = new Publication(title: 'DGG', author: author)

        when:
        binder.bind pub, new SimpleMapDataBindingSource([publisher: [name: 'Apress']])

        // pending investigation...
        DeferredBindingActions.runActions()

        def publisher = pub.publisher

        then:
        publisher != null

        when:
        publisher.save()

        then:
        pub.publisher.name == 'Apress'
        pub.publisher.publications.size() == 1

        // this is what we are really testing...
        pub.publisher.publications[0] == pub
    }

    void 'Test binding to a hasMany List'() {
        given:
        def publisher = new Publisher()

        when:
        binder.bind publisher, new SimpleMapDataBindingSource([name: 'Apress',
            'publications[0]': [title: 'DGG', author: [name: 'Graeme']],
            'publications[1]': [title: 'DGG2', author: [name: 'Jeff']]])

        then:
        publisher.name == 'Apress'
        publisher.publications instanceof List
        publisher.publications.size() == 2
        publisher.publications[0].title == 'DGG'
        publisher.publications[0].author.name == 'Graeme'
        publisher.publications[0].publisher == publisher
        publisher.publications[1].title == 'DGG2'
        publisher.publications[1].author.name == 'Jeff'
        publisher.publications[1].publisher == publisher
    }

    @Issue('#9003')
    void 'Test binding an array of ids to a collection of persistent instances'() {
        given:
        def book = new AssociationBindingBook()

        when:
        def pInitial = new AssociationBindingPage(number: 1).save()
        book.addToPages(pInitial)
        def p1 = new AssociationBindingPage(number: 42).save()
        def p2 = new AssociationBindingPage(number: 2112).save()

        then:
        p1.id != null
        p2.id != null
        book.pages?.find { it.number == 1 && it.id == pInitial.id }

        when:
        binder.bind book, [pages: [p1.id, p2.id] as String[]] as SimpleMapDataBindingSource

        then: 'the initial page should have been replaced by the 2 new pages'
        book.pages?.size() == 2
        book.pages.find { it.number == 42 && it.id == p1.id }
        book.pages.find { it.number == 2112 && it.id == p2.id }
    }

    void 'Test bindable'() {
        given:
        def widget = new Widget()

        when:
        binder.bind widget, new SimpleMapDataBindingSource([isBindable: 'Should Be Bound', isNotBindable: 'Should Not Be Bound'])

        then:
        widget.isBindable == 'Should Be Bound'
        widget.isNotBindable == null
    }

    void 'Test binding to a collection of String'() {
        given:
        def book = new DataBindingBook()

        when:
        binder.bind book, new SimpleMapDataBindingSource([topics: ['journalism', null, 'satire']])
        binder.bind book, new SimpleMapDataBindingSource(['topics[1]': 'counterculture'])

        then:
        book.topics == ['journalism', 'counterculture', 'satire']
    }

    void 'Test binding to a collection of Integer'() {
        given:
        def book = new DataBindingBook()

        when:
        binder.bind book, new SimpleMapDataBindingSource([importantPageNumbers: ['5', null, '42']])
        binder.bind book, new SimpleMapDataBindingSource(['importantPageNumbers[1]': '2112'])

        then:
        book.importantPageNumbers == [5, 2112, 42]
    }

    void 'Test binding to a collection of primitive'() {
        given:
        def parent = new Parent()

        when:
        binder.bind parent, new SimpleMapDataBindingSource([child: [someOtherIds: '4']])

        then:
        parent.child.someOtherIds.size() == 1
        parent.child.someOtherIds.contains(4)

        when:
        parent.child = null
        binder.bind(parent,  new SimpleMapDataBindingSource([child: [someOtherIds: ['4', '5', '6']]]))

        then:
        parent.child.someOtherIds.size() == 3
        parent.child.someOtherIds.contains(4)
        parent.child.someOtherIds.contains(5)
        parent.child.someOtherIds.contains(6)

        when:
        parent.child = null
        binder.bind(parent,  new SimpleMapDataBindingSource([child: [someOtherIds: 4]]))

        then:
        parent.child.someOtherIds.size() == 1
        parent.child.someOtherIds.contains(4)
    }

    void 'Test unbinding a Map entry'() {
        given:
        def team = new Team()

        when:
        team.members = ['jeff': new Author(name: 'Jeff Scott Brown'),'betsy': new Author(name: 'Sarah Elizabeth Brown')]

        then:
        team.members.size() == 2
        team.members.containsKey('betsy')
        team.members.containsKey('jeff')
        'Sarah Elizabeth Brown' == team.members.betsy.name
        'Jeff Scott Brown' == team.members.jeff.name

        when:
        binder.bind team, new SimpleMapDataBindingSource(['members[jeff]': [id: 'null']])

        then:
        team.members.size() == 1
        team.members.containsKey('betsy')
        'Sarah Elizabeth Brown' == team.members.betsy.name
    }

    void 'Test binding to a Map for new instance with quoted key'() {
        given:
        def team = new Team()

        when:
        binder.bind team, new SimpleMapDataBindingSource(["members['jeff']": [name: 'Jeff Scott Brown'], 'members["betsy"]': [name: 'Sarah Elizabeth Brown']])

        then:
        team.members.size() == 2
        assert team.members.jeff instanceof Author
        assert team.members.betsy instanceof Author
        team.members.jeff.name == 'Jeff Scott Brown'
        team.members.betsy.name == 'Sarah Elizabeth Brown'
    }

    void 'Test binding to Set with subscript'() {
        given:
        def pub = new Publisher()

        when:
        binder.bind pub, new SimpleMapDataBindingSource(['authors[0]': [name: 'Author Uno'], 'authors[1]': [name: 'Author Dos']])

        then:
        pub.authors.size() == 2
        pub.authors.find {it.name == 'Author Uno'}
        pub.authors.find {it.name == 'Author Dos'}
    }

    void 'Test binding existing entities to a new Set'() {
        when:
        def a1 = new Author(name: 'Author One').save()
        def a2 = new Author(name: 'Author Two').save(flush:true)

        then:
        a2
        a1

        when:
        def pub = new Publisher()
        binder.bind pub, new SimpleMapDataBindingSource(['authors[0]': [id: a1.id], 'authors[1]': [id: a2.id]])

        then:
        pub.authors.size() == 2
        pub.authors.find { it.name == 'Author One' } != null
        pub.authors.find { it.name == 'Author Two' } != null
    }

    void 'Test binding a String to an domain class object reference in a Collection'() {
        when:
        def a1 = new Author(name: 'Author One').save()
        def a2 = new Author(name: 'Author Two').save(flush:true)

        then:
        a2
        a1

        when:
        def pub = new Publisher()
        String stringToBind = a2.id as String
        binder.bind pub, new SimpleMapDataBindingSource(['authors[0]': stringToBind])

        then:
        pub.authors.size() == 1
        pub.authors.find { it.name == 'Author Two' } != null
    }


    void 'Test binding a String to a domain class object reference'() {
        given:
        def author = new Author(name: 'Lewis Black')

        when:
        author.save()

        then:
        author.id !=  null

        when:
        def publication = new Publication()
        def bindingSource = [title: 'Me Of Little Faith', author: author.id.toString()] as SimpleMapDataBindingSource
        binder.bind publication, bindingSource

        then:
        publication.author.name == 'Lewis Black'
        publication.title == 'Me Of Little Faith'
        publication.author.is author
    }

    void 'Test updating Set elements by id and subscript operator'() {
        when:
        def a1 = new Author(name: 'Author One').save()
        def a2 = new Author(name: 'Author Two').save()
        def a3 = new Author(name: 'Author Three').save()
        def publisher = new Publisher(name: 'Some Publisher')
        publisher.addToAuthors(a1)
        publisher.addToAuthors(a2)
        publisher.addToAuthors(a3)

        then:
        a1.id != null
        a2.id != null
        a3.id != null

        when:
        // the subscript values are not important, the ids drive selection from the Set
        binder.bind publisher, new SimpleMapDataBindingSource(['authors[123]': [id: a3.id, name: 'Author Tres'],
                                'authors[456]': [id: a1.id, name: 'Author Uno'],
                                'authors[789]': [id: a2.id, name: 'Author Dos']])
        def updatedA1 = publisher.authors.find { it.id == a1.id }
        def updatedA2 = publisher.authors.find { it.id == a2.id }
        def updatedA3 = publisher.authors.find { it.id == a3.id }

        then:
        updatedA1.name == 'Author Uno'
        updatedA2.name == 'Author Dos'
        updatedA3.name == 'Author Tres'
    }

    void 'Test updating Set elements by id'() {
        when:
        def a1 = new Author(name: 'Author One').save()
        def a2 = new Author(name: 'Author Two').save()
        def a3 = new Author(name: 'Author Three').save()
        def publisher = new Publisher(name: 'Some Publisher')
        publisher.addToAuthors(a1)
        publisher.addToAuthors(a2)
        publisher.addToAuthors(a3)

        then:
        a1.id != null
        a2.id != null
        a3.id != null

        when:
        binder.bind publisher, new SimpleMapDataBindingSource(['authors': [
                                [id: a3.id, name: 'Author Tres'],
                                [id: a1.id, name: 'Author Uno'],
                                [id: a2.id, name: 'Author Dos']]])
        def updatedA1 = publisher.authors.find { it.id == a1.id }
        def updatedA2 = publisher.authors.find { it.id == a2.id }
        def updatedA3 = publisher.authors.find { it.id == a3.id }

        then:
        publisher.authors.size()
        updatedA1.name == 'Author Uno'
        updatedA2.name == 'Author Dos'
        updatedA3.name == 'Author Tres'
    }

    void 'Test updating Set elements by id in addition to adding new elements'() {
        when:
        def a1 = new Author(name: 'Author One').save()
        def a2 = new Author(name: 'Author Two').save()
        def a3 = new Author(name: 'Author Three').save()
        def publisher = new Publisher(name: 'Some Publisher')
        publisher.addToAuthors(a1)
        publisher.addToAuthors(a2)
        publisher.addToAuthors(a3)

        then:
        a1.id != null
        a2.id != null
        a3.id != null

        when:
        binder.bind publisher, new SimpleMapDataBindingSource(['authors': [
                                [id: a3.id, name: 'Author Tres'],
                                [id: a1.id, name: 'Author Uno'],
                                [name: 'Author Uno Part Two'],
                                [id: a2.id, name: 'Author Dos']]])
        def updatedA1 = publisher.authors.find { it.id == a1.id }
        def updatedA1Part2 = publisher.authors.find { it.name == 'Author Uno Part Two' }
        def updatedA2 = publisher.authors.find { it.id == a2.id }
        def updatedA3 = publisher.authors.find { it.id == a3.id }

        then:
        publisher.authors.size() == 4
        updatedA1Part2
        updatedA1.name == 'Author Uno'
        updatedA2.name == 'Author Dos'
        updatedA3.name == 'Author Tres'
    }

    void 'Test binding a List of Maps to a persistent Set'() {
        when:
        def publisher = new Publisher(name: 'Some Publisher')
        binder.bind publisher, new SimpleMapDataBindingSource(['authors': [
                                [name: 'Author One'],
                                [name: 'Author Two'],
                                [name: 'Author Three']]])
        def a1 = publisher.authors.find { it.name == 'Author One' }
        def a2 = publisher.authors.find { it.name == 'Author Two' }
        def a3 = publisher.authors.find { it.name == 'Author Three' }
        
        then:
        a1
        a2
        a3
        publisher.authors.size() == 3
    }

    void 'Test updating a Set element by id that does not exist'() {
        given:
        def bindingErrors = []
        def listener = new DataBindingListenerAdapter() {
            void bindingError(BindingError error, errors) {
                bindingErrors << error
            }
        }

        when:
        def publisher = new Publisher(name: 'Apress').save()
        publisher.save(flush: true)
        binder.bind publisher,new SimpleMapDataBindingSource( ['authors[0]': [id: 42, name: 'Some Name']]), listener

        then:
        bindingErrors?.size() == 1

        when:
        def error = bindingErrors[0]

        then:
        error.propertyName == 'authors'
        error.cause?.message == 'Illegal attempt to update element in [authors] Set with id [42]. No such record was found.'
    }

    void 'Test updating nested entities retrieved by id'() {
        when:
        def publisher = new Publisher(name: 'Apress').save()
        def publication = new Publication(title: 'Definitive Guide To Grails', author: new Author(name: 'Author Name'))
        publisher.addToPublications(publication)
        publisher.save(flush: true)
        then:
        publication.publisher != null
        publication.id != null

        when:
        binder.bind publisher, new SimpleMapDataBindingSource(['publications[0]': [id: publication.id, title: 'Definitive Guide To Grails 2']])

        then:
        publisher.publications[0].title == 'Definitive Guide To Grails 2'
    }

    void 'Test using @BindUsing to initialize property with a type other than the declared type'() {
        given:
        def author = new Author()

        when:
        binder.bind author, new SimpleMapDataBindingSource([widget: [name: 'Some Name', isBindable: 'Some Bindable String']])

        then:
        // should be a Fidget, not a Widget
        author.widget instanceof Fidget

        // property in Fidget
        author.widget.name == 'Some Name'

        // property in Widget
        author.widget.isBindable == 'Some Bindable String'
    }

    void 'Test binding to different collection types'() {
        given:
        def obj = new CollectionContainer()
        def map = [:]

//        map['collectionOfWidgets[0]'] = [isBindable: 'Is Uno (Collection)', isNotBindable: 'Is Not Uno (Collection)']
//        map['collectionOfWidgets[1]'] = [isBindable: 'Is Dos (Collection)', isNotBindable: 'Is Not Dos (Collection)']
//        map['collectionOfWidgets[2]'] = [isBindable: 'Is Tres (Collection)', isNotBindable: 'Is Not Tres (Collection)']

        map['listOfWidgets[0]'] = [isBindable: 'Is Uno (List)', isNotBindable: 'Is Not Uno (List)']
        map['listOfWidgets[1]'] = [isBindable: 'Is Dos (List)', isNotBindable: 'Is Not Dos (List)']
        map['listOfWidgets[2]'] = [isBindable: 'Is Tres (List)', isNotBindable: 'Is Not Tres (List)']

        map['setOfWidgets[0]'] = [isBindable: 'Is Uno (Set)', isNotBindable: 'Is Not Uno (Set)']
        map['setOfWidgets[1]'] = [isBindable: 'Is Dos (Set)', isNotBindable: 'Is Not Dos (Set)']
        map['setOfWidgets[2]'] = [isBindable: 'Is Tres (Set)', isNotBindable: 'Is Not Tres (Set)']

        map['sortedSetOfWidgets[0]'] = [isBindable: 'Is Uno (SortedSet)', isNotBindable: 'Is Not Uno (SortedSet)']
        map['sortedSetOfWidgets[1]'] = [isBindable: 'Is Dos (SortedSet)', isNotBindable: 'Is Not Dos (SortedSet)']
        map['sortedSetOfWidgets[2]'] = [isBindable: 'Is Tres (SortedSet)', isNotBindable: 'Is Not Tres (SortedSet)']

        when:
        binder.bind obj, new SimpleMapDataBindingSource(map)
        def listOfWidgets = obj.listOfWidgets
        def setOfWidgets = obj.setOfWidgets
        def collectionOfWidgets = obj.collectionOfWidgets
        def sortedSetOfWidgets = obj.sortedSetOfWidgets

        then:
        listOfWidgets instanceof List
        listOfWidgets.size() == 3
        listOfWidgets[0].isBindable == 'Is Uno (List)'
        listOfWidgets[0].isNotBindable == null
        listOfWidgets[1].isBindable == 'Is Dos (List)'
        listOfWidgets[1].isNotBindable == null
        listOfWidgets[2].isBindable == 'Is Tres (List)'
        listOfWidgets[2].isNotBindable == null

        setOfWidgets instanceof Set
        !(setOfWidgets instanceof SortedSet)
        setOfWidgets.size() == 3
        setOfWidgets.find { it.isBindable == 'Is Uno (Set)' && it.isNotBindable == null }
        setOfWidgets.find { it.isBindable == 'Is Dos (Set)' && it.isNotBindable == null }
        setOfWidgets.find { it.isBindable == 'Is Tres (Set)' && it.isNotBindable == null }

        sortedSetOfWidgets instanceof SortedSet
        sortedSetOfWidgets.size() == 3
        sortedSetOfWidgets[0].isBindable == 'Is Dos (SortedSet)'
        sortedSetOfWidgets[1].isBindable == 'Is Tres (SortedSet)'
        sortedSetOfWidgets[2].isBindable == 'Is Uno (SortedSet)'
    }

    void 'Test that binding errors are populated on a @Validateable instance'() {
        given:
        def obj = new SomeValidateableClass()

        when: 'binding with just a binding source'
        binder.bind obj, [someNumber: 'not a number'] as SimpleMapDataBindingSource

        then:
        obj.hasErrors()
        obj.errors.errorCount == 1
        obj.errors['someNumber'].code == 'typeMismatch'

        when:
        obj.clearErrors()

        then:
        !obj.hasErrors()

        when: 'binding with a binding source and a white list'
        binder.bind obj, [someNumber: 'not a number'] as SimpleMapDataBindingSource, ['someNumber']

        then:
        obj.hasErrors()
        obj.errors.errorCount == 1
        obj.errors['someNumber'].code == 'typeMismatch'

        when:
        obj.clearErrors()

        then:
        !obj.hasErrors()

        when: 'binding with a binding source, a white list and a black list'
        binder.bind obj, [someNumber: 'not a number'] as SimpleMapDataBindingSource, ['someNumber'], ['someOtherProperty']

        then:
        obj.hasErrors()
        obj.errors.errorCount == 1
        obj.errors['someNumber'].code == 'typeMismatch'
        when:
        obj.clearErrors()

        then:
        !obj.hasErrors()

        when: 'binding with a binding source and a listener'
        def beforeBindingArgs = []
        def bindingErrorArgs = []
        def afterBindingArgs = []
        def bindingErrors = []
        def listener = new DataBindingListenerAdapter() {
            Boolean beforeBinding(object, String propertyName, value, errors) {
                beforeBindingArgs << [object: object, propName: propertyName, value: value]
                true
            }

            void afterBinding(object, String propertyName, errors) {
                afterBindingArgs << [object: object, propertyName: propertyName]
            }

            void bindingError(BindingError error, errors) {
                bindingErrorArgs << error
            }
        }

        binder.bind obj, [someNumber: 'not a number'] as SimpleMapDataBindingSource, listener

        then:
        obj.hasErrors()
        obj.errors.errorCount == 1
        obj.errors['someNumber'].code == 'typeMismatch'
        beforeBindingArgs.size() == 1
        beforeBindingArgs[0].object.is obj
        beforeBindingArgs[0].propName == 'someNumber'
        beforeBindingArgs[0].value == 'not a number'
        bindingErrorArgs.size() == 1
        bindingErrorArgs[0].object.is obj
        bindingErrorArgs[0].propertyName == 'someNumber'
        bindingErrorArgs[0].rejectedValue == 'not a number'
        afterBindingArgs.size() == 1
        afterBindingArgs[0].object.is obj
        afterBindingArgs[0].propertyName == 'someNumber'
    }
    
    void 'Test binding a List<String>'() {
        given:
        def obj = new CollectionContainer()
        
        when:
        binder.bind obj, [listOfStrings: ['One', 'Two', 'Three']] as SimpleMapDataBindingSource
        
        then:
        obj.listOfStrings == ['One', 'Two', 'Three']
    }
    
    void 'Test one to many list binding with nested subscript operator can insert to empty index of List'() {
        when:
        def author = new AssociationBindingAuthor(name: "William Gibson").save()
        def page1 = new AssociationBindingPage(number: 1).save()
        def page2 = new AssociationBindingPage(number: 2).save()
        def book = new AssociationBindingBook()
        binder.bind book, [title: "Pattern Recognition", author: author, pages: [null, page2]] as SimpleMapDataBindingSource
        book.save()
        binder.bind author, ["books[0]": ['pages[0]': [id: page1.id]]] as SimpleMapDataBindingSource

        then:
        2 == author.books[0].pages.size()
        author.books[0].pages.find { it.id == page1.id }
        author.books[0].pages.find { it.id == page2.id }
        2 == author.books.sum { it.pages.size() }
        !author.books.any { it.pages.contains(null) }
    }

    void 'Test typeMismatch error codes'() {
        given:
        def obj = new SomeValidateableClass()

        when:
        binder.bind obj, [someNumber: 'not a number'] as SimpleMapDataBindingSource

        then:
        obj.hasErrors()
        obj.errors.errorCount == 1
        obj.errors['someNumber'].codes == [
            "grails.web.databinding.SomeValidateableClass.someNumber.typeMismatch.error",
            "grails.web.databinding.SomeValidateableClass.someNumber.typeMismatch",
            "someValidateableClass.someNumber.typeMismatch.error",
            "someValidateableClass.someNumber.typeMismatch",
            "typeMismatch.grails.web.databinding.SomeValidateableClass.someNumber",
            "typeMismatch.someNumber",
            "typeMismatch.java.lang.Integer",
            "typeMismatch"
        ]
    }
    
    @Issue('GRAILS-10696')
    void 'Test binding a simple String to a List<Long> on a non domain class'() {
        given:
        def obj = new SomeNonDomainClass()
        
        when:
        binder.bind obj, [listOfLong: '42'] as SimpleMapDataBindingSource
        
        then:
        obj.listOfLong[0] == 42
    }
    
    @Issue('GRAILS-10689')
    void 'Test binding a String[] to a List<Long> on a non domain class'() {
        given:
        def obj = new SomeNonDomainClass()
        
        when:
        binder.bind obj, [listOfLong: ['42', '2112'] as String[]] as SimpleMapDataBindingSource
        
        then:
        obj.listOfLong.size() == 2
        obj.listOfLong[0] == 42
        obj.listOfLong[1] == 2112
    }

    @Issue('GRAILS-10696')
    void 'Test binding a simple String to a List<Long> on a domain class'() {
        given:
        def obj = new CollectionContainer()
        
        when:
        binder.bind obj, [listOfLong: '42'] as SimpleMapDataBindingSource
        
        then:
        obj.listOfLong[0] == 42
    }

    @Issue('GRAILS-10689')
    void 'Test binding  String[] to a List<Long> on a domain class'() {
        given:
        def obj = new CollectionContainer()
        
        when:
        binder.bind obj, [listOfLong: ['42', '2112'] as String[]] as SimpleMapDataBindingSource
        
        then:
        obj.listOfLong.size() == 2
        obj.listOfLong[0] == 42
        obj.listOfLong[1] == 2112
    }
    
    void 'Test @BindUsing on a List of domain objects'() {
        given:
        def pub = new Publisher()
        
        when:
        binder.bind pub, [widgets: '4'] as SimpleMapDataBindingSource
        
        then:
        pub.widgets.size() == 4
        pub.widgets[0] instanceof Widget
        pub.widgets[1] instanceof Widget
        pub.widgets[2] instanceof Widget
        pub.widgets[3] instanceof Widget
    }

    void 'Test @BindUsing on a List<Integer>'() {
        given:
        def widget = new Widget()
        
        when:
        binder.bind widget, [listOfIntegers: '4'] as SimpleMapDataBindingSource
        
        then:
        widget.listOfIntegers == [0, 1, 2, 3]
    }
    
    @Issue('GRAILS-10899')
    @Ignore
    void 'Test binding to a property that has a getter and setter with declared type java.util.Collection'() {
        when:
        def f = new Foo(airports: ['STL', 'LHR', 'MIA'])
        
        then:
        f.airports == ['STL', 'LHR', 'MIA']
    }
    
    @Issue('GRAILS-10899')
    void 'Test binding to a collection of values which need to be converted to a collection property that has a getter and setter with declared type java.util.Collection'() {
        when:
        def f = new Foo(numbers: ['2112', '42', '0'])
        
        then:
        f.numbers == [0, 42, 2112] as Set
    }
    
    @Issue('GRAILS-10728')
    @Ignore
    void 'Test binding to a Set property that has a getter which returns an unmodifiable Set'() {
        when:
        def f = new Foo(names: ['Lemmy', 'Phil', 'Mikkey'] as Set)
        
        then:
        f.names == ['Lemmy', 'Phil', 'Mikkey'] as Set
    }
    
    @Issue('GRAILS-10728')
    @Ignore
    void 'Test binding to a collection property that has a setter and no getter'() {
        when:
        def f = new Foo(workdays: [Calendar.MONDAY, Calendar.TUESDAY])
        
        then:
        f.getTheValueOfWorkdays() == [Calendar.MONDAY, Calendar.TUESDAY] as Set
    }

    @Issue('GRAILS-10717')
    @Ignore
    void 'Test binding to a property that does not correspond to a field'() {
        when:
        def f = new Foo(activeDays:['mon'])
        
        then:
        f.activeDays == ['mon']
    }
    
    @Issue('GRAILS-10790')
    void 'Test binding to a Map on a non domain class'() {
        given:
        def obj = new NonDomainClassWithMapProperty()
        
        when:
        binder.bind obj, [name: 'Alpha Omega', 
                         'albums[uno]': [title: 'Album Number One'],
                         'albums[dos]': [title: 'Album Number Two']] as SimpleMapDataBindingSource
                     
        then:
        obj.name == 'Alpha Omega'
        obj.albums.size() == 2
        obj.albums['uno'] instanceof Album
        obj.albums['uno'].title == 'Album Number One'
        obj.albums['dos'] instanceof Album
        obj.albums['dos'].title == 'Album Number Two'
    }
    
    @Issue(['GRAILS-10796','GRAILS-10829'])
    void 'Test replacing existing collection of persistent entities'() {
        given: 
        def container = new CollectionContainer().save()
        
        when:
        ['one', 'two', 'three'].each { name ->
            def widget = new Widget(isBindable: name)
            widget.isNotBindable = ''
            container.addToSetOfWidgets(widget)
        }
        
        then:
        container.save()
        container.setOfWidgets.size() == 3
        def originalSetOfWidgets = container.setOfWidgets
        
        when: 'A List of ids is bound to the collection container'
        def newWidgets = ['four', 'five'].collect { name ->
            def widget = new Widget(isBindable: name)
            widget.isNotBindable = ''
            widget.save().id
        } 
        binder.bind container, [setOfWidgets: newWidgets] as SimpleMapDataBindingSource
        
        then: 'the set of widgets should have been replaced, not appended to'
        container.setOfWidgets.find { it.isBindable == 'four' }
        container.setOfWidgets.find { it.isBindable == 'five' }
        container.setOfWidgets.size() == 2
        
        and: 'The containing Set is the same set that we started with'
        originalSetOfWidgets.is container.setOfWidgets
    }
    
    @Issue('GRAILS-10910')
    void 'Test binding an empty List to a List property which has elements in it'() {
        given:
        def publisher = new Publisher()
        
        when:
        publisher.addToPublications([title: 'Pub 1'])
        publisher.addToPublications([title: 'Pub 2'])
        
        then:
        publisher.publications.size() == 2
        
        when:
        binder.bind publisher, [publications: []] as SimpleMapDataBindingSource
        
        then:
        publisher.publications.size() == 0
    }
    
    @Issue('GRAILS-11018')
    void 'Test binding an invalid String to a List<Long>'() {
        given:
        def command = new ListCommand()
        
        when:
        binder.bind command, [myLongList: 'a,b,c'] as SimpleMapDataBindingSource
        
        then:
        command.hasErrors()
        command.errors.errorCount == 1
        command.errors['myLongList'].code == 'typeMismatch'
    }
    
    void 'Test binding to indexes of a List of Long which leaves gaps in the List'() {
        given:
        def obj = new SomeNonDomainClass()
        
        when:
        binder.bind obj, ['listOfLong[1]': 1, 'listOfLong[5]': 5, 'listOfLong[3]': 3] as SimpleMapDataBindingSource
        
        then:
        obj.listOfLong.size() == 6
        obj.listOfLong[0] == null
        obj.listOfLong[1] == 1
        obj.listOfLong[2] == null
        obj.listOfLong[3] == 3
        obj.listOfLong[4] == null
        obj.listOfLong[5] == 5
    }
    
    
    void 'Test binding to indexes of a List of domain objects which leaves gaps in the List'() {
        given:
        def obj = new CollectionContainer()
        
        when:
        binder.bind obj, ['listOfWidgets[1]': ['isBindable': 'one'], 'listOfWidgets[5]': ['isBindable': 'five'], 'listOfWidgets[3]': ['isBindable': 'three']] as SimpleMapDataBindingSource
        
        then:
        obj.listOfWidgets.size() == 6
        obj.listOfWidgets[0] == null
        obj.listOfWidgets[1].isBindable == 'one'
        obj.listOfWidgets[2] == null
        obj.listOfWidgets[3].isBindable == 'three'
        obj.listOfWidgets[4] == null
        obj.listOfWidgets[5].isBindable == 'five'
    }
    
    void 'Test binding to a TimeZone property'() {
        given:
        def obj = new Widget()
        
        when:
        binder.bind obj, [timeZone: 'Europe/Berlin'] as SimpleMapDataBindingSource
        
        then:
        obj.timeZone == TimeZone.getTimeZone('Europe/Berlin')
    }
    
    void 'Test binding to a typed List of non-domain objects'() {
        given:
        def obj = new DocumentHolder()
        
        when:
        binder.bind obj, [objectIds: ['two', 'four', 'six', 'eight']] as SimpleMapDataBindingSource
        
        then:
        obj.objectIds.size() == 4
        obj.objectIds[0] instanceof ObjectId
        obj.objectIds[0].value == 'two'
        obj.objectIds[1] instanceof ObjectId
        obj.objectIds[1].value == 'four'
        obj.objectIds[2] instanceof ObjectId
        obj.objectIds[2].value == 'six'
        obj.objectIds[3] instanceof ObjectId
        obj.objectIds[3].value == 'eight'
    }
    
    void 'Test binding to a typed array of non-domain objects'() {
        given:
        def obj = new DocumentHolder()
        
        when:
        binder.bind obj, [objectIds: ['two', 'four', 'six', 'eight'] as String[]] as SimpleMapDataBindingSource
        
        then:
        obj.objectIds.size() == 4
        obj.objectIds[0] instanceof ObjectId
        obj.objectIds[0].value == 'two'
        obj.objectIds[1] instanceof ObjectId
        obj.objectIds[1].value == 'four'
        obj.objectIds[2] instanceof ObjectId
        obj.objectIds[2].value == 'six'
        obj.objectIds[3] instanceof ObjectId
        obj.objectIds[3].value == 'eight'
    }
    
    @Issue('GRAILS-11174')
    void 'Test binding null to a Date marked with @BindingFormat'() {
        given:
        def obj = new DataBindingBook()
        
        when:
        binder.bind obj, [datePublished: null] as SimpleMapDataBindingSource
        
        then:
        obj.datePublished == null
        !obj.hasErrors()
        
        when:
        obj.datePublished = new Date()
        binder.bind obj, [datePublished: null] as SimpleMapDataBindingSource
        
        then:
        obj.datePublished == null
        !obj.hasErrors()
    }
    
    @Issue('GRAILS-11238')
    void 'Test binding to a property that hides a field of a different type'() {
        when:
        def holder = new AlbumHolder()
        def album = new Album(title: 'Some Album')
        binder.bind holder, [album: album] as SimpleMapDataBindingSource
        
        then:
        holder.album.title == 'Some Album'
    }
    
    @Issue('GRAILS-11402')
    void 'Test binding when the binding source contains the key "_"'() {
        given:
        def publisher = new Publisher()
        
        when:
        binder.bind publisher, [_: '', name: 'Some Publisher'] as SimpleMapDataBindingSource
        
        then:
        !publisher.hasErrors()
        publisher.name == 'Some Publisher'
    }
    
    @Issue('GRAILS-11472')
    void 'test binding an empty string to a Date marked with @BindingFormat'() {
        given:
        def book = new DataBindingBook()
        
        when: 'a valid date string is bound'
        binder.bind book, [datePublished: '11151969'] as SimpleMapDataBindingSource
        
        then: 'the date is initialized'
        !book.hasErrors()
        book.datePublished
        Calendar.NOVEMBER == book.datePublished.month
        15 == book.datePublished.date
        69 == book.datePublished.year
        
        when: 'an empty string is bound'
        binder.bind book, [datePublished: ''] as SimpleMapDataBindingSource
        
        then: 'the date is null'
        book.datePublished == null
        !book.hasErrors()
    }

    void 'test binding an Date to code in @BindingFormat'() {
        given:
        Locale.setDefault(Locale.getInstance("en", "US", ""))
        messageSource.addMessage 'my.date.format', Locale.US, 'MMddyyyy'
        def child = new Child()

        when: 'a valid date string is bound'
        binder.bind child, [birthDate: '11151969'] as SimpleMapDataBindingSource

        then: 'the date is initialized'
        !child.hasErrors()
        child.birthDate
        Calendar.NOVEMBER == child.birthDate.month
        15 == child.birthDate.date
        69 == child.birthDate.year
    }
    
    void 'Test binding String to currency in a domain class'() {
        given:
        def publisher = new Publisher()
        
        when:
        binder.bind publisher, [localCurrency: 'USD'] as SimpleMapDataBindingSource

        then:
        publisher.localCurrency instanceof Currency
        'USD' == publisher.localCurrency.currencyCode
    }
    
    @Issue('GRAILS-11666')
    void 'test binding array of id to a collection of domain instances in a non domain classes'() {
        given:
        def pub1 = new Publisher(name: 'Pub One').save()
        def pub2 = new Publisher(name: 'Pub Two').save()
        def pub3 = new Publisher(name: 'Pub Three').save()
        def obj = new NonDomainClassWithSetOfDomainInstances()
        String[] idArray = [pub1.id, pub3.id] as String[]
        
        when:
        binder.bind obj, [publishers: idArray] as SimpleMapDataBindingSource
        
        then:
        obj.publishers?.size() == 2
        obj.publishers.find { it.name == 'Pub One' }
        obj.publishers.find { it.name == 'Pub Three' }
    }
}

@Entity
class Team {

    Map members
    Map states

    @SuppressWarnings('unused')
    static hasMany = [members: Author, states: String]
}

@Entity
class Publisher {

    String name
    List publications
    
    @BindUsing({ obj, source ->
        def cnt = source['widgets'] as int
        def result = []
        cnt.times { result << new Widget() }
        result
    })
    List widgets = []
    
    Currency localCurrency

    @SuppressWarnings('unused')
    static hasMany = [publications: Publication, authors: Author, widgets: Widget]

    @SuppressWarnings('unused')
    static constraints = {
        localCurrency nullable: true
    }
}

class SomeNonDomainClass {
    Publication publication
    List<Long> listOfLong
}

@Entity
class Publication {

    String title
    Author author

    @SuppressWarnings('unused')
    static belongsTo = [publisher: Publisher]
}

@Entity
class Author {
    String name

    @BindUsing({obj, source ->
        source['stringWithSpecialBinding']?.trim()
    })
    String stringWithSpecialBinding

    @BindUsing({ obj, DataBindingSource source ->
        // could have conditional logic here
        // that instantiates different types
        // based on entries in the source map
        // or some other criteria.
        // in this case, hardcoded to return a
        // particular type.

        new Fidget(source['widget'])
    })
    ParentWidget widget

    @SuppressWarnings('unused')
    static constraints = {
        widget nullable: true
        stringWithSpecialBinding nullable: true
    }
}

@Entity
@Sortable(includes = ["isBindable", "isNotBindable"])
class Widget {

    String isBindable
    String isNotBindable

    @BindUsing({ obj, source ->
        def cnt = source['listOfIntegers'] as int
        def result = []
        cnt.times { c ->
            result << c 
        }
        result
    })
    List<Integer> listOfIntegers = []

    TimeZone timeZone

    @SuppressWarnings('unused')
    static constraints = {
        isNotBindable bindable: false
        timeZone nullable: true
    }
}

// Since Groovy 4, parent domain classes cannot be annotated with @Entity (https://issues.apache.org/jira/browse/GROOVY-5106)
@Sortable(includes = ['isBindable', 'isNotBindable'])
@SuppressWarnings('unused')
class ParentWidget implements Validateable {

    String isBindable
    String isNotBindable

    @BindUsing({ Object obj, DataBindingSource source ->
        def cnt = source['listOfIntegers'] as int
        def result = []
        cnt.times { result << it }
        result
    })
    List<Integer> listOfIntegers = []

    TimeZone timeZone

    static constraints = {
        isNotBindable(bindable: false)
        timeZone(nullable: true)
    }
}

@Entity
class Fidget extends ParentWidget {
    String name
}

@Entity
class Parent {
    Child child
}

@Entity
class Child {

    @BindingFormat(code='my.date.format')
    Date birthDate

    @SuppressWarnings('unused')
    static hasMany = [someOtherIds: Integer]
}

@Entity
class DataBindingBook {

    String title
    List importantPageNumbers
    List topics

    @BindingFormat("MMddyyyy")
    Date datePublished

    @SuppressWarnings('unused')
    static hasMany = [topics: String, importantPageNumbers: Integer]
}

@Entity
class CollectionContainer {

    List listOfWidgets
    SortedSet sortedSetOfWidgets
    Collection collectionOfWidgets
    List<String> listOfStrings
    List<Long> listOfLong

    @SuppressWarnings('unused')
    static hasMany = [listOfWidgets: Widget,
                      setOfWidgets: Widget,
                      collectionOfWidgets: Widget,
                      sortedSetOfWidgets: Widget]
}

class DocumentHolder {
    List<ObjectId> objectIds
}

class ObjectId {
    String value
    
    ObjectId(String str) {
        value = str
    }
}

class PrimitiveContainer implements Validateable {
    boolean someBoolean
    byte someByte
    char someChar
    short someShort
    int someInt
    long someLong
    float someFloat
    double someDouble
}

class SomeValidateableClass implements Validateable {
    Integer someNumber
}

@Entity
class AssociationBindingPage {
    Integer number
}

@Entity
class AssociationBindingBook {

    String title
    List pages

    @SuppressWarnings('unused')
    static belongsTo = [author: AssociationBindingAuthor]

    @SuppressWarnings('unused')
    static hasMany = [pages: AssociationBindingPage]
}

@Entity
class AssociationBindingAuthor {

    String name
    List books

    @SuppressWarnings('unused')
    static hasMany = [books: AssociationBindingBook]
}

@Entity
class Foo {

    Boolean activeMonday
    Collection<Integer> numbers

    private Set<String> _names

    private transient Collection<String> _airports
    private transient Set<Integer> _workdays

    @SuppressWarnings('unused')
    static constraints = {
        activeDays bindable: true
    }

    @SuppressWarnings('unused')
    static transients = ['activeDays']
    
    List getActiveDays() {
        def activeDays = []
        if (activeMonday) activeDays << 'mon'
        activeDays
    }

    void setActiveDays(List activeDays) {
        if (activeDays.contains('mon')) {
            activeMonday = true
        }
    }
    
    void setWorkdays(Collection<Integer> workdays) {
        _workdays = new HashSet<Integer>(workdays)
    }
    
    def getTheValueOfWorkdays() {
        _workdays
    }
    
    void setNames(Set<String> names) {
        _names = names
    }
    
    Set<String> getNames() {
        Collections.unmodifiableSet(_names ?: [] as Set) as Set<String>
    }
    
    void setAirports(Collection<String> airports) {
        _airports = airports
    }
    
    Collection<String> getAirports() {
        _airports
    }
}

class NonDomainClassWithMapProperty {
    String name
    Map<String, Album> albums
}

class NonDomainClassWithSetOfDomainInstances {
    Set<Publisher> publishers
}
class Album {
    String title
}

class AlbumHolder {
    // see GRAILS-11238
    String album
    
    void setAlbum(Album a) {
        album = a.title
    }
    
    Album getAlbum() {
        return new Album(title: album)
    }
}

class ListCommand implements Validateable { 
    List<Long> myLongList 
}
