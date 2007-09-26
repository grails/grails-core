/**
 * Tests that the Hibernate mapping DSL constructs a valid Mapping object

 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 26, 2007
 * Time: 2:29:54 PM
 * 
 */
package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.CompositeIdentity
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateMappingBuilder

class HibernateMappingBuilderTests extends GroovyTestCase {

     void testEvaluateTableName() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
        }

        assertEquals 'myTable', mapping.tableName
     }

    void testDefaultCacheStrategy() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            cache true
        }

        assertEquals 'read-write', mapping.cache.usage
        assertEquals 'all', mapping.cache.include

    }


     void testCustomCacheStrategy() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             cache usage:'read-only', include:'non-lazy'
         }

         assertEquals 'read-only', mapping.cache.usage
         assertEquals 'non-lazy', mapping.cache.include

     }

     void testInvalidCacheValues() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             cache usage:'rubbish', include:'more-rubbish'
         }

         // should be ignored and logged to console
         assertEquals 'read-write', mapping.cache.usage
         assertEquals 'all', mapping.cache.include

     }

     void testEvaluateVersioning() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            version false
        }

        assertEquals 'myTable', mapping.tableName
        assertEquals false, mapping.versioned
     }

     void testDefaultIdStrategy() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
         }

         assertEquals Long, mapping.identity.type
         assertEquals 'id', mapping.identity.column
         assertEquals 'native', mapping.identity.generator
     }

     void testHiloIdStrategy() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             id generator:'hilo', params:[table:'hi_value',column:'next_value',max_lo:100]
         }

         assertEquals Long, mapping.identity.type
         assertEquals 'id', mapping.identity.column
         assertEquals 'hilo', mapping.identity.generator
         assertEquals 'hi_value', mapping.identity.params.table

     }

     void testCompositeIdStrategy() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             id composite:['one','two']
         }

         assert mapping.identity instanceof CompositeIdentity
         assertEquals "one", mapping.identity.propertyNames[0]
         assertEquals "two", mapping.identity.propertyNames[1]
     }

     void testSimpleColumnMappings() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             columns {
                 firstName name:'First_Name'
                 lastName name:'Last_Name'
             }
         }

        
        assertEquals "First_Name",mapping.getColumn('firstName').name
        assertEquals "Last_Name",mapping.getColumn('lastName').name
     }

     void testComplexColumnMappings() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             columns {
                 firstName  name:'First_Name',
                            lazy:true,
                            unique:true,
                            type: java.sql.Clob,
                            length:255,
                            index:'foo'
                            
                 lastName name:'Last_Name'
             }
         }


        assertEquals "First_Name",mapping.columns.firstName.name
        assertEquals true,mapping.columns.firstName.lazy
        assertEquals true,mapping.columns.firstName.unique
        assertEquals java.sql.Clob,mapping.columns.firstName.type
        assertEquals 255,mapping.columns.firstName.length
        assertEquals 'foo',mapping.columns.firstName.index
        assertEquals "Last_Name",mapping.columns.lastName.name

     }
}