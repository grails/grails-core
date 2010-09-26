package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FetchMode;
import org.hibernate.Hibernate

/**
 * @author Jeff Brown
 */
class NamedCriteriaTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class PlantCategory {
    Long id
    Long version
    Set plants
    String name

    static hasMany = [plants:Plant]

    static namedQueries = {
        withPlantsInPatch {
            plants {
                eq 'goesInPatch', true
            }
        }
        withPlantsThatStartWithG {
            plants {
                like 'name', 'G%'
            }
        }
        withPlantsInPatchThatStartWithG {
            withPlantsInPatch()
            withPlantsThatStartWithG()
        }
    }
}

class Plant {
    Long id
    Long version
    boolean goesInPatch
    String name
}

class PublicationSubclassWithoutNamedQueries extends Publication {
}

class PublicationSubclassWithNamedQueries extends Publication {
    static namedQueries = {
        oldPaperbacks {
            paperbacks()
            lt 'datePublished', new Date() - 365
        }
    }
}

class Publication {
   Long id
   Long version
   String title
   Date datePublished
   Boolean paperback = true

   static namedQueries = {
       recentPublications {
           def now = new Date()
           gt 'datePublished', now - 365
       }

       publicationsWithBookInTitle {
           like 'title', '%Book%'
       }

       recentPublicationsByTitle { title ->
           recentPublications()
           eq 'title', title
       }

       latestBooks {
           maxResults(10)
           order("datePublished", "desc")
       }

       publishedBetween { start, end ->
           between 'datePublished', start, end
       }

        publishedAfter { date ->
           gt 'datePublished', date
        }

       paperbackOrRecent {
           or {
                def now = new Date()
                gt 'datePublished', now - 365
                paperbacks()
           }
       }

       paperbacks {
          eq 'paperback', true
       }

       paperbackAndRecent {
           paperbacks()
           recentPublications()
       }

       thisWeeksPaperbacks() {
           paperbacks()
           def today = new Date()
           publishedBetween(today - 7, today)
       }

       queryThatNestsMultipleLevels {
           // this nested query will call other nested queries
           thisWeeksPaperbacks()
       }
   }
}
''')
    }

	void testSorting() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "ZZZ New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: "AAA New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: "CCC New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: "BBB New Paperback",
                datePublished: now - 10, paperback: true).save()

        assert publicationClass.newInstance(title: "ZZZ Old Paperback",
                datePublished: now - 900, paperback: true).save()
        assert publicationClass.newInstance(title: "AAA Old Paperback",
                datePublished: now - 900, paperback: true).save()

		session.clear()

		// verify the sort works...
		def results = publicationClass.paperbackAndRecent.list(sort: 'title')

		assertEquals 'wrong number of results', 4, results?.size()
		assertEquals 'wrong title', 'AAA New Paperback', results[0].title
		assertEquals 'wrong title', 'BBB New Paperback', results[1].title
		assertEquals 'wrong title', 'CCC New Paperback', results[2].title
		assertEquals 'wrong title', 'ZZZ New Paperback', results[3].title

		// verify the sort works along with additional criteria...
		results = publicationClass.paperbackAndRecent(sort: 'title') {
			ne 'title', 'CCC New Paperback'
		}

		assertEquals 'wrong number of results', 3, results?.size()
		assertEquals 'wrong title', 'AAA New Paperback', results[0].title
		assertEquals 'wrong title', 'BBB New Paperback', results[1].title
		assertEquals 'wrong title', 'ZZZ New Paperback', results[2].title

		// verify the order works...
		results = publicationClass.paperbackAndRecent.list(sort: 'title', order: 'desc')

		assertEquals 'wrong number of results', 4, results?.size()
		assertEquals 'wrong title', 'ZZZ New Paperback', results[0].title
		assertEquals 'wrong title', 'CCC New Paperback', results[1].title
		assertEquals 'wrong title', 'BBB New Paperback', results[2].title
		assertEquals 'wrong title', 'AAA New Paperback', results[3].title

        assert publicationClass.newInstance(title: "zzz New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: "aaa New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: "ccc New Paperback",
                datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: "bbb New Paperback",
                datePublished: now - 10, paperback: true).save()

		// verify the ignoreCase works
		results = publicationClass.paperbackAndRecent.list(sort: 'title', ignoreCase: false)

        assertEquals 'wrong number of results', 8, results?.size()
        assertEquals 'wrong title', 'AAA New Paperback', results[0].title
        assertEquals 'wrong title', 'BBB New Paperback', results[1].title
        assertEquals 'wrong title', 'CCC New Paperback', results[2].title
        assertEquals 'wrong title', 'ZZZ New Paperback', results[3].title
        assertEquals 'wrong title', 'aaa New Paperback', results[4].title
        assertEquals 'wrong title', 'bbb New Paperback', results[5].title
        assertEquals 'wrong title', 'ccc New Paperback', results[6].title
        assertEquals 'wrong title', 'zzz New Paperback', results[7].title
        results = publicationClass.paperbackAndRecent.list(sort: 'title', ignoreCase: true)

        assertEquals 'wrong number of results', 8, results?.size()

		// AAA and aaa should be the first 2, BBB and bbb should be the second 2 etc...
		// but we don't know if AAA or aaa comes first
		assertTrue 'AAA New Paperback was not where it should have been in the results', results.title[0..1].contains('AAA New Paperback')
		assertTrue 'aaa New Paperback was not where it should have been in the results', results.title[0..1].contains('aaa New Paperback')
		assertTrue 'BBB New Paperback was not where it should have been in the results', results.title[2..3].contains('BBB New Paperback')
		assertTrue 'bbb New Paperback was not where it should have been in the results', results.title[2..3].contains('bbb New Paperback')
		assertTrue 'CCC New Paperback was not where it should have been in the results', results.title[4..5].contains('CCC New Paperback')
		assertTrue 'ccc New Paperback was not where it should have been in the results', results.title[4..5].contains('ccc New Paperback')
		assertTrue 'ZZZ New Paperback was not where it should have been in the results', results.title[6..7].contains('ZZZ New Paperback')
		assertTrue 'zzz New Paperback was not where it should have been in the results', results.title[6..7].contains('zzz New Paperback')
	}

	void testInheritedNamedQueries() {
        def publicationClass = ga.getDomainClass("PublicationSubclassWithoutNamedQueries").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save()

        session.clear()

        def publications = publicationClass.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title

		publicationClass = ga.getDomainClass("PublicationSubclassWithNamedQueries").clazz

        now = new Date()
        assert publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save()

        session.clear()

        publications = publicationClass.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title

		publications = publicationClass.oldPaperbacks.list()
		assertEquals 1, publications?.size()
		assertEquals 'Some Old Book', publications[0].title
	}

	void testFetch() {
        def plantCategoryClass = ga.getDomainClass("PlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        session.clear()

		// the code below relies on some implementation details which is a little brittle but should work...

		def query = plantCategoryClass.withPlantsInPatch
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
        def plantCategoryClass = ga.getDomainClass("PlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"Lettuce")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"groovy")
                                 .addToPlants(goesInPatch: true, name: 'Gplant')
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"Gray")
                                 .save(flush:true)

        session.clear()

        def results = plantCategoryClass.withPlantsInPatch.list()
        assertEquals 2, results.size()
        assertTrue 'leafy' in results*.name
        assertTrue 'groovy' in results*.name

        results = plantCategoryClass.withPlantsThatStartWithG.list()
        assertEquals 2, results.size()
        assertTrue 'groovy' in results*.name
        assertTrue 'grapes' in results*.name

        results = plantCategoryClass.withPlantsInPatchThatStartWithG.list()
        assertEquals 1, results.size()
        assertEquals 'groovy', results[0].name
    }

    void testListDistinct() {
        def plantCategoryClass = ga.getDomainClass("PlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:false, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = plantCategoryClass.withPlantsInPatch().listDistinct()

        assertEquals 2, categories.size()
        def names = categories*.name
        assertEquals 2, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
    }

    void testListDistinct2() {
        def plantCategoryClass = ga.getDomainClass("PlantCategory").clazz

        assert plantCategoryClass.newInstance(name:"leafy")
                                 .addToPlants(goesInPatch:true, name:"lettuce")
                                 .addToPlants(goesInPatch:true, name:"cabbage")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

        assert plantCategoryClass.newInstance(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:true, name:"white")
                                 .save(flush:true)

        session.clear()

        def categories = plantCategoryClass.withPlantsInPatch.listDistinct()

        assertEquals 3, categories.size()
        def names = categories*.name
        assertEquals 3, names.size()
        assertTrue 'leafy' in names
        assertTrue 'orange' in names
        assertTrue 'grapes' in names
    }

    void testFindAllWhereAttachedToChainedNamedQueries() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        def now = new Date()

        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10, paperback: false).save()
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 1000, paperback: true).save()
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10, paperback: true).save()

        assert publicationClass.newInstance(title: "Some Title",
                datePublished: now - 10, paperback: false).save()
        assert publicationClass.newInstance(title: "Some Title",
                datePublished: now - 1000, paperback: false).save()
        assert publicationClass.newInstance(title: "Some Title",
                datePublished: now - 10, paperback: true).save()
        session.clear()

        def results = publicationClass.recentPublications().publicationsWithBookInTitle().findAllWhere(paperback: true)

        assertEquals 1, results?.size()
    }

    void testNamedQueryPassingMultipleParamsToNestedNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        def now = new Date()

        assert publicationClass.newInstance(title: "Some Book",
        datePublished: now - 10, paperback: false).save()
        assert publicationClass.newInstance(title: "Some Book",
                                            datePublished: now - 1000, paperback: true).save()
        assert publicationClass.newInstance(title: "Some Book",
                                            datePublished: now - 2, paperback: true).save()

        assert publicationClass.newInstance(title: "Some Title",
                                            datePublished: now - 2, paperback: false).save()
        assert publicationClass.newInstance(title: "Some Title",
                                            datePublished: now - 1000, paperback: false).save()
        assert publicationClass.newInstance(title: "Some Title",
                                            datePublished: now - 2, paperback: true).save()
        session.clear()

        def results = publicationClass.thisWeeksPaperbacks().list()

        assertEquals 2, results?.size()

		results = publicationClass.queryThatNestsMultipleLevels().list()

        assertEquals 2, results?.size()
    }

    void testGetAttachedToChainedNamedQueries() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        def now = new Date()

        def oldPaperBackWithBookInTitleId =  publicationClass.newInstance(title: "Some Book",
                datePublished: now - 1000, paperback: true).save().id
        def newPaperBackWithBookInTitleId =  publicationClass.newInstance(title: "Some Book",
                datePublished: now, paperback: true).save().id

        assertNull publicationClass.publicationsWithBookInTitle().publishedAfter(now - 5).get(oldPaperBackWithBookInTitleId)
        assertNull publicationClass.publishedAfter(now - 5).publicationsWithBookInTitle().get(oldPaperBackWithBookInTitleId)
        assertNotNull publicationClass.publicationsWithBookInTitle().publishedAfter(now - 5).get(newPaperBackWithBookInTitleId)
        assertNotNull publicationClass.publishedAfter(now - 5).publicationsWithBookInTitle().get(newPaperBackWithBookInTitleId)
    }

    void testPassingParamsAndAdditionalCriteria() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        def now = new Date()

        6.times { cnt ->
            assert publicationClass.newInstance(title: "Some Old Book #${cnt}",
                      datePublished: now - 1000, paperback: true).save(failOnError: true).id
            assert publicationClass.newInstance(title: "Some New Book #${cnt}",
                      datePublished: now, paperback: true).save(failOnError: true).id
        }

        def results = publicationClass.publishedAfter(now - 5) {
            eq 'paperback', true
        }
        assertEquals 6, results?.size()

        results = publicationClass.publishedAfter(now - 5, [max: 2, offset: 1]) {
            eq 'paperback', true
        }
        assertEquals 2, results?.size()

        results = publicationClass.publishedBetween(now - 5, now + 1) {
            eq 'paperback', true
        }
        assertEquals 6, results?.size()

        results = publicationClass.publishedBetween(now - 5, now + 1, [max: 2, offset: 1]) {
            eq 'paperback', true
        }
        assertEquals 2, results?.size()

        results = publicationClass.publishedAfter(now - 1005) {
            eq 'paperback', true
        }
        assertEquals 12, results?.size()

        results = publicationClass.publishedAfter(now - 5) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = publicationClass.publishedAfter(now - 5, [max: 2, offset: 1]) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = publicationClass.publishedBetween(now - 5, now + 1) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = publicationClass.publishedBetween(now - 5, now + 1, [max: 2, offset: 1]) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()

        results = publicationClass.publishedAfter(now - 1005) {
            eq 'paperback', false
        }
        assertEquals 0, results?.size()
    }

    void testChainingNamedQueries() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        [true, false].each { isPaperback ->
            4.times {
                assert publicationClass.newInstance(title: "Some Book",
                        datePublished: now - 10, paperback: isPaperback).save()
                assert publicationClass.newInstance(title: "Some Other Book",
                        datePublished: now - 10, paperback: isPaperback).save()
                assert publicationClass.newInstance(title: "Some Other Title",
                        datePublished: now - 10, paperback: isPaperback).save()
                assert publicationClass.newInstance(title: "Some Book",
                        datePublished: now - 1000, paperback: isPaperback).save()
                assert publicationClass.newInstance(title: "Some Other Book",
                        datePublished: now - 1000, paperback: isPaperback).save()
                assert publicationClass.newInstance(title: "Some Other Title",
                        datePublished: now - 1000, paperback: isPaperback).save()
            }
        }
        session.clear()

        def results = publicationClass.recentPublications().publicationsWithBookInTitle().list()
        assertEquals 'wrong number of books were returned from chained queries', 16, results?.size()
        results = publicationClass.recentPublications().publicationsWithBookInTitle().count()
        assertEquals 16, results

        results = publicationClass.recentPublications.publicationsWithBookInTitle.list()
        assertEquals 'wrong number of books were returned from chained queries', 16, results?.size()
        results = publicationClass.recentPublications.publicationsWithBookInTitle.count()
        assertEquals 16, results

        results = publicationClass.paperbacks().recentPublications().publicationsWithBookInTitle().list()
        assertEquals 'wrong number of books were returned from chained queries', 8, results?.size()
        results = publicationClass.paperbacks().recentPublications().publicationsWithBookInTitle().count()
        assertEquals 8, results

        results = publicationClass.recentPublications().publicationsWithBookInTitle().findAllByPaperback(true)
        assertEquals 'wrong number of books were returned from chained queries', 8, results?.size()

        results = publicationClass.paperbacks.recentPublications.publicationsWithBookInTitle.list()
        assertEquals 'wrong number of books were returned from chained queries', 8, results?.size()
        results = publicationClass.paperbacks.recentPublications.publicationsWithBookInTitle.count()
        assertEquals 8, results
    }

    void testChainingQueriesWithParams() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def lastWeek = now - 7
        def longAgo = now - 1000
        2.times {
            assert publicationClass.newInstance(title: 'Some Book',
                    datePublished: now).save()
            assert publicationClass.newInstance(title: 'Some Title',
                    datePublished: now).save()
        }
        3.times {
            assert publicationClass.newInstance(title: 'Some Book',
                    datePublished: lastWeek).save()
            assert publicationClass.newInstance(title: 'Some Title',
                    datePublished: lastWeek).save()
        }
        4.times {
            assert publicationClass.newInstance(title: 'Some Book',
                    datePublished: longAgo).save()
            assert publicationClass.newInstance(title: 'Some Title',
                    datePublished: longAgo).save()
        }
        session.clear()

        def results = publicationClass.recentPublicationsByTitle('Some Book').publishedAfter(now - 2).list()
        assertEquals 'wrong number of books were returned from chained queries', 2, results?.size()

        results = publicationClass.recentPublicationsByTitle('Some Book').publishedAfter(now - 2).count()
        assertEquals 2, results

        results = publicationClass.recentPublicationsByTitle('Some Book').publishedAfter(lastWeek - 2).list()
        assertEquals 'wrong number of books were returned from chained queries', 5, results?.size()

        results = publicationClass.recentPublicationsByTitle('Some Book').publishedAfter(lastWeek - 2).count()
        assertEquals 5, results
    }

    void testReferencingNamedQueryBeforeAnyDynamicMethodsAreInvoked() {
        // GRAILS-5809
        def publicationClass = ga.getDomainClass("Publication").clazz

        /*
         * currently this will work:
         *   publicationClass.recentPublications().list()
         * but this will not:
         *   publicationClass.recentPublications.list()
         *
         * the static property isn't being added to the class until
         * the first dynamic method (recentPublications(), save(), list() etc...) is
         * invoked
         */
        def publications = publicationClass.recentPublications.list()
        assertEquals 0, publications.size()
    }

    void testAdditionalCriteriaClosure() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        6.times {
            assert publicationClass.newInstance(title: "Some Book",
            datePublished: now - 10).save()
            assert publicationClass.newInstance(title: "Some Other Book",
            datePublished: now - 10).save()
            assert publicationClass.newInstance(title: "Some Book",
            datePublished: now - 900).save()
        }
        session.clear()

        def publications = publicationClass.recentPublications {
            eq 'title', 'Some Book'
        }
        assertEquals 6, publications?.size()

        publications = publicationClass.recentPublications {
            like 'title', 'Some%'
        }
        assertEquals 12, publications?.size()

        publications = publicationClass.recentPublications(max: 3) {
            like 'title', 'Some%'
        }
        assertEquals 3, publications?.size()

        def cnt = publicationClass.recentPublications.count {
            eq 'title', 'Some Book'
        }
        assertEquals 6, cnt
    }

    void testDisjunction() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def oldDate = now - 2000

        assert publicationClass.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert publicationClass.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert publicationClass.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert publicationClass.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.clear()

        def publications = publicationClass.paperbackOrRecent.list()
        assertEquals 3, publications?.size()

        def titles = publications.title
    }

    void testConjunction() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def oldDate = now - 2000

        assert publicationClass.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert publicationClass.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert publicationClass.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert publicationClass.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.clear()

        def publications = publicationClass.paperbackAndRecent.list()
        assertEquals 1, publications?.size()
    }

    void testList() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save()

        session.clear()

        def publications = publicationClass.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title
    }

    void testFindAllBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        3.times {
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 10).save()
            assert publicationClass.newInstance(title: "Some Other Book",
                    datePublished: now - 10).save()
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 900).save()
        }
        session.clear()

        def publications = publicationClass.recentPublications.findAllByTitle('Some Book')

        assertEquals 3, publications?.size()
        assertEquals 'Some Book', publications[0].title
        assertEquals 'Some Book', publications[1].title
        assertEquals 'Some Book', publications[2].title
    }

    void testFindAllByBoolean() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()

        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()

        def publications = publicationClass.recentPublications.findAllPaperbackByTitle('Some Book')

        assertEquals 2, publications?.size()
        assertEquals publications[0].title, 'Some Book'
        assertEquals publications[1].title, 'Some Book'
    }

    void testFindByBoolean() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()

        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()
        assert publicationClass.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()

        def publication = publicationClass.recentPublications.findPaperbackByTitle('Some Book')

        assertEquals publication.title, 'Some Book'
    }

    void testFindBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 900).save()
        def recentBookId = publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 10).save().id
        session.clear()

        def publication = publicationClass.recentPublications.findByTitle('Some Book')

        assertEquals recentBookId, publication.id
    }

    void testCountBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        3.times {
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 10).save()
            assert publicationClass.newInstance(title: "Some Other Book",
                    datePublished: now - 10).save()
            assert publicationClass.newInstance(title: "Some Book",
                    datePublished: now - 900).save()
        }
        session.clear()

        def numberOfNewBooksNamedSomeBook = publicationClass.recentPublications.countByTitle('Some Book')

        assertEquals 3, numberOfNewBooksNamedSomeBook
    }

    void testListOrderBy() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()

        assert publicationClass.newInstance(title: "Book 1", datePublished: now).save()
        assert publicationClass.newInstance(title: "Book 5", datePublished: now).save()
        assert publicationClass.newInstance(title: "Book 3", datePublished: now - 900).save()
        assert publicationClass.newInstance(title: "Book 2", datePublished: now - 900).save()
        assert publicationClass.newInstance(title: "Book 4", datePublished: now).save()
        session.clear()

        def publications = publicationClass.recentPublications.listOrderByTitle()

        assertEquals 3, publications?.size()
        assertEquals 'Book 1', publications[0].title
        assertEquals 'Book 4', publications[1].title
        assertEquals 'Book 5', publications[2].title

    }

    void testGetWithIdOfObjectWhichDoesNotMatchCriteria() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def hasBookInTitle = publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10).save()
        assert hasBookInTitle
        def doesNotHaveBookInTitle = publicationClass.newInstance(title: "Some Publication",
                datePublished: now - 900).save()
        assert doesNotHaveBookInTitle

        session.clear()

        def result = publicationClass.publicationsWithBookInTitle.get(doesNotHaveBookInTitle.id)
        assertNull result
    }

    void testGetReturnsCorrectObject() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def newPublication = publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save()
        assert oldPublication

        session.clear()

        def publication = publicationClass.recentPublications.get(newPublication.id)
        assert publication
        assertEquals 'Some New Book', publication.title
    }

    void testThatParameterToGetIsConverted() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def newPublication = publicationClass.newInstance(title: "Some New Book", datePublished: now - 10).save()
        assert newPublication
        def oldPublication = publicationClass.newInstance(title: "Some Old Book",
        datePublished: now - 900).save()
        assert oldPublication

        session.clear()

        def publication = publicationClass.recentPublications.get(newPublication.id.toString())
        assert publication
        assertEquals 'Some New Book', publication.title
    }

    void testGetReturnsNull() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def newPublication = publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save()
        assert oldPublication

        session.clear()

        def publication = publicationClass.recentPublications.get(42 + oldPublication.id)

        assert !publication
    }

    void testCount() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def newPublication = publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save()
        assert oldPublication

        session.clear()
        assertEquals 2, publicationClass.publicationsWithBookInTitle.count()
        assertEquals 1, publicationClass.recentPublications.count()
    }

    void testCountWithParameterizedNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10).save()
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 10).save()
        assert publicationClass.newInstance(title: "Some Book",
                datePublished: now - 900).save()

        session.clear()
        assertEquals 2, publicationClass.recentPublicationsByTitle('Some Book').count()
    }

    void testMaxParam() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        (1..25).each {num ->
            publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: new Date()).save()
        }

        def pubs = publicationClass.recentPublications.list(max: 10)
        assertEquals 10, pubs?.size()
    }

    void testMaxResults() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        (1..25).each {num ->
            publicationClass.newInstance(title: 'Book Title',
                    datePublished: new Date() + num).save()
        }

        def pubs = publicationClass.latestBooks.list()
        assertEquals 10, pubs?.size()
    }

    void testMaxAndOffsetParam() {
        def publicationClass = ga.getDomainClass("Publication").clazz
        (1..25).each {num ->
            publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: new Date()).save()
        }

        def pubs = publicationClass.recentPublications.list(max: 10, offset: new Integer(5))
        assertEquals 10, pubs?.size()

        (6..15).each {num ->
            assert pubs.find { it.title == "Book Number ${num}" }
        }

        pubs = publicationClass.recentPublications.list(max: '10', offset: '5')
        assertEquals 10, pubs?.size()
        (6..15).each {num ->
                assert pubs.find { it.title == "Book Number ${num}" }
        }
    }

    void testFindAllWhereWithNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert publicationClass.newInstance(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

        def pubs = publicationClass.recentPublications.findAllWhere(title: 'Book Number 2')
        assertEquals 3, pubs?.size()
    }

    void testFindAllWhereWithNamedQueryAndDisjuction() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def oldDate = now - 2000

        assert publicationClass.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert publicationClass.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert publicationClass.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert publicationClass.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert publicationClass.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.clear()

        def publications = publicationClass.paperbackOrRecent.findAllWhere(title: 'Old Paperback')
        assertEquals 1, publications?.size()
        publications = publicationClass.paperbackOrRecent.findAllWhere(title: 'Old Hardback')
        assertEquals 0, publications?.size()
        publications = publicationClass.paperbackOrRecent.findAllWhere(title: 'New Paperback')
        assertEquals 2, publications?.size()
    }

    void testGetWithParameterizedNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def recentPub = publicationClass.newInstance(title: "Some Title",
                            datePublished: now).save()
        def oldPub = publicationClass.newInstance(title: "Some Title",
                            datePublished: now - 900).save()

        def pub = publicationClass.recentPublicationsByTitle('Some Title').get(oldPub.id)
        session.clear()
        assertNull pub
        pub = publicationClass.recentPublicationsByTitle('Some Title').get(recentPub.id)
        assertEquals recentPub.id, pub?.id
    }

    void testNamedQueryWithOneParameter() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert publicationClass.newInstance(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

        def pubs = publicationClass.recentPublicationsByTitle('Book Number 2').list()
        assertEquals 3, pubs?.size()
    }

    void testNamedQueryWithMultipleParameters() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            assert publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: ++now).save()
        }

        def pubs = publicationClass.publishedBetween(now-2, now).list()
        assertEquals 3, pubs?.size()
    }

    void testNamedQueryWithMultipleParametersAndDynamicFinder() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            assert publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: now + num).save()
            assert publicationClass.newInstance(title: "Another Book Number ${num}",
                    datePublished: now + num).save()
        }

        def pubs = publicationClass.publishedBetween(now, now + 2).findAllByTitleLike('Book%')
        assertEquals 2, pubs?.size()
    }

    void testNamedQueryWithMultipleParametersAndMap() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..10).each {num ->
            assert publicationClass.newInstance(title: "Book Number ${num}",
                    datePublished: ++now).save()
        }

        def pubs = publicationClass.publishedBetween(now-8, now-2).list(offset:2, max: 4)
        assertEquals 4, pubs?.size()
    }

    void testFindWhereWithNamedQuery() {
        def publicationClass = ga.getDomainClass("Publication").clazz

        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert publicationClass.newInstance(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

        def pub = publicationClass.recentPublications.findWhere(title: 'Book Number 2')
        assertEquals 'Book Number 2', pub.title
    }
}
