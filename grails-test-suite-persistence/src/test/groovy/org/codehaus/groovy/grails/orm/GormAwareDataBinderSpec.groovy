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
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.validation.DeferredBindingActions

import org.codehaus.groovy.grails.web.binding.GormAwareDataBinder

import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
class GormAwareDataBinderSpec extends Specification {

    protected dataBinder
    
    protected doBind(obj, source) {
        if(dataBinder == null) {
            dataBinder = new GormAwareDataBinder(grailsApplication)
        }
        dataBinder.bind obj, source
    }
    
    protected doBind(obj, source, whiteList, blackList) {
        if(dataBinder == null) {
            dataBinder = new GormAwareDataBinder(grailsApplication)
        }
        dataBinder.bind obj, source, null, whiteList, blackList, null
    }
    
    void 'Test string trimming'() {
        given:
        def binder = new GormAwareDataBinder(grailsApplication)
        mockDomain Author
        def author = new Author()
        
        when:
        binder.bind author, [name: '   Jeff Scott Brown ', nullableString: ' ']
        
        then:
        author.name == 'Jeff Scott Brown'
        author.nullableString == ''
        
        when:
        def actualName = 'Jeff Scott Brown'
        def space = ' '
        binder.bind author, [name: "   ${actualName} ", nullableString: "${space}"]
        
        then:
        author.name == 'Jeff Scott Brown'
        author.nullableString == ''
        
        when:
        binder.trimStrings = false
        binder.bind author, [name: '  Jeff Scott Brown   ', nullableString: ' ']
        
        then:
        author.name == '  Jeff Scott Brown   '
        author.nullableString == ' '
        
        when:
        binder.trimStrings = false
        binder.bind author, [name: "  ${actualName}   ", nullableString: "${space}"]
        
        then:
        author.name == '  Jeff Scott Brown   '
        author.nullableString == ' '

    }
    
    void 'Test binding empty String to nullable property'() {
        given:
        mockDomain Author
        def binder = new GormAwareDataBinder(grailsApplication)
        def obj = new Author()
        
        when:
        doBind obj, [nullableString: '']
        
        then:
        obj.nullableString == null
        
        when:
        def emptyString = ''
        doBind obj, [nullableString: "${emptyString}"]
        
        then:
        obj.nullableString == null
    }
    
    void 'Test binding blank String to nullable property'() {
        given:
        mockDomain Author
        def binder = new GormAwareDataBinder(grailsApplication)
        def obj = new Author()
        
        when:
        doBind obj, [nullableString: '  ']
        
        then:
        obj.nullableString == ''
    }
    
    void 'Test binding blank String to nullable property with trimming turned off'() {
        given:
        def binder = new GormAwareDataBinder(grailsApplication)
        binder.trimStrings = false
        mockDomain Author
        def obj = new Author()
        
        when:
        binder.bind obj, [nullableString: '  ']
        
        then:
        obj.nullableString == '  '
    }
    
    void 'Test binding to primitives from Strings'() {
        given:
        def binder = new GormAwareDataBinder()
        def obj = new PrimitiveContainer()

        when:
        doBind(obj, [someBoolean: 'true',
            someByte: '1',
            someChar: 'a',
            someShort: '2',
            someInt: '3',
            someLong: '4',
            someFloat: '5.5',
            someDouble: '6.6'])

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

    void 'Test id binding'() {
        given:
        mockDomains Author, Publication
        def author = new Author(name: 'David Foster Wallace').save(flush: true)
        def publication = new Publication()

        when:
        doBind publication, [title: 'Infinite Jest', author: [id: author.id]]

        then:
        publication.title == 'Infinite Jest'
        publication.author.name == 'David Foster Wallace'

        when:
        doBind publication, [author: [id: 'null']]

        then:
        publication.author == null

        when:
        publication.title = null
        doBind publication, [title: 'Infinite Jest', author: [id: author.id]], [], ['author']

        then:
        publication.title == 'Infinite Jest'
        publication.author == null

        when:
        publication.author = null
        doBind publication, [title: 'Infinite Jest 2', author: [id: author.id]]

        then:
        publication.author.name == 'David Foster Wallace'
    }

    void 'Test binding to the one side of a one to many'() {
        given:
        mockDomains Author, Publication, Publisher
        def author = new Author(name: 'Graeme').save()
        def pub = new Publication(title: 'DGG', author: author)

        when:
        doBind pub, [publisher: [name: 'Apress']]

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
        mockDomain Publisher
        def publisher = new Publisher()

        when:
        doBind publisher, [name: 'Apress',
            'publications[0]': [title: 'DGG', author: [name: 'Graeme']],
            'publications[1]': [title: 'DGG2', author: [name: 'Jeff']]]

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
        def widget = new Widget()

        when:
        doBind widget, [isBindable: 'Should Be Bound', isNotBindable: 'Should Not Be Bound']

        then:
        widget.isBindable == 'Should Be Bound'
        widget.isNotBindable == null
    }

    void 'Test binding to a collection of String'() {
        given:
        mockDomain DataBindingBook
        def book = new DataBindingBook()
        
        when:
        doBind book, [topics: ['journalism', null, 'satire']]
        doBind book, ['topics[1]': 'counterculture']
        
        then:
        book.topics == ['journalism', 'counterculture', 'satire']
    }
    
    void 'Test binding to a collection of Integer'() {
        given:
        mockDomain DataBindingBook
        def book = new DataBindingBook()
        
        when:
        doBind book, [importantPageNumbers: ['5', null, '42']]
        doBind book, ['importantPageNumbers[1]': '2112']
        
        then:
        book.importantPageNumbers == [5, 2112, 42]
    }
    
    void 'Test binding to a collection of primitive'() {
        given:
        mockDomains Parent, Child
        def parent = new Parent()

        when:
        doBind parent, [child: [someOtherIds: '4']]

        then:
        parent.child.someOtherIds.size() == 1
        parent.child.someOtherIds.contains(4)

        when:
        parent.child = null
        doBind(parent,  [child: [someOtherIds: ['4', '5', '6']]])

        then:
        parent.child.someOtherIds.size() == 3
        parent.child.someOtherIds.contains(4)
        parent.child.someOtherIds.contains(5)
        parent.child.someOtherIds.contains(6)

        when:
        parent.child = null
        doBind(parent,  [child: [someOtherIds: 4]])

        then:
        parent.child.someOtherIds.size() == 1
        parent.child.someOtherIds.contains(4)
    }
    
    void 'Test unbinding a Map entry'() {
        given:
        mockDomains Team, Author
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
        doBind team, ['members[jeff]': [id: 'null']]
        
        then:
        team.members.size() == 1
        team.members.containsKey('betsy')
        'Sarah Elizabeth Brown' == team.members.betsy.name
    }
    
    void 'Test binding to a Map for new instance with quoted key'() {
        given:
        mockDomains Team, Author
        def team = new Team()
        
        when:
        doBind team, ["members['jeff']": [name: 'Jeff Scott Brown'], 'members["betsy"]': [name: 'Sarah Elizabeth Brown']]
        
        then:
        team.members.size() == 2
        assert team.members.jeff instanceof Author
        assert team.members.betsy instanceof Author
        team.members.jeff.name == 'Jeff Scott Brown'
        team.members.betsy.name == 'Sarah Elizabeth Brown'

    }
    
    void 'Test autoGrowCollectionLimit with Maps of String'() {
        given:
        def binder = new GormAwareDataBinder(grailsApplication)
        mockDomains Team, Author
        def team = new Team()
        binder.autoGrowCollectionLimit = 2
        def bindingSource = [:]
        bindingSource['states[MO]'] = 'Missouri'
        bindingSource['states[IL]'] = 'Illinois'
        bindingSource['states[VA]'] = 'Virginia'
        bindingSource['states[CA]'] = 'California'
        
        when:
        binder.bind team, bindingSource
        
        then:
        team.states.size() == 2
        team.states.containsKey('MO')
        team.states.containsKey('IL')
        team.states.MO == 'Missouri'
        team.states.IL == 'Illinois'

    }
    
    void 'Test autoGrowCollectionLimit with Maps of domain objects'() {
        given:
        def binder = new GormAwareDataBinder(grailsApplication)
        mockDomains Team, Author
        def team = new Team()
        binder.autoGrowCollectionLimit = 2
        def bindingSource = [:]
        bindingSource['members[jeff]'] = [name: 'Jeff Scott Brown']
        bindingSource['members[betsy]'] = [name: 'Sarah Elizabeth Brown']
        bindingSource['members[jake]'] = [name: 'Jacob Ray Brown']
        bindingSource['members[zack]'] = [name: 'Zachary Scott Brown']
        
        when:
        binder.bind team, bindingSource
        
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
        mockDomains Publisher, Author
        def pub = new Publisher()
        pub.addToAuthors(name: 'Author One')
        
        when:
        doBind pub, ['authors[0]': [name: 'Author Uno'], 'authors[1]': [name: 'Author Dos']]
        
        then:
        pub.authors.size() == 2
        pub.authors[0].name == 'Author Uno'
        pub.authors[1].name == 'Author Dos'
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
    String nullableString
    static constraints = {
        nullableString nullable: true
    }
}

@Entity
class Widget {
    String isBindable
    String isNotBindable

    static constraints = {
        isNotBindable bindable: false
    }
}

@Entity
class Parent {
    Child child
}

@Entity
class Child {
    static hasMany = [someOtherIds: Integer]
}

@Entity
class DataBindingBook {
    String title
    List importantPageNumbers
    List topics
    static hasMany = [topics: String, importantPageNumbers: Integer]
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
