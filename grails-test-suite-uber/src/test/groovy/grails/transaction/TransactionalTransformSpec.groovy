/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.transaction

import grails.spring.BeanBuilder
import org.codehaus.groovy.control.MultipleCompilationErrorsException

import javax.annotation.PostConstruct
import javax.sql.DataSource

import grails.core.DefaultGrailsApplication
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager

import spock.lang.Issue
import spock.lang.Specification

/**
 */
class TransactionalTransformSpec extends Specification {
    @Issue('GRAILS-10402')
    void "Test @Transactional annotation with inheritance"() {
        when:"A new instance of a class with a @Transactional method is created that subclasses another transactional class"
            def bookService = new GroovyShell().evaluate('''
    import grails.transaction.*
    import grails.transaction.TransactionManagerAware
    import org.springframework.transaction.PlatformTransactionManager
    import org.springframework.transaction.TransactionStatus
    import org.springframework.transaction.annotation.Isolation
    import org.springframework.transaction.annotation.Propagation

    @Transactional
    class ParentService {
           void doWork() {}
    }

    class BookService extends ParentService{

        void updateBook() {

        }

        @Transactional(readOnly = true, timeout = 1000, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
        TransactionStatus readBook() {
             return transactionStatus
        }

        int add(int a, int b) {
            a + b
        }
    }

    new BookService()
    ''')

        then:"It implements TransactionManagerAware"
            bookService instanceof TransactionManagerAware


        when:"A transactionManager is set"
            final transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        then:"It is not null"
            bookService.transactionManager != null

        when:"A non-transactional method is called"
            bookService.updateBook()

        then:"The transaction was not started"
            transactionManager.transactionStarted == false

        when:"A transactional method is called"
            bookService.readBook()

        then:"The transaction was started"
            transactionManager.transactionStarted == true

        when:"A parent method that starts a transactiona is called"
            transactionManager.transactionStarted = false
            bookService.doWork()

        then:"The transaction was started"
            transactionManager.transactionStarted == true



    }

    void "Test that overriding the transaction manager with a custom setter works"() {
        when:"A new instance of a class with a @Transactional method is created"
        def bookService = new GroovyShell().evaluate('''
import grails.transaction.*
import grails.transaction.TransactionManagerAware
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.beans.factory.annotation.*

@Transactional
class BookService {

    private PlatformTransactionManager transactionManager

    @Autowired
    @Qualifier("transactionManager_configurationData")
    void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager
    }
    @Transactional(readOnly = true, timeout = 1000, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    TransactionStatus readBook() {
         return transactionStatus
    }

    void updateBook() {

    }
    int add(int a, int b) {
        a + b
    }
}

new BookService()
''')

        then:"It implements TransactionManagerAware"
            bookService instanceof TransactionManagerAware


        when:"A transactionManager is set"
            final transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        then:"It is not null"
            bookService.transactionManager != null

        when:"A transactional method is called"
            bookService.updateBook()

        then:"The transaction was started"
            transactionManager.transactionStarted == true


        when:"A transactional method that takes arguments is called"
            def result = bookService.add(1, 2)

        then:"THe variables can be referenced"
            result == 3

        when:"When a read-only transaction is created"
            DefaultTransactionStatus status = (DefaultTransactionStatus )bookService.readBook()

        then:"The transaction definition is read-only"
            status.isReadOnly()
    }

    void "Test that a @Transactional annotation on a class results in a call to TransactionTemplate"() {
        when:"A new instance of a class with a @Transactional method is created"
        def bookService = new GroovyShell().evaluate('''
import grails.transaction.*
import grails.transaction.TransactionManagerAware
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation

@Transactional
class BookService {

    void updateBook() {

    }

    @Transactional(readOnly = true, timeout = 1000, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    TransactionStatus readBook() {
         return transactionStatus
    }

    int add(int a, int b) {
        a + b
    }
}

new BookService()
''')

        then:"It implements TransactionManagerAware"
            bookService instanceof TransactionManagerAware


        when:"A transactionManager is set"
            final transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        then:"It is not null"
            bookService.transactionManager != null

        when:"A transactional method is called"
            bookService.updateBook()

        then:"The transaction was started"
            transactionManager.transactionStarted == true


        when:"A transactional method that takes arguments is called"
            def result = bookService.add(1, 2)

        then:"THe variables can be referenced"
            result == 3

        when:"When a read-only transaction is created"
            DefaultTransactionStatus status = (DefaultTransactionStatus )bookService.readBook()

        then:"The transaction definition is read-only"
            status.isReadOnly()

    }

    void "Test that a @Transactional annotation on a method results in a call to TransactionTemplate"() {
        when:"A new instance of a class with a @Transactional method is created"
            def bookService = new GroovyShell().evaluate('''
import grails.transaction.*
import grails.transaction.TransactionManagerAware
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation

class BookService {

    @Transactional
    void updateBook() {

    }

    @Transactional(readOnly = true, timeout = 1000, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    TransactionStatus readBook() {
         return transactionStatus
    }

    @Transactional
    int add(int a, int b) {
        a + b
    }
}

new BookService()
''')

        then:"It implements TransactionManagerAware"
            bookService instanceof TransactionManagerAware


        when:"A transactionManager is set"
            final transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        then:"It is not null"
            bookService.transactionManager != null

        when:"A transactional method is called"
            bookService.updateBook()

        then:"The transaction was started"
            transactionManager.transactionStarted == true


        when:"A transactional method that takes arguments is called"
            def result = bookService.add(1, 2)

        then:"THe variables can be referenced"
            result == 3

        when:"When a read-only transaction is created"
            DefaultTransactionStatus status = (DefaultTransactionStatus )bookService.readBook()

        then:"The transaction definition is read-only"
            status.isReadOnly()

    }

    @Issue("GRAILS-10557")
    void "Test rollback with @Transactional annotation"() {
        when:"A new instance of a class with a @Transactional method is created"
            def bookService = new GroovyShell().evaluate('''
import grails.transaction.*
import grails.transaction.TransactionManagerAware
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation

class BookService {

    @Transactional
    void throwRuntimeException() {
        throw new TestTransactionRuntimeException()
    }

    @Transactional
    void throwException() {
        throw new TestTransactionException()
    }

}

new BookService()
''')

        then:"It implements TransactionManagerAware"
            bookService instanceof TransactionManagerAware

        when:"A transactionManager is set"
            def transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        and:"A transactional method throw RuntimeException"
            bookService.throwRuntimeException()

        then:"The transaction was rolled back"
            thrown(TestTransactionRuntimeException)
            transactionManager.transactionRolledBack == true

        when:"A transactionManager is set"
            transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        and:"A transactional method throw RuntimeException"
            bookService.throwException()

        then:"The transaction wasn't rolled back"
            thrown(TestTransactionException)
            transactionManager.transactionRolledBack == false
    }

    @Issue("GRAILS-10564")
    void "Test rollback with @Transactional annotation attributes"() {
        when:"A new instance of a class with a @Transactional method is created"
            def bookService = new GroovyShell().evaluate('''
import grails.transaction.*
import grails.transaction.TransactionManagerAware
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation

class BookService {

    @Transactional(noRollbackFor = [TestTransactionRuntimeException])
    void noRollbackForMethod() {
        throw new TestTransactionRuntimeException()
    }

    @Transactional(noRollbackForClassName = ["TestTransactionRuntimeException"])
    void noRollbackForClassNameMethod() {
        throw new TestTransactionRuntimeException()
    }

    @Transactional(rollbackFor = TestTransactionException)
    void rollbackForMethod() {
        throw new TestTransactionException()
    }

    @Transactional(rollbackForClassName = "TestTransactionException")
    void rollbackForClassNameMethod() {
        throw new TestTransactionException()
    }

}

new BookService()
''')

        then:"It implements TransactionManagerAware"
            bookService instanceof TransactionManagerAware

        when:"A transactionManager is set"
            def transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        and:"A transactional method throw RuntimeException"
            bookService.noRollbackForMethod()

        then:"The transaction wasn't rolled back"
            thrown(TestTransactionRuntimeException)
            transactionManager.transactionRolledBack == false

        when:"A transactionManager is set"
            transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        and:"A transactional method throw RuntimeException"
            bookService.noRollbackForClassNameMethod()

        then:"The transaction wasn't rolled back"
            thrown(TestTransactionRuntimeException)
            transactionManager.transactionRolledBack == false

        when:"A transactionManager is set"
            transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        and:"A transactional method throw Exception"
            bookService.rollbackForMethod()

        then:"The transaction was rolled back"
            thrown(TestTransactionException)
            transactionManager.transactionRolledBack == true

        when:"A transactionManager is set"
            transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager

        and:"A transactional method throw Exception"
            bookService.rollbackForClassNameMethod()

        then:"The transaction was rolled back"
            thrown(TestTransactionException)
            transactionManager.transactionRolledBack == true
    }

    TestTransactionManager getPlatformTransactionManager() {
        def dataSource =  new DriverManagerDataSource("jdbc:h2:mem:${TransactionalTransformSpec.name};MVCC=TRUE;LOCK_TIMEOUT=10000", "sa", "")
        
        // this may not be necessary... 
        dataSource.driverClassName = "org.h2.Driver"

        return new TestTransactionManager(dataSource) {}
    }

    @Issue("GRAILS-10748")
    void "transactional shouldn't be applied to bean initialization methods"() {
        when:
            def application = new DefaultGrailsApplication()
            def bb = new BeanBuilder()
            bb.beans {
                commonAnnotationBeanPostProcessor(CommonAnnotationBeanPostProcessor)
                testService(TransactionalTransformSpecService) { bean ->
                    bean.autowire = true
                    bean.lazyInit = false
                }
                transactionManager(MethodInvokingFactoryBean) {
                    targetObject = this
                    targetMethod = 'getPlatformTransactionManager'
                }
            }
            def applicationContext = bb.createApplicationContext()
            def bean = applicationContext.getBean('testService')
            bean.name = 'Grails'
        then:
            applicationContext.transactionManager != null
            bean.transactionManager != null
            bean.process() != null
            bean.isActualTransactionActive() == false
            bean.name == 'Grails'
            bean.isActive() == false
    }
    
    @Issue(['GRAILS-11145', 'GRAILS-11134'])
    void "Test inheritRollbackOnly attribute"() {
        given:
            def bookService = new GroovyShell().evaluate('''
    import grails.transaction.*
    import grails.transaction.TransactionManagerAware
    import org.springframework.transaction.PlatformTransactionManager
    import org.springframework.transaction.TransactionStatus
    import org.springframework.transaction.annotation.Isolation
    import org.springframework.transaction.annotation.Propagation

    @Transactional
    class BookService {

        void updateBook() {
            doNestedUpdate()
        }

        void doNestedUpdate() {
            transactionStatus.setRollbackOnly()
        }
    }

    new BookService()
    ''')
            final transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager
        when:"A transactional method containing setRollbackOnly in nested transaction template is called"
            bookService.updateBook()
        then:"The test passes without UnexpectedRollbackException"
            1==1
    }
    
    @Issue(['GRAILS-11145', 'GRAILS-11134'])
    void "Test disabling inheritRollbackOnly"() {
        given:
            def bookService = new GroovyShell().evaluate('''
    import grails.transaction.*
    import grails.transaction.TransactionManagerAware
    import org.springframework.transaction.PlatformTransactionManager
    import org.springframework.transaction.TransactionStatus
    import org.springframework.transaction.annotation.Isolation
    import org.springframework.transaction.annotation.Propagation

    @Transactional(inheritRollbackOnly=false)
    class BookService {

        void updateBook() {
            doNestedUpdate()
        }

        void doNestedUpdate() {
            transactionStatus.setRollbackOnly()
        }

        void doRollback() {
            transactionStatus.setRollbackOnly()
        }
    }

    new BookService()
    ''')
            final transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager
        when:"A transactional method containing setRollbackOnly in nested transaction template is called"
            bookService.updateBook()
        then:"UnexpectedRollbackException is thrown"
            thrown UnexpectedRollbackException
        when:
             bookService.doRollback()
        then:"no exception should be thrown when there are no nested transactions"
             1==1
    }

    void "Test rollback transformation"() {
        given:
        def bookService = new GroovyShell().evaluate('''
    import grails.transaction.*
    import org.springframework.transaction.TransactionStatus

    @Rollback
    class BookService {


        TransactionStatus doRollback() {
            def status = transactionStatus
            return  transactionStatus
        }
    }

    new BookService()
    ''')
            final transactionManager = getPlatformTransactionManager()
            bookService.transactionManager = transactionManager
        when:"A method is called"
            TransactionStatus status = bookService.doRollback()
        then:"Then the transaction has been rolled back"
            status.isRollbackOnly()
    }

    void 'test CompileStatic on a method in a class marked with Transactional'() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
package demo

@grails.transaction.Transactional
class SomeClass {
    @groovy.transform.CompileStatic
    def someMethod() {
        int x = 'Jeff'.lastName()
    }
}
''')
        then:
        MultipleCompilationErrorsException ex = thrown()
        ex.message.contains 'Cannot find matching method java.lang.String#lastName()'

    }
}


@Transactional
class TransactionalTransformSpecService implements InitializingBean {
    String name   
    
    public TransactionStatus process() {
        return transactionStatus
    }
    
    @NotTransactional
    public boolean isActualTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive()
    }
    
    @PostConstruct
    public void init() {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        assert !TransactionSynchronizationManager.isActualTransactionActive()
    }
    
    public void setName(String name) {
        assert !TransactionSynchronizationManager.isActualTransactionActive()
        this.name = name
    }
    
    public String getName() {
        assert !TransactionSynchronizationManager.isActualTransactionActive()
        name
    }
    
    public boolean isActive() {
        TransactionSynchronizationManager.isActualTransactionActive()
    }
}


class TestTransactionManager extends DataSourceTransactionManager {
    boolean transactionStarted = false
    boolean transactionRolledBack = false

    TestTransactionManager(DataSource dataSource) {
        super(dataSource)
    }

    @Override
    protected Object doGetTransaction() {
        transactionStarted = true
        return super.doGetTransaction()
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        transactionRolledBack = true
        super.doRollback(status)
    }
}

class TestTransactionRuntimeException extends RuntimeException {
}

class TestTransactionException extends Exception {
}
