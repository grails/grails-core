package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FetchMode

class NamedCriteriaRelationshipTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [NamedCriteriaPlantCategory,
         NamedCriteriaPlant,
         Book,
         OneBookAuthor,
         OneAuthorPublisher]
    }

    void testFetch() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        session.clear()

        // the code below relies on some implementation details which is a little brittle but should work...

        def query = NamedCriteriaPlantCategory.withPlantsInPatch
        query.list(fetch: [plants: 'lazy'])
        def crit = query.queryBuilder.instance
        def plantFetchMode = crit.getFetchMode('plants')
        assertEquals 'wrong fetch mode for plants', FetchMode.SELECT, plantFetchMode

        query.list(fetch: [plants: 'eager'])
        crit = query.queryBuilder.instance
        plantFetchMode = crit.getFetchMode('plants')
        assertEquals 'wrong fetch mode fro plants', FetchMode.JOIN, plantFetchMode
    }

    void testNamedQueryWithRelationshipInCriteria() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"groovy")
                                 .addToPlants(goesInPatch: true, name: 'Gplant')
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"Gray")
                                 .save(flush:true)

        session.clear()

        def results = NamedCriteriaPlantCategory.withPlantsInPatch.list()
        assertEquals 2, results.size()
        assertTrue 'leafy' in results*.name
        assertTrue 'groovy' in results*.name

        results = NamedCriteriaPlantCategory.withPlantsThatStartWithG.list()
        assertEquals 2, results.size()
        assertTrue 'groovy' in results*.name
        assertTrue 'grapes' in results*.name

        results = NamedCriteriaPlantCategory.withPlantsInPatchThatStartWithG.list()
        assertEquals 1, results.size()
        assertEquals 'groovy', results[0].name
    }

    void testInvokingNamedQueryDefinedInAnotherDomainClass() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"groovy")
                                 .addToPlants(goesInPatch: true, name: 'Gplant')
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"Gray")
                                 .save(flush:true)

        session.clear()

        def results = NamedCriteriaPlantCategory.withPlantsThatStartWithG.list()
        assertEquals 2, results.size()
        def names = results*.name
        assertTrue 'groovy' in names
        assertTrue 'grapes' in names
    }

    void testListDistinct() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:false, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = NamedCriteriaPlantCategory.withPlantsInPatch().listDistinct()

        assertEquals 2, categories.size()
        def names = categories*.name
        assertEquals 2, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
    }

    void testListDistinct2() {
        assert new NamedCriteriaPlantCategory(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert new NamedCriteriaPlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:true, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = NamedCriteriaPlantCategory.withPlantsInPatch.listDistinct()

        assertEquals 3, categories.size()
        def names = categories*.name
        assertEquals 3, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
        assertTrue 'grapes' in names
    }

    void testNamedQueryWithAssociationClosure() {
        def book = new Book(title: 'First Popular Book')
        book.popularity = new Popularity(liked: 42)
        assert book.save(flush: true)

        book = new Book(title: 'Second Popular Book')
        book.popularity = new Popularity(liked: 2112)
        assert book.save(flush: true)

        book = new Book(title: 'First Unpopular Book')
        book.popularity = new Popularity(liked: 0)
        assert book.save(flush: true)

        book = new Book(title: 'Second Unpopular Book')
        book.popularity = new Popularity(liked: 0)
        assert book.save(flush: true)

        session.clear()

        //def result = Book.popularBooks.list()

        def result = Book.withCriteria {
            popularity {
                gt 'liked', 0
            }
        }
        assertEquals 2, result?.size()

        def titles = result.title
        assertTrue 'First Popular Book' in titles
        assertTrue 'Second Popular Book' in titles
    }

    void testNamedQueryInvolvingNestedRelationshipsSomeOfWhichAreEmbedded() {
        def book = new Book(title: 'First Popular Book')
        book.popularity = new Popularity(liked: 42)
        assert book.save(flush: true)

        def author = new OneBookAuthor()
        author.book = book
        assert author.save(flush: true)

        def publisher = new OneAuthorPublisher(name: 'First Good Publisher')
        publisher.author = author
        assert publisher.save(flush: true)

        book = new Book(title: 'Second Popular Book')
        book.popularity = new Popularity(liked: 2112)
        assert book.save(flush: true)

        author = new OneBookAuthor()
        author.book = book
        assert author.save(flush: true)

        publisher = new OneAuthorPublisher(name: 'Second Good Publisher')
        publisher.author = author
        assert publisher.save(flush: true)

        book = new Book(title: 'First Unppular Book')
        book.popularity = new Popularity(liked: 0)
        assert book.save(flush: true)

        author = new OneBookAuthor()
        author.book = book
        assert author.save(flush: true)

        publisher = new OneAuthorPublisher(name: 'First Bad Publisher')
        publisher.author = author
        assert publisher.save(flush: true)

        def result = OneAuthorPublisher.withPopularBooks.list()
        assertEquals 2, result?.size()

        def names = result.name
        assert 'First Good Publisher' in names
        assert 'Second Good Publisher' in names
    }
}
