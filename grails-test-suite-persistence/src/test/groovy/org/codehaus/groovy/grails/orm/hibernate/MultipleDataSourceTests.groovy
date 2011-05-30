package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.HibernateException
import org.springframework.transaction.NoTransactionException

// TODO test Library + readonly datasource once H2 supports per-connection read-only (planned for 1.3)

class MultipleDataSourceTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import org.springframework.transaction.interceptor.TransactionAspectSupport
import org.codehaus.groovy.grails.orm.hibernate.MdsException
import grails.persistence.Entity

@Entity
class MdsPerson {
    String name
}

@Entity
class MdsLibrary {
    String name
    static hasMany = [visits: MdsVisit]
    static dataSources = ['ds2']
}

@Entity
class MdsVisit {
    String person
    Date visitDate
    static belongsTo = [library: MdsLibrary]
    static dataSources = ['ds2']
}

@Entity
class MdsZipCode {
    String code
    static dataSources = ['ds3']
}

class MdsService {

    MdsPerson createPerson() {
        new MdsPerson(name: 'person').save(flush: true)
        throw new MdsException('fail')
    }

    MdsLibrary createLibrary() {
        new MdsLibrary(name: 'library').save(flush: true)
        throw new MdsException('fail')
    }

    MdsZipCode createZipCode() {
        new MdsZipCode(code: '12345').save(flush: true)
        throw new MdsException('fail')
    }

    boolean transactionIsReadOnly() {
        TransactionAspectSupport.currentTransactionStatus().readOnly
    }
}

class MdsDs2Service {

    static dataSource = 'ds2'

    MdsPerson createPerson() {
        new MdsPerson(name: 'person').save(flush: true)
        throw new MdsException('fail')
    }

    MdsLibrary createLibrary() {
        new MdsLibrary(name: 'library').save(flush: true)
        throw new MdsException('fail')
    }

    MdsZipCode createZipCode() {
        new MdsZipCode(code: '12345').save(flush: true)
        throw new MdsException('fail')
    }

    boolean transactionIsReadOnly() {
        TransactionAspectSupport.currentTransactionStatus().readOnly
    }
}

class MdsDs3Service {

    static dataSource = 'ds3'

    MdsPerson createPerson() {
        new MdsPerson(name: 'person').save(flush: true)
        throw new MdsException('fail')
    }

    MdsLibrary createLibrary() {
        new MdsLibrary(name: 'library').save(flush: true)
        throw new MdsException('fail')
    }

    MdsZipCode createZipCode() {
        new MdsZipCode(code: '12345').save(flush: true)
        throw new MdsException('fail')
    }

    boolean transactionIsReadOnly() {
        TransactionAspectSupport.currentTransactionStatus().readOnly
    }
}

class MdsNonTxService {

    static transactional = false

    MdsPerson createPerson() {
        new MdsPerson(name: 'person').save(flush: true)
        throw new MdsException('fail')
    }

    MdsLibrary createLibrary() {
        new MdsLibrary(name: 'library').save(flush: true)
        throw new MdsException('fail')
    }

    MdsZipCode createZipCode() {
        new MdsZipCode(code: '12345').save(flush: true)
        throw new MdsException('fail')
    }

    boolean transactionIsReadOnly() {
        TransactionAspectSupport.currentTransactionStatus().readOnly
    }
}
'''
    }

    @Override
    protected void configureDataSource() {
        gcl.parseClass('''
dataSource {
    pooled = true
    driverClassName = 'org.h2.Driver'
    url = 'jdbc:h2:mem:testDb'
    username = 'sa'
    password = ''
}

dataSource_ds2 {
    driverClassName = 'org.h2.Driver'
    url = 'jdbc:h2:mem:testDb2'
    username = 'sa'
    password = ''
    dialect = org.hibernate.dialect.H2Dialect
}

dataSource_ds3 {
    driverClassName = 'org.h2.Driver'
    url = 'jdbc:h2:mem:testDb3'
    username = 'sa'
    password = ''
    readOnly = true
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
}
''', 'DataSource')
    }

    void testBeans() {

        assertTrue appCtx.containsBean('dataSource')
        assertTrue appCtx.containsBean('dataSource_ds2')
        assertTrue appCtx.containsBean('dataSource_ds3')

        assertTrue appCtx.containsBean('dataSourceUnproxied')
        assertTrue appCtx.containsBean('dataSourceUnproxied_ds2')
        assertTrue appCtx.containsBean('dataSourceUnproxied_ds3')

        assertTrue appCtx.containsBean('dialectDetector')
        assertTrue appCtx.containsBean('dialectDetector_ds3')
        assertFalse 'dialect is specified', appCtx.containsBean('dialectDetector_ds2')

        assertTrue appCtx.containsBean('flushingRedirectEventListener')
        assertTrue appCtx.containsBean('flushingRedirectEventListener_ds2')
        assertTrue appCtx.containsBean('flushingRedirectEventListener_ds3')

        assertTrue appCtx.containsBean('hibernateProperties')
        assertTrue appCtx.containsBean('hibernateProperties_ds2')
        assertTrue appCtx.containsBean('hibernateProperties_ds3')

        assertTrue appCtx.containsBean('lobHandlerDetector')
        assertTrue appCtx.containsBean('lobHandlerDetector_ds2')
        assertTrue appCtx.containsBean('lobHandlerDetector_ds3')

        assertTrue appCtx.containsBean('openSessionInViewInterceptor')
        assertTrue appCtx.containsBean('openSessionInViewInterceptor_ds2')
        assertTrue appCtx.containsBean('openSessionInViewInterceptor_ds3')

        assertTrue appCtx.containsBean('sessionFactory')
        assertTrue appCtx.containsBean('sessionFactory_ds2')
        assertTrue appCtx.containsBean('sessionFactory_ds3')

        assertTrue appCtx.containsBean('transactionManager')
        assertTrue appCtx.containsBean('transactionManager_ds2')
        assertTrue appCtx.containsBean('transactionManager_ds3')

        assertTrue appCtx.containsBean('MdsLibrary')
        assertTrue appCtx.containsBean('MdsLibraryDomainClass')
        assertTrue appCtx.containsBean('MdsLibraryPersistentClass')
        assertTrue appCtx.containsBean('MdsLibraryValidator_ds2')
        assertTrue 'extra, created by DomainClassGrailsPlugin', appCtx.containsBean('MdsLibraryValidator')
        assertFalse appCtx.containsBean('MdsLibraryValidator_ds3')

        assertTrue appCtx.containsBean('MdsPerson')
        assertTrue appCtx.containsBean('MdsPersonDomainClass')
        assertTrue appCtx.containsBean('MdsPersonPersistentClass')
        assertTrue appCtx.containsBean('MdsPersonValidator')
        assertFalse appCtx.containsBean('MdsPersonValidator_ds2')
        assertFalse appCtx.containsBean('MdsPersonValidator_ds3')

        assertTrue appCtx.containsBean('MdsVisit')
        assertTrue appCtx.containsBean('MdsVisitDomainClass')
        assertTrue appCtx.containsBean('MdsVisitPersistentClass')
        assertTrue appCtx.containsBean('MdsVisitValidator_ds2')
        assertTrue 'extra, created by DomainClassGrailsPlugin', appCtx.containsBean('MdsVisitValidator')
        assertFalse appCtx.containsBean('MdsVisitValidator_ds3')

        assertTrue appCtx.containsBean('MdsZipCode')
        assertTrue appCtx.containsBean('MdsZipCodeDomainClass')
        assertTrue appCtx.containsBean('MdsZipCodePersistentClass')
        assertTrue appCtx.containsBean('MdsZipCodeValidator_ds3')
        assertTrue 'extra, created by DomainClassGrailsPlugin', appCtx.containsBean('MdsZipCodeValidator')
        assertFalse appCtx.containsBean('MdsZipCodeValidator_ds2')

        assertTrue appCtx.containsBean('mdsService')
        assertTrue appCtx.containsBean('MdsServiceServiceClass')

        assertTrue appCtx.containsBean('mdsDs2Service')
        assertTrue appCtx.containsBean('MdsDs2ServiceServiceClass')

        assertTrue appCtx.containsBean('mdsDs3Service')
        assertTrue appCtx.containsBean('MdsDs3ServiceServiceClass')

        assertTrue appCtx.containsBean('mdsNonTxService')
        assertTrue appCtx.containsBean('MdsNonTxServiceServiceClass')
    }

    void testServices_default() {

        def Person = ga.getDomainClass('MdsPerson').clazz
        def Library = ga.getDomainClass('MdsLibrary').clazz
        def Visit = ga.getDomainClass('MdsVisit').clazz
        def ZipCode = ga.getDomainClass('MdsZipCode').clazz

        def service = appCtx.mdsService

        int personCount = Person.count()
        shouldFail(MdsException) {
            service.createPerson()
        }
        assertEquals 'Should have rolled back', personCount, Person.count()

//        int libraryCount = Library.count()
//        shouldFail(org.springframework.dao.TransientDataAccessResourceException) {
//            service.createLibrary()
//        }
//        assertEquals 'read-only', libraryCount, Library.count()

        int zipCodeCount = ZipCode.count()
        assertEquals 0, ZipCode.count()
        shouldFail(MdsException) {
            service.createZipCode()
        }
        assertEquals 'should not have rolled back, wrong DataSource', zipCodeCount + 1, ZipCode.count()

        assertFalse service.transactionIsReadOnly()
    }

    void testServices_ds2() {

        def Person = ga.getDomainClass('MdsPerson').clazz
        def Library = ga.getDomainClass('MdsLibrary').clazz
        def Visit = ga.getDomainClass('MdsVisit').clazz
        def ZipCode = ga.getDomainClass('MdsZipCode').clazz

        def service = appCtx.mdsDs2Service

        int personCount = Person.count()
        shouldFail(MdsException) {
            service.createPerson()
        }
        assertEquals 'should not have rolled back, wrong DataSource', personCount + 1, Person.count()

//        int libraryCount = Library.count()
//        shouldFail(org.springframework.dao.TransientDataAccessResourceException) {
//            service.createLibrary()
//        }
//        assertEquals 'read-only', libraryCount, Library.count()

        int zipCodeCount = ZipCode.count()
        shouldFail(MdsException) {
            service.createZipCode()
        }
        assertEquals 'should not have rolled back, wrong DataSource', zipCodeCount + 1, ZipCode.count()

        assertFalse service.transactionIsReadOnly()
    }

    void testServices_ds3() {

        def Person = ga.getDomainClass('MdsPerson').clazz
        def Library = ga.getDomainClass('MdsLibrary').clazz
        def Visit = ga.getDomainClass('MdsVisit').clazz
        def ZipCode = ga.getDomainClass('MdsZipCode').clazz

        def service = appCtx.mdsDs3Service

        int personCount = Person.count()
        shouldFail(MdsException) {
            service.createPerson()
        }
        assertEquals 'should not have rolled back, wrong DataSource', personCount + 1, Person.count()

//        int libraryCount = Library.count()
//        shouldFail(org.springframework.dao.TransientDataAccessResourceException) {
//            service.createLibrary()
//        }
//        assertEquals 'read-only', libraryCount, Library.count()

        int zipCodeCount = ZipCode.count()
        shouldFail(MdsException) {
            service.createZipCode()
        }
        assertEquals 'Should have rolled back', zipCodeCount, ZipCode.count()

        assertTrue service.transactionIsReadOnly()
    }
}

class MdsException extends RuntimeException {
    MdsException(String message) {
        super(message)
    }
}
