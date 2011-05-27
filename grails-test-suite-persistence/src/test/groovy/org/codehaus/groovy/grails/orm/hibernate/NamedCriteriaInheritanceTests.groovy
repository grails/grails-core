package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class NamedCriteriaInheritanceTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [NamedCriteriaPublication,
         NamedCriteriaPublicationSubclassWithNamedQueries,
         NamedCriteriaPublicationSubclassWithoutNamedQueries]
    }

    void testInheritedNamedQueries() {
        def now = new Date()
        assert new NamedCriteriaPublicationSubclassWithoutNamedQueries(title: "Some New Book",
                datePublished: now - 10).save()
        assert new NamedCriteriaPublicationSubclassWithoutNamedQueries(title: "Some Old Book",
                datePublished: now - 900).save()

        session.clear()

        def publications = NamedCriteriaPublicationSubclassWithoutNamedQueries.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title

        now = new Date()
        assert new NamedCriteriaPublicationSubclassWithNamedQueries(title: "Some New Book",
                datePublished: now - 10).save()
        assert new NamedCriteriaPublicationSubclassWithNamedQueries(title: "Some Old Book",
                datePublished: now - 900).save()

        session.clear()

        publications = NamedCriteriaPublicationSubclassWithNamedQueries.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title

        publications = NamedCriteriaPublicationSubclassWithNamedQueries.oldPaperbacks.list()
        assertEquals 1, publications?.size()
        assertEquals 'Some Old Book', publications[0].title
    }


}
