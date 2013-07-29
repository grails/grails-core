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

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.validation.DeferredBindingActions

import org.apache.commons.lang.builder.CompareToBuilder
import org.codehaus.groovy.grails.web.binding.GrailsWebDataBinder
import org.grails.databinding.BindUsing
import org.grails.databinding.BindingFormat
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.errors.BindingError
import org.grails.databinding.events.DataBindingListener
import org.springframework.context.support.StaticMessageSource

import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
@Mock([Author, Child, CollectionContainer, DataBindingBook, Fidget, Parent, Publication, Publisher, Team, Widget])
class GrailsWebDataBinderSpec extends Specification {

    void 'Test string trimming'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
        def publication = new Publication()

        when:
        binder.bind publication, new SimpleMapDataBindingSource([author: '42'])

        then:
        publication.author == null
    }

    void 'Test binding empty and blank String'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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

    void 'Test id binding'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
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
    }

    void 'Test id binding with a non dataSource aware binding source'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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

    void 'Test bindable'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
        def widget = new Widget()

        when:
        binder.bind widget, new SimpleMapDataBindingSource([isBindable: 'Should Be Bound', isNotBindable: 'Should Not Be Bound'])

        then:
        widget.isBindable == 'Should Be Bound'
        widget.isNotBindable == null
    }

    void 'Test binding to a collection of String'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
        def book = new DataBindingBook()

        when:
        binder.bind book, new SimpleMapDataBindingSource([topics: ['journalism', null, 'satire']])
        binder.bind book, new SimpleMapDataBindingSource(['topics[1]': 'counterculture'])

        then:
        book.topics == ['journalism', 'counterculture', 'satire']
    }

    void 'Test binding to a collection of Integer'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
        def book = new DataBindingBook()

        when:
        binder.bind book, new SimpleMapDataBindingSource([importantPageNumbers: ['5', null, '42']])
        binder.bind book, new SimpleMapDataBindingSource(['importantPageNumbers[1]': '2112'])

        then:
        book.importantPageNumbers == [5, 2112, 42]
    }

    void 'Test binding to a collection of primitive'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)

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
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)

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
        def binder = new GrailsWebDataBinder(grailsApplication)
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

    void 'Test updating a Set element by id that does not exist'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
        def bindingErrors = []
        def listener = { BindingError error ->
            bindingErrors << error
        } as DataBindingListener

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
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)

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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def binder = new GrailsWebDataBinder(grailsApplication)
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
        def messageSource = new StaticMessageSource()
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
    static hasMany = [publications: Publication, authors: Author]
    List publications
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
