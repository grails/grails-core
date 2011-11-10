package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.hibernate.SessionFactory

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
    static constraints = {
        name unique: true
    }
}

@Entity
class MdsLibrary {
    String name
    static hasMany = [visits: MdsVisit]
    static mapping = {
        datasource 'ds2'
    }
}

@Entity
class MdsVisit {
    String person
    Date visitDate
    static belongsTo = [library: MdsLibrary]
    static mapping = {
        datasource 'ds2'
    }
}

@Entity
class MdsZipCode {
    String code
    static mapping = {
        datasource 'ds3'
    }
    static constraints = {
        code unique: true
    }
}

@Entity
class MdsTwoDatasources {
    String name
    static mapping = {
        datasources(['ds2', 'ds3'])
    }
}

@Entity
class MdsAllDatasources {
    String name
    static mapping = {
        datasource 'ALL'
    }
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

    static datasource = 'ds2'

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

    static datasource = 'ds3'

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
    dbCreate = 'update'
}

dataSource_ds2 {
    pooled = true
    driverClassName = 'org.h2.Driver'
    url = 'jdbc:h2:mem:testDb2'
    username = 'sa'
    password = ''
    dialect = org.hibernate.dialect.H2Dialect
    dbCreate = 'update'
}

dataSource_ds3 {
    pooled = true
    driverClassName = 'org.h2.Driver'
    url = 'jdbc:h2:mem:testDb3'
    username = 'sa'
    password = ''
    readOnly = true
    dbCreate = 'update'
}

dataSource_ds4 {
    pooled = true
    driverClassName = 'org.h2.Driver'
    url = 'jdbc:h2:mem:testDb4'
    username = 'sa'
    password = ''
    dbCreate = 'update'
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
}
''', 'DataSource')
    }

    @Override
    protected void registerHibernateSession() {
        super.registerHibernateSession()

        appCtx.getBeansOfType(SessionFactory).each { name, bean ->
            if (!GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN.equals(name)) {
                bindSessionFactory bean
            }
        }
    }

    @Override
    protected void unregisterHibernateSession() {
        super.unregisterHibernateSession()

        appCtx.getBeansOfType(SessionFactory).each { name, bean ->
            if (!GrailsRuntimeConfigurator.SESSION_FACTORY_BEAN.equals(name)) {
                unbindSessionFactory sessionFactory
            }
        }
    }

    @Override
    protected void tearDown() {
        for (String name in ['MdsPerson', 'MdsLibrary', 'MdsVisit', 'MdsZipCode']) {
            ga.getDomainClass(name).clazz.list()*.delete(flush: true)
        }
        super.tearDown()
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

    void testPartitioning() {
        Map meta = appCtx.sessionFactory.allClassMetadata
        assertEquals 2, meta.size()
        assertTrue meta.keySet().contains('MdsAllDatasources')
        assertTrue meta.keySet().contains('MdsPerson')

        meta = appCtx.sessionFactory_ds2.allClassMetadata
        assertEquals 4, meta.size()
        assertTrue meta.keySet().contains('MdsAllDatasources')
        assertTrue meta.keySet().contains('MdsTwoDatasources')
        assertTrue meta.keySet().contains('MdsLibrary')
        assertTrue meta.keySet().contains('MdsVisit')

        meta = appCtx.sessionFactory_ds3.allClassMetadata
        assertEquals 3, meta.size()
        assertTrue meta.keySet().contains('MdsAllDatasources')
        assertTrue meta.keySet().contains('MdsTwoDatasources')
        assertTrue meta.keySet().contains('MdsZipCode')
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

    void testNamespaces_Two() {

        def TwoDatasourcesDomainClass = ga.getDomainClass('MdsTwoDatasources')
        assertEquals(['ds2', 'ds3'], GrailsHibernateUtil.getDatasourceNames(TwoDatasourcesDomainClass))
        assertEquals 'ds2', GrailsHibernateUtil.getDefaultDataSource(TwoDatasourcesDomainClass)

        def TwoDatasources = TwoDatasourcesDomainClass.clazz
        def instance = TwoDatasources.newInstance()

        assertFalse instance.validate()
        assertFalse instance.ds2.validate()
        assertFalse instance.ds3.validate()
        shouldFail(MissingPropertyException) {
            println instance.ds4.validate()
        }

        instance.name = 'two!'

        assertTrue instance.validate()
        assertTrue instance.ds2.validate()
        assertTrue instance.ds3.validate()
        assertEquals 0, TwoDatasources.count()
        assertEquals 0, TwoDatasources.ds2.count()
        assertEquals 0, TwoDatasources.ds3.count()

        assertNotNull instance.save(flush: true)
        appCtx.sessionFactory.currentSession.clear()
        def session2 = appCtx.sessionFactory_ds2.currentSession
        session2.clear()

        assertEquals 1, TwoDatasources.count()
        assertEquals 1, TwoDatasources.ds2.count()
        assertEquals 0, TwoDatasources.ds3.count()

        instance.name += 'updated'
        assertNotNull instance.ds2.save(flush: true)

        session2.clear()
        assertEquals 'two!updated', TwoDatasources.get(instance.id).name

        // need these 2 lines otherwise it looks like it's already persistent and you'll get an opt lock exception
        instance.id = null
        instance.version = null
        assertNotNull instance.ds3.save(flush: true)
        appCtx.sessionFactory_ds3.currentSession.clear()

        assertEquals 1, TwoDatasources.count()
        assertEquals 1, TwoDatasources.ds2.count()
        assertEquals 1, TwoDatasources.ds3.count()

        assertEquals 1, TwoDatasources.findAllByName(instance.name).size()
        assertEquals 1, TwoDatasources.ds2.findAllByName(instance.name).size()
        assertEquals 1, TwoDatasources.ds3.findAllByName(instance.name).size()
    }

    void testNamespaces_All() {

        def AllDatasourcesDomainClass = ga.getDomainClass('MdsAllDatasources')
        assertEquals(['ALL'], GrailsHibernateUtil.getDatasourceNames(AllDatasourcesDomainClass))
        assertEquals 'DEFAULT', GrailsHibernateUtil.getDefaultDataSource(AllDatasourcesDomainClass)

        def AllDatasources = AllDatasourcesDomainClass.clazz

        def instance = AllDatasources.newInstance()

        assertFalse instance.validate()
        assertFalse instance.ds2.validate()
        assertFalse instance.ds3.validate()
        assertFalse instance.ds4.validate()
        shouldFail(MissingPropertyException) {
            println instance.ds5.validate()
        }

        instance.name = 'all!'

        assertTrue instance.validate()
        assertTrue instance.ds2.validate()
        assertTrue instance.ds3.validate()
        assertTrue instance.ds4.validate()
        assertEquals 0, AllDatasources.count()
        assertEquals 0, AllDatasources.ds2.count()
        assertEquals 0, AllDatasources.ds3.count()
        assertEquals 0, AllDatasources.ds4.count()

        assertNotNull instance.save(flush: true)
        appCtx.sessionFactory.currentSession.clear()

        assertEquals 1, AllDatasources.count()
        assertEquals 0, AllDatasources.ds2.count()
        assertEquals 0, AllDatasources.ds3.count()
        assertEquals 0, AllDatasources.ds4.count()

        // need these 2 lines otherwise it looks like it's already persistent and you'll get an opt lock exception
        instance.id = null
        instance.version = null
        assertNotNull instance.ds2.save(flush: true)
        appCtx.sessionFactory_ds2.currentSession.clear()

        assertEquals 1, AllDatasources.count()
        assertEquals 1, AllDatasources.ds2.count()
        assertEquals 0, AllDatasources.ds3.count()
        assertEquals 0, AllDatasources.ds4.count()

        instance.id = null
        instance.version = null
        assertNotNull instance.ds3.save(flush: true)
        appCtx.sessionFactory_ds3.currentSession.clear()

        assertEquals 1, AllDatasources.count()
        assertEquals 1, AllDatasources.ds2.count()
        assertEquals 1, AllDatasources.ds3.count()
        assertEquals 0, AllDatasources.ds4.count()

        instance.id = null
        instance.version = null
        assertNotNull instance.ds4.save(flush: true)
        appCtx.sessionFactory_ds4.currentSession.clear()

        assertEquals 1, AllDatasources.count()
        assertEquals 1, AllDatasources.ds2.count()
        assertEquals 1, AllDatasources.ds3.count()
        assertEquals 1, AllDatasources.ds4.count()

        assertEquals 1, AllDatasources.findAllByName(instance.name).size()
        assertEquals 1, AllDatasources.ds2.findAllByName(instance.name).size()
        assertEquals 1, AllDatasources.ds3.findAllByName(instance.name).size()
        assertEquals 1, AllDatasources.ds4.findAllByName(instance.name).size()
    }

    void testUnique() {
        def Person = ga.getDomainClass('MdsPerson').clazz

        def data = [name: 'person 1']

        def person = Person.newInstance(data)
        person.save()
        assertFalse person.hasErrors()

        person = Person.newInstance(data)
        person.save()

        assertEquals 1, person.errors.errorCount
        def error = person.errors.getFieldError('name')
        assertNotNull error
        assertEquals 'unique', error.code

        def ZipCode = ga.getDomainClass('MdsZipCode').clazz

        data = [code: 'zip 1']
        def zipCode = ZipCode.newInstance(data)
        zipCode.save()
        assertFalse zipCode.hasErrors()

        zipCode = ZipCode.newInstance(data)
        zipCode.save()
        assertTrue zipCode.hasErrors()

        assertEquals 1, zipCode.errors.errorCount
        error = zipCode.errors.getFieldError('code')
        assertNotNull error
        assertEquals 'unique', error.code
    }
}

class MdsException extends RuntimeException {
    MdsException(String message) {
        super(message)
    }
}
