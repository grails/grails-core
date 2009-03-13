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
       gcl.parseClass '''
import org.hibernate.usertype.UserType

import org.hibernate.type.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import org.hibernate.Hibernate
import org.hibernate.HibernateException
import java.sql.Types

class WeightUserType implements UserType {

    private static final int[] SQL_TYPES = [ Types.INTEGER ]
    public int[] sqlTypes() {
        return SQL_TYPES
    }

    public Class returnedClass() {
        return Weight.class
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
          return true;
        } else if (x == null || y == null) {
          return false;
        } else {
          return x.equals(y);
        }
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode()
    }

  public Object nullSafeGet(ResultSet resultSet,  String[] names, Object owner) throws HibernateException, SQLException {
    Weight result = null;
    int pounds = resultSet.getInt(names[0])
    if (!resultSet.wasNull()) {
      result = new Weight(pounds)
    }
    return result;
  }

    public void nullSafeSet(PreparedStatement statement,  Object value, int index)   throws HibernateException, SQLException {
        if (value == null) {
            statement.setNull(index);
        } else {
            Integer pounds = value.pounds
            statement.setInt(index, pounds);
        }
    }

  public Object deepCopy(Object value) throws HibernateException {
    return value;
  }

  public boolean isMutable() {
    return false;
  }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    public Object assemble(Serializable state, Object owner) throws HibernateException {
        return state;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}

class Weight {
    Integer pounds
    Weight(Integer pounds)
    {
        this.pounds= pounds
    }
}
'''
        
       gcl.parseClass '''
class UserTypeMappingTestsPerson {
    Long id
    Long version 
    String name
    Weight weight

    static constraints = {
        name(unique: true)
        weight(nullable: true)
    }

    static mapping = {
        columns {
            weight( type:WeightUserType)
        }
    }

}

'''
    }


    void testCustomUserType() {
        def personClass = ga.getDomainClass("UserTypeMappingTestsPerson").clazz
        def weightClass = ga.classLoader.loadClass("Weight")

        def person = personClass.newInstance(name:"Fred", weight:weightClass.newInstance(200))

        person.save(flush:true)
        session.clear()

        person = personClass.get(1)

        assert person
        assert person.weight
        assertEquals 200, person.weight.pounds
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