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
import spock.lang.Specification
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware

import javax.sql.DataSource

/**
 */
class TransactionalTransformSpec extends Specification {

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


    TestTransactionManager getPlatformTransactionManager() {
        def dataSource = new DriverManagerDataSource("org.h2.Driver", "jdbc:h2:mem:${TransactionalTransformSpec.name};MVCC=TRUE;LOCK_TIMEOUT=10000", "sa", "")

        return new TestTransactionManager(dataSource) {}
    }
}
class TestTransactionManager extends DataSourceTransactionManager {
    boolean transactionStarted = false
    TestTransactionManager(DataSource dataSource) {
        super(dataSource)
    }

    @Override
    protected Object doGetTransaction() {
        transactionStarted = true
        return super.doGetTransaction()
    }
}



