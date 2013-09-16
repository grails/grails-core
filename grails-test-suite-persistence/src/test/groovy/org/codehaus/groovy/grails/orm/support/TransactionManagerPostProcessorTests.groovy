package org.codehaus.groovy.grails.orm.support

import org.springframework.context.groovy.GroovyBeanDefinitionReader
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.PlatformTransactionManager

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TransactionManagerPostProcessorTests extends GroovyTestCase{

    void testTransactionManagerPostProccessor() {
        def bb = new GroovyBeanDefinitionReader()

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
        assert bean
        assert bean.tm
    }
}

class MyBean implements TransactionManagerAware {

    PlatformTransactionManager tm

    void setTransactionManager(PlatformTransactionManager transactionManager) {
        tm = transactionManager
    }
}
