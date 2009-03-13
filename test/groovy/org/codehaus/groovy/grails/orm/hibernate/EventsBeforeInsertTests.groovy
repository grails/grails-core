/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 15, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class EventsBeforeInsertTests extends AbstractGrailsHibernateTests{
                             
    protected void onSetUp() {
        gcl.parseClass('''
class BeforeInsertExample {

Long id
Long version
BeforeInsertArticle article
BeforeInsertArticle news
String moduleName = ''

static constraints = {
    article(nullable:true)
    news(nullable:true)
}

def beforeInsert = {
    println "INSIDE BEFORE INSERT!!!!!! $article $news"
        if (article) {
            moduleName = 'article'
        } else if (news) {
            moduleName = 'news'
        }
}

}
class BeforeInsertArticle {
  Long id
  Long version
  static belongsTo = BeforeInsertExample
}
''')
    }


    void testBeforeInsertEvent() {
        def exampleClass = ga.getDomainClass("BeforeInsertExample")
        def articleClass = ga.getDomainClass("BeforeInsertArticle")

        def e = exampleClass.newInstance()
        e.news = articleClass.newInstance()

        assert e.save()
        session.flush()

        assertEquals "news", e.moduleName

    }

}