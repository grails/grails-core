package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource

/**
* @author Graeme Rocher
*/
class UserTypeMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
import org.hibernate.type.*
class UserTypeMappingTest
{
  Long id
  Long version

  Boolean active

  static mapping = {
    table 'type_test'
    columns {
      active (column: 'active', type: YesNoType)
    }
  }
}


'''
    }


    void testUserTypeMapping() {

        def clz = ga.getDomainClass("UserTypeMappingTest").clazz


        assert clz.newInstance(active:true).save(flush:true)

        DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from type_test")
             def result = statement.executeQuery()
             assert result.next()
             def value = result.getString('active')

             assertEquals "Y", value

         } finally {
             con.close()
         }
    }


}