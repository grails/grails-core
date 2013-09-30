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

import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.support.DefaultTransactionStatus
import spock.lang.Issue
import spock.lang.Specification
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware

import javax.sql.DataSource

/**
 */
class TransactionalTransformSpec extends Specification {

    @Issue('GRAILS-10402')
    void "Test @Transactional annotation with inheritance"() {
        when:"A new instance of a class with a @Transactional method is created that subclasses another transactional class"
            def bookService = new GroovyShell().evaluate('''
    import grails.transaction.*
    import org.codehaus.groovy.grails.orm.support.TransactionManagerAware
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


    void "Test that a @Transactional annotation on a class results in a call to TransactionTemplate"() {
        when:"A new instance of a class with a @Transactional method is created"
        def bookService = new GroovyShell().evaluate('''
import grails.transaction.*
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware
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
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware
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
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware
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
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware
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
        def dataSource = new DriverManagerDataSource("org.h2.Driver", "jdbc:h2:mem:${TransactionalTransformSpec.name};MVCC=TRUE;LOCK_TIMEOUT=10000", "sa", "")

        return new TestTransactionManager(dataSource) {}
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
