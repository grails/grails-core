package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ListEagerFetchingTests extends AbstractGrailsHibernateTests {


    void testListEagerFetchResults() {

        new Store(name:"one")
             .addToCategories(name:"A")
             .addToCategories(name:"B")
             .addToCategories(name:"C").save()
        new Store(name:"one")
             .addToCategories(name:"D")
             .addToCategories(name:"E")
             .addToCategories(name:"F").save()

        new Store(name:"three").save(flush:true)

        assertEquals 3, Store.list().size()
        assertEquals 3, Store.listOrderByName().size()
        assertEquals 2, Store.findAllByName("one").size()
        assertEquals 2, Store.findAllWhere(name:"one").size()
    }

    @Override
    protected getDomainClasses() {
        [Store, Category]
    }


}

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