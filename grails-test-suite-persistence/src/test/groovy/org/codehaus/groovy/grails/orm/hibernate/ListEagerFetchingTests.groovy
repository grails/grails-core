package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ListEagerFetchingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
package listeagerfetchingtests

import grails.persistence.*

@Entity
class Store {
    String name

    List categories
    static hasMany = [categories: Category]

    static mapping = {
        categories fetch:"join"
    }
}

@Entity
class Category {
    String name
    Store store
    Category parent

    static belongsTo = Store
    static hasMany = [subCategories: Category]
    static mappedBy = [parent: "Category"]
    static constraints = {
         parent(nullable:true)
    }
}
'''
    }

    void testListEagerFetchResults() {
        def Store = ga.getDomainClass("listeagerfetchingtests.Store").clazz
        def Category = ga.getDomainClass("listeagerfetchingtests.Category").clazz

        Store.newInstance(name:"one")
             .addToCategories(name:"A")
             .addToCategories(name:"B")
             .addToCategories(name:"C").save()
        Store.newInstance(name:"one")
             .addToCategories(name:"D")
             .addToCategories(name:"E")
             .addToCategories(name:"F").save()

        Store.newInstance(name:"three").save(flush:true)

        assertEquals 3, Store.list().size()
        assertEquals 3, Store.listOrderByName().size()
        assertEquals 2, Store.findAllByName("one").size()
        assertEquals 2, Store.findAllWhere(name:"one").size()
    }
}
