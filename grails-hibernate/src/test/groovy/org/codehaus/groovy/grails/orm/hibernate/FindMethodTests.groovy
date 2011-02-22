package org.codehaus.groovy.grails.orm.hibernate

class FindMethodTests extends AbstractGrailsHibernateTests {

    void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class FindMethodTestClass {
    String one
    Integer two
}
'''
    }

    void testFindMethodWithHQL() {
        def domain = ga.getDomainClass("FindMethodTestClass").clazz

        assertNotNull "should have saved", domain.newInstance(one:"one", two:2).save(flush:true)

        session.clear()

        assert domain.find("from FindMethodTestClass as f where f.one = ? and f.two = ?", ["one", 2]) : "should have returned a result"
    }
    
    void testUsingHibernateCache() {
        def theClass = ga.getDomainClass("FindMethodTestClass").clazz

        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true
        stats.clear()
        
        def cacheStats = stats.getSecondLevelCacheStatistics('org.hibernate.cache.StandardQueryCache')
        assertEquals 0, cacheStats.hitCount
        assertEquals 0, cacheStats.missCount
        assertEquals 0, cacheStats.putCount

        theClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 0, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 1, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = 'Angus'")
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = 'Angus'", [cache: false])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = 'Malcolm'", [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = 'Malcolm'", [cache: true])
        assertEquals 4, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = :name", [name: 'Brian'], [cache: true])
        assertEquals 4, cacheStats.hitCount
        assertEquals 3, cacheStats.missCount
        assertEquals 3, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = :name", [name: 'Brian'], [cache: true])
        assertEquals 5, cacheStats.hitCount
        assertEquals 3, cacheStats.missCount
        assertEquals 3, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = :name", [name: 'Bon'], [cache: true])
        assertEquals 5, cacheStats.hitCount
        assertEquals 4, cacheStats.missCount
        assertEquals 4, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = :name", [name: 'Bon'], [cache: false])
        assertEquals 5, cacheStats.hitCount
        assertEquals 4, cacheStats.missCount
        assertEquals 4, cacheStats.putCount
        
        theClass.find("from FindMethodTestClass where one = :name", [name: 'Bon'], [cache: true])
        assertEquals 6, cacheStats.hitCount
        assertEquals 4, cacheStats.missCount
        assertEquals 4, cacheStats.putCount
    }
}