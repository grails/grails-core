package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 20, 2009
 */

public class OneToManyWithComposideIdentifierTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class BlogArticle extends Content { }

@Entity
class BlogArticleRevision extends ContentRevision {
       String title
       String body
}

@Entity
class Content implements Serializable {
      Date dateCreated
      Date lastUpdated
      static hasMany = [ revisions: ContentRevision ]
      static mapping = {
          table 'test_content'
      }
}     

@Entity
class ContentRevision implements Serializable {
     int revision
     Date dateCreated
     static belongsTo = [ content: Content ]
     static mapping = {
         table 'test_content_revision'
         id composite: [ 'content', 'revision' ]
     }
}
''')
    }


    void testPersistAssociationWithCompositeId() {
	    def content = ga.getDomainClass("BlogArticle").newInstance()
        def contentRevision = ga.getDomainClass("BlogArticleRevision").clazz.newInstance(
           title:"The blog post",
           body:"The body of the post",
           revision:0
           )
        content.addToRevisions(contentRevision)
		content.save(flush:true, insert:true)
    }

    void testUpdateInverseSide() {
	    def content = ga.getDomainClass("BlogArticle").newInstance()
        content.save(flush:true)
        def contentRevision = ga.getDomainClass("BlogArticleRevision").clazz.newInstance(
           title:"The blog post",
           body:"The body of the post",
           revision:0
           )
        contentRevision.save(flush:true)
    }
}