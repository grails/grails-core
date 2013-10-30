/* Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.orm

import java.util.List;

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.validation.DeferredBindingActions
import grails.validation.Validateable

import org.apache.commons.lang.builder.CompareToBuilder
import org.codehaus.groovy.grails.web.binding.GrailsWebDataBinder
import org.grails.databinding.BindUsing
import org.grails.databinding.BindingFormat
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.errors.BindingError
import org.grails.databinding.events.DataBindingListenerAdapter
import org.springframework.context.support.StaticMessageSource

import spock.lang.Issue
import spock.lang.Specification

import com.google.gson.internal.LazilyParsedNumber

@TestMixin(DomainClassUnitTestMixin)
@Mock([AssociationBindingAuthor, AssociationBindingPage, AssociationBindingBook, Author, Child, CollectionContainer, DataBindingBook, Fidget, Parent, Publication, Publisher, Team, Widget])
class GrailsWebDataBinderSpec extends Specification {

    GrailsWebDataBinder binder
    def messageSource

    void setup() {
        binder = new GrailsWebDataBinder(grailsApplication)
        messageSource = new StaticMessageSource()
        binder.messageSource = messageSource
    }

    void 'Test string trimming'() {
        given:
        def author = new Author()

        when:
        binder.bind author, new SimpleMapDataBindingSource([name: '   Jeff Scott Brown ', stringWithSpecialBinding: '   Jeff Scott Brown '])

        then:
        author.name == 'Jeff Scott Brown'
        author.stringWithSpecialBinding == 'Jeff Scott Brown'

        when:
        def actualName = 'Jeff Scott Brown'
        binder.bind author, new SimpleMapDataBindingSource([name: "   ${actualName} ", stringWithSpecialBinding: "   ${actualName} "])

        then:
        author.name == 'Jeff Scott Brown'
        author.stringWithSpecialBinding == 'Jeff Scott Brown'

        when:
        binder.trimStrings = false
        binder.bind author, new SimpleMapDataBindingSource([name: '  Jeff Scott Brown   ', stringWithSpecialBinding: '  Jeff Scott Brown   '])

        then:
        author.name == '  Jeff Scott Brown   '
        author.stringWithSpecialBinding == 'Jeff Scott Brown'

        when:
        binder.trimStrings = false
        binder.bind author, new SimpleMapDataBindingSource([name: "  ${actualName}   ", stringWithSpecialBinding: "  ${actualName}   "])

        then:
        author.name == '  Jeff Scott Brown   '
        author.stringWithSpecialBinding == 'Jeff Scott Brown'
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

    void 'Test binding to primitives from Strings'() {
        given:
        def obj = new PrimitiveContainer()

        when:
        binder.bind(obj, new SimpleMapDataBindingSource([someBoolean: 'true',
            someByte: '1',
            someChar: 'a',
            someShort: '2',
            someInt: '3',
            someLong: '4',
            someFloat: '5.5',
            someDouble: '6.6']))

        then:
        obj.someBoolean == true
        obj.someByte == 1
        obj.someChar == ('a' as char)
        obj.someShort == 2
        obj.someInt == 3
        obj.someLong == 4
        obj.someFloat == 5.5
        obj.someDouble == 6.6
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def author = new Author(name: 'David Foster Wallace').save(flush: true)
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

    void 'Test binding an array of ids to a collection of persistent instances'() {
        given:
        def book = new AssociationBindingBook()
        
        when:
        def p1 = new AssociationBindingPage(number: 42).save()
        def p2 = new AssociationBindingPage(number: 2112).save()
        
        then:
        p1.id != null
        p2.id != null
        
        when:
        binder.bind book, [pages: [p1.id, p2.id] as String[]] as SimpleMapDataBindingSource
        
        then:
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
        binder.bind team, new SimpleMapDataBindingSource(bindingSource)

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
        binder.bind team, new SimpleMapDataBindingSource(bindingSource)

        then:
        team.members.size() == 2
        team.members.containsKey('jeff')
        team.members.containsKey('betsy')
        team.members.jeff instanceof Author
        team.members.betsy instanceof Author
        team.members.jeff.name == 'Jeff Scott Brown'
        team.members.betsy.name == 'Sarah Elizabeth Brown'
    }

    void 'Test binding to Set with subscript'() {
        given:
        def pub = new Publisher()
        pub.addToAuthors(name: 'Author One')

        when:
        binder.bind pub, new SimpleMapDataBindingSource(['authors[0]': [name: 'Author Uno'], 'authors[1]': [name: 'Author Dos']])

        then:
        pub.authors.size() == 2
        pub.authors[0].name == 'Author Uno'
        pub.authors[1].name == 'Author Dos'
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

    void 'Test binding a gson LazilyParsedNumber to a domain class object reference'() {
        given:
        def author = new Author(name: 'Lewis Black')

        when:
        author.save()

        then:
        author.id !=  null

        when:
        def publication = new Publication()
        def bindingSource = [title: 'Me Of Little Faith', author: new LazilyParsedNumber(author.id.toString())] as SimpleMapDataBindingSource
        binder.bind publication, bindingSource

        then:
        publication.author.name == 'Lewis Black'
        publication.title == 'Me Of Little Faith'
        publication.author.is author
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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

    void 'Test binding format code'() {
        given:
        messageSource.addMessage 'my.date.format', Locale.US, 'MMddyyyy'
        messageSource.addMessage 'my.date.format', Locale.UK, 'ddMMyyyy'
        def child = new Child()

        when:
        binder = new GrailsWebDataBinder(grailsApplication)  {
            Locale getLocale() {
                Locale.US
            }
        }
        binder.messageSource = messageSource
        binder.bind child, new SimpleMapDataBindingSource([birthDate: '11151969'])
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
        binder.bind child, new SimpleMapDataBindingSource([birthDate: '15111969'])
        birthDate = child.birthDate

        then:
        Calendar.NOVEMBER == birthDate.month
        15 == birthDate.date
        69 == birthDate.year
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
            "org.codehaus.groovy.grails.orm.SomeValidateableClass.someNumber.typeMismatch.error",
            "org.codehaus.groovy.grails.orm.SomeValidateableClass.someNumber.typeMismatch",
            "someValidateableClass.someNumber.typeMismatch.error",
            "someValidateableClass.someNumber.typeMismatch",
            "typeMismatch.org.codehaus.groovy.grails.orm.SomeValidateableClass.someNumber",
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
}

@Entity
class Team {
    static hasMany = [members: Author, states: String]
    Map members
    Map states
}

@Entity
class Publisher {
    String name
    static hasMany = [publications: Publication, authors: Author, widgets: Widget]
    List publications
    
    @BindUsing({ obj, source ->
        def cnt = source['widgets'] as int
        def result = []
        cnt.times { result << new Widget() }
        result
    })
    List widgets = []
}

class SomeNonDomainClass {
    Publication publication
    List<Long> listOfLong
}

@Entity
class Publication {
    String title
    Author author
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
    Widget widget

    static constraints = {
        widget nullable: true
        stringWithSpecialBinding nullable: true
    }
}

@Entity
class Widget implements Comparable {
    String isBindable
    String isNotBindable
    @BindUsing({ obj, source ->
        def cnt = source['listOfIntegers'] as int
        def result = []
        cnt.times { c -> 
            result << c 
        }
        println "Result: $result"
        result
    })
    List listOfIntegers = []

    static constraints = {
        isNotBindable bindable: false
    }

    int compareTo(Object rhs) {
        new CompareToBuilder().append(isBindable, rhs.isBindable).append(isNotBindable, rhs.isNotBindable).toComparison()
    }
}

@Entity
class Fidget extends Widget {
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
    static hasMany = [someOtherIds: Integer]
}

@Entity
class DataBindingBook {
    String title
    List importantPageNumbers
    List topics
    static hasMany = [topics: String, importantPageNumbers: Integer]
}

@Entity
class CollectionContainer {
    static hasMany = [listOfWidgets: Widget,
                      setOfWidgets: Widget,
                      collectionOfWidgets: Widget,
                      sortedSetOfWidgets: Widget]
    List listOfWidgets
    SortedSet sortedSetOfWidgets
    Collection collectionOfWidgets
    List<String> listOfStrings
    List<Long> listOfLong
}

class PrimitiveContainer {
    boolean someBoolean
    byte someByte
    char someChar
    short someShort
    int someInt
    long someLong
    float someFloat
    double someDouble
}

@Validateable
class SomeValidateableClass {
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
    static belongsTo = [author: AssociationBindingAuthor]
    static hasMany = [pages:AssociationBindingPage]
}

@Entity
class AssociationBindingAuthor {
    String name
    static hasMany = [books: AssociationBindingBook]
}
