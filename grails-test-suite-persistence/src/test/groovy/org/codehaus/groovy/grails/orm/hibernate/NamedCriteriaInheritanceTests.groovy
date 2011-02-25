package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class NamedCriteriaInheritanceTests extends AbstractGrailsHibernateTests {
    
    void onApplicationCreated() {
        def domainClasses = [NamedCriteriaPublication,
                             NamedCriteriaPublicationSubclassWithNamedQueries, 
                             NamedCriteriaPublicationSubclassWithoutNamedQueries]
        
        domainClasses.each {
            ga.addArtefact 'Domain', it
        }
    }

    void testInheritedNamedQueries() {
        def publicationClass = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.NamedCriteriaPublicationSubclassWithoutNamedQueries").clazz

        def now = new Date()
        assert publicationClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert publicationClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save()

        session.clear()

        def publications = publicationClass.recentPublications.list()

        assertEquals 1, publications?.size()
        assertEquals 'Some New Book', publications[0].title

        publicationClass = ga.getDomainClass("org.codehaus.groovy.grails.orm.hibernate.NamedCriteriaPublicationSubclassWithNamedQueries").clazz

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


}
