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

import org.codehaus.groovy.grails.web.binding.GormAwareDataBinder

@spock.lang.Ignore
class GormAwareDataBinderSpec extends GormSpec {

    void 'Test id binding'() {
        given:
        def binder = new GormAwareDataBinder(grailsApplication)
        def author = new Author(name: 'David Foster Wallace').save(flush: true)
        def publication = new Publication()

        when:
        binder.bind publication, [title: 'Infinite Jest', author: [id: author.id]]

        then:
        publication.title == 'Infinite Jest'
        publication.author.name == 'David Foster Wallace'
        
        when:
        binder.bind publication, [author: [id: 'null']]
        
        then:
        publication.author == null

        when:
        publication.title = null
        binder.bind publication, [title: 'Infinite Jest', author: [id: author.id]], [], ['author']

        then:
        publication.title == 'Infinite Jest'
        publication.author == null
        
        when:
        publication.author = null
        binder.bind publication, [title: 'Infinite Jest 2', author: [id: author.id]]
        
        then:
        publication.author.name == 'David Foster Wallace'
        
    }

    void 'Test binding to the one side of a one to many'() {
        given:
        def binder = new GormAwareDataBinder(grailsApplication)
        def author = new Author(name: 'Graeme').save()
        def pub = new Publication(title: 'DGG', author: author)
        
        when:
        binder.bind pub, [publisher: [name: 'Apress']]
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
        def binder = new GormAwareDataBinder(grailsApplication)
        def publisher = new Publisher()

        when:
        binder.bind publisher, [name: 'Apress',
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
        def binder = new GormAwareDataBinder(grailsApplication)
        def widget = new Widget()
        
        when:
        binder.bind widget, [isBindable: 'Should Be Bound', isNotBindable: 'Should Not Be Bound']
        
        then:
        widget.isBindable == 'Should Be Bound'
        widget.isNotBindable == null
    }
    
    @Override
    List getDomainClasses() {
        [Publication, Author, Publisher, Widget]
    }
}

@Entity
class Publisher {
    String name
    static hasMany = [publications: Publication]
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
}

@Entity
class Widget {
    String isBindable
    String isNotBindable
    
    static constraints = {
        isNotBindable bindable: false
    }
}
