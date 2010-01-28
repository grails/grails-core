package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Jeff Brown
 */
class NamedCriteriaTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''

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
           def now = new Date()
           gt 'datePublished', now - 365
           eq 'title', title
       }

       latestBooks {
 			maxResults(10)
 			order("datePublished", "desc")
       }

       publishedBetween { start, end ->
           between 'datePublished', start, end
       }

       paperbackOrRecent {
           or {
        		def now = new Date()
        		gt 'datePublished', now - 365
        		eq 'paperback', true
           }
       }

       paperbackAndRecent {
           and {
               def now = new Date()
               gt 'datePublished', now - 365
               eq 'paperback', true
           }
       }
   }
}
''')
    }
	
	void testReferencingNamedQueryBeforeAnyDynamicMethodsAreInvoked() {
		if(notYetImplemented()) return
		
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

        def pubs = publicationClass.recentPublications.list(max: 10, offset: 5)
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