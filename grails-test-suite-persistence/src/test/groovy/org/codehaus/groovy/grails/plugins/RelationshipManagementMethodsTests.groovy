package org.codehaus.groovy.grails.plugins;

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

class RelationshipManagementMethodsTests extends AbstractGrailsHibernateTests {

    void testDependencyInjectionHappensOnInstancesCreatedByAddToMethods() {
        def storeClass = ga.getDomainClass('Store')
        def store = storeClass.newInstance()
        store.addToWidgets(widgetName: 'Some Widget')
        def widgets = store.widgets as List
        assertNotNull 'the someService property in the Widget was null, but should not have been', widgets[0].someService
    }

    void testHandlingGstrings() {
        // GRAILS-5499

        def uc = ga.getDomainClass('DemoUser')
        def u = uc.newInstance()

        // add some GStrings...
        5.times {
            u.addToNicknames "nickname${it}"
        }

        // add some regular Strings...
        u.addToNicknames 'somenickname'
        u.addToNicknames 'someothernickname'

        assertEquals 7, u.nicknames.size()
        assertTrue 'nickname0' in u.nicknames
        assertTrue 'nickname1' in u.nicknames
        assertTrue 'nickname2' in u.nicknames
        assertTrue 'nickname3' in u.nicknames
        assertTrue 'nickname4' in u.nicknames
        assertTrue 'somenickname' in u.nicknames
        assertTrue 'someothernickname' in u.nicknames

        // remove using GStrings...
        3.times {
            u.removeFromNicknames "nickname${it}"
        }

        // remove using regular String...
        u.removeFromNicknames 'someothernickname'

        assertEquals 3, u.nicknames.size()
        assertFalse 'nickname0' in u.nicknames
        assertFalse 'nickname1' in u.nicknames
        assertFalse 'nickname2' in u.nicknames
        assertTrue 'nickname3' in u.nicknames
        assertTrue 'nickname4' in u.nicknames
        assertTrue 'somenickname' in u.nicknames
        assertFalse 'someothernickname' in u.nicknames
    }

    void testWithMapAddition() {
        def personClass = ga.getDomainClass("Person")
        def addressClass = ga.getDomainClass("Address")

        def p = personClass.newInstance()

        p.name = "Homer Simpson"

        p.addToAddresses(number:"22")
        def address = p.addresses.iterator().next()
        assert address
        assertEquals "22", address.number
        assertEquals p, address.person

        p.removeFromAddresses(address)

        assertFalse p.addresses.contains(address)
        assert !address.person
    }

    void testWithInstanceAddition() {
        def personClass = ga.getDomainClass("Person")
        def addressClass = ga.getDomainClass("Address")

        def p = personClass.newInstance()

        p.name = "Homer Simpson"

        def address = addressClass.newInstance()
        address.number = "22"

        p.addToAddresses(address)

        assertTrue p.addresses.contains(address)

        assert address
        assertEquals "22", address.number
        assertEquals p, address.person

        p.removeFromAddresses(address)

        assertFalse p.addresses.contains(address)
        assert !address.person
    }

    void testWithManyToManyRelationship() {
        def bookmarkClass = ga.getDomainClass("Bookmark")
        def tagClass = ga.getDomainClass("Tag")

        def bookmark = bookmarkClass.newInstance()
        bookmark.url='http://www.ru'

        def tag = tagClass.newInstance()
        tag.name = "foo"

        bookmark.addToTags(tag)

        assert bookmark.tags.contains(tag)
        assert tag.bookmarks.contains(bookmark)

        bookmark.removeFromTags(tag)
        assert !bookmark.tags.contains(tag)
        assert !tag.bookmarks.contains(bookmark)
    }

    void testSubclassWithManyToManyRelationship() {
        def bookmarkClass = ga.getDomainClass("BookmarkSubclass")
        def tagClass = ga.getDomainClass("Tag")

        def bookmark = bookmarkClass.newInstance()
        bookmark.url='http://www.ru'

        def tag = tagClass.newInstance()
        tag.name = "foo"

        bookmark.addToTags(tag)

        assert bookmark.tags.contains(tag)
        assert tag.bookmarks.contains(bookmark)

        bookmark.removeFromTags(tag)
        assert !bookmark.tags.contains(tag)
        assert !tag.bookmarks.contains(bookmark)
    }

    void onSetUp() {

        gcl.parseClass('''
import grails.persistence.*

@grails.persistence.Entity
class DemoUser {
  String name
  static hasMany = [nicknames: String]
}

@Entity
class Person {
    String name
    static hasMany = [addresses:Address]
}

@Entity
class Address {
    String number
    Person person
}

@Entity
class Bookmark {

    String url
    static hasMany = [tags:Tag]
    static belongsTo = [Tag]
}

@Entity
class BookmarkSubclass extends Bookmark {
}

@Entity
class Tag {
    String name
    static hasMany = [bookmarks:Bookmark]
}
@grails.persistence.Entity
class Store {
   static hasMany = [widgets: Widget]
}

@grails.persistence.Entity
class Widget {
    def someService
    String widgetName
}

class SomeService {}
''')
    }
}
