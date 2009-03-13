package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class CompositeIdentifierNoVersionTests extends AbstractGrailsHibernateTests{

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
       table 'content'
       version false
   }
   static constraints = {
       revisions(minSize:1)
   }
}

@Entity
class ContentRevision implements Serializable {
   int revision
   Date dateCreated
   static belongsTo = [ content: Content ]
   static mapping = {
       table 'content_revision'
       version false
       id composite: [ 'content', 'revision' ]
   }
}
''')
    }


    void testCompositeIdentifierWithNoVersion() {
        def BlogArticle = ga.getDomainClass("BlogArticle").clazz
        def BlogArticleRevision = ga.getDomainClass("BlogArticleRevision").clazz

        def content = BlogArticle.newInstance()
        def revision = BlogArticleRevision.newInstance(title:"Test",body:"The Body", revision:0)
        content.addToRevisions(revision)
        
        assertNotNull content.save(flush:true)
        
    }
}