package org.codehaus.groovy.grails.orm.support

import grails.spring.BeanBuilder
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 28, 2008
 */
class TransactionManagerPostProcessorTests extends GroovyTestCase{

    void testTransactionManagerPostProccessor() {
        def bb = new BeanBuilder()

        bb.beans {
            myBean(MyBean) { bean ->
                bean.lazyInit = true                
            }
            dataSource(DriverManagerDataSource) {
                url = "jdbc:hsqldb:mem:tmpptDB"
                driverClassName = "org.hsqldb.jdbcDriver"
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

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        tm = transactionManager
    }

}