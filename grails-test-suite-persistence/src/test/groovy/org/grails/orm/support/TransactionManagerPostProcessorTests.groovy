package org.grails.orm.support

import grails.spring.BeanBuilder
import grails.transaction.TransactionManagerAware
import org.grails.transaction.TransactionManagerPostProcessor
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.PlatformTransactionManager

import static org.junit.jupiter.api.Assertions.assertNotNull

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TransactionManagerPostProcessorTests {

    @Test
    void testTransactionManagerPostProccessor() {
        def bb = new BeanBuilder()

        bb.beans {
            myBean(MyBean) { bean ->
                bean.lazyInit = true
            }
            dataSource(DriverManagerDataSource) {
                url = "jdbc:h2:mem:tmpptDB"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
            }
            transactionManager(DataSourceTransactionManager) {
                dataSource = dataSource
            }
            transactionManagerPostProcessor(TransactionManagerPostProcessor)
        }

        def ctx = bb.createApplicationContext()

        MyBean bean = ctx.getBean("myBean")
        assertNotNull bean
        assertNotNull bean.tm
    }
}

class MyBean implements TransactionManagerAware {

    PlatformTransactionManager tm

    void setTransactionManager(PlatformTransactionManager transactionManager) {
        tm = transactionManager
    }
}
