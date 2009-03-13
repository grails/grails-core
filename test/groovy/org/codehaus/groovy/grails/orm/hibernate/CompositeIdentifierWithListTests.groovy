package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 22, 2009
 */

public class CompositeIdentifierWithListTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Article {

    
    List revisions
	static hasMany = [ revisions: ArticleRevision ]

    static constraints = {
        revisions(minSize:1)
    }
}

@Entity
class ArticleRevision implements Serializable{

    String title

    int revision

    static belongsTo = [article:Article]

    static mapping = {
       id composite:['article','revision']
    }

}
''')
    }

    void testCompositeIdentifierWithList() {

        def articleClass = ga.getDomainClass("Article").clazz
        def revisionClass = ga.getDomainClass("ArticleRevision").clazz

        def article = articleClass.newInstance()

        article.addToRevisions(title:"one", revision:1)
        assertNotNull "should have saved", article.save(flush:true)

        session.clear()

        article = articleClass.get(1)

        assertEquals "one",article.revisions[0].title

        session.clear()

        article = articleClass.get(1)

        def revision = revisionClass.get(revisionClass.newInstance(article:article, revision:1))

        assertNotNull "many-to-one should have been loaded",revision.article
        
    }


}