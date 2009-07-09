package org.codehaus.groovy.grails.orm.hibernate.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Feb 4, 2009
 */

public class CascadingValidationWithInheritance2Tests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Article extends Content {}

@Entity
class Content implements Serializable {

    Date dateCreated
    Date lastUpdated

    List revisions    

	static hasMany = [
	    revisions: ContentRevision
	]
}


@Entity
class ContentRevision implements Serializable {

    Date dateCreated

    static belongsTo = [
        content: Content
    ]

}

@Entity
class ArticleRevision extends ContentRevision {

    static constraints = {
        title(blank:false)
        body(blank:false)
    }


    static mapping = {
        body(type:"text")
    }

    String title
    String body
}

''')
    }


    void testCascadingValidationFromSubclasses() {
        def Article = ga.getDomainClass("Article").clazz
        def ArticleRevision = ga.getDomainClass("ArticleRevision").clazz

        def article = Article.newInstance()

        article.addToRevisions(ArticleRevision.newInstance(title:"", body:""))

        assertNull "should have failed cascading validation", article.save(flush:true)

        assertNotNull "title should not have been allowed to be blank", article.errors.getFieldError("revisions.title")
        assertNotNull "body should not have been allowed to be blank", article.errors.getFieldError("revisions.body")

    }

}