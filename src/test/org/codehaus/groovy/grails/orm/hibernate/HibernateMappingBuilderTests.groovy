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
import org.codehaus.groovy.grails.orm.hibernate.cfg.PropertyConfig
import org.codehaus.groovy.grails.orm.hibernate.cfg.MyUserType
import org.hibernate.FetchMode

class HibernateMappingBuilderTests extends GroovyTestCase {

//    void testWildcardApplyToAllProperties() {
//        def builder = new HibernateMappingBuilder("Foo")
//        def mapping = builder.evaluate {
//            '*'(column:"foo")
//            '*-1'(column:"foo")
//            '1-1'(column:"foo")
//            '1-*'(column:"foo")
//            '*-*'(column:"foo")
//            one cache:true
//            two ignoreNoteFound:false
//        }
//
//    }

    void testIgnoreNotFound() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            foos ignoreNotFound:true
        }

        assertTrue "ignore not found should have been true",mapping.getPropertyConfig("foos").ignoreNotFound


        mapping = builder.evaluate {
            foos ignoreNotFound:false
        }
        assertFalse "ignore not found should have been false",mapping.getPropertyConfig("foos").ignoreNotFound

        mapping = builder.evaluate { // default
            foos lazy:false
        }
        assertFalse "ignore not found should have been false",mapping.getPropertyConfig("foos").ignoreNotFound

    }

    void testNaturalId() {

        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            id natural: 'one'
        }

        assertEquals( ['one'], mapping.identity.natural.propertyNames)

        mapping = builder.evaluate {
            id natural: ['one','two']
        }

        assertEquals( ['one','two'], mapping.identity.natural.propertyNames)

        mapping = builder.evaluate {
            id natural: [properties:['one','two'], mutable:true]
        }

        assertEquals( ['one','two'], mapping.identity.natural.propertyNames)
        assertTrue mapping.identity.natural.mutable
    }

    void testDiscriminator() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            discriminator 'one'
        }

        assertEquals "one", mapping.discriminator
        assertNull mapping.discriminatorColumn

        mapping = builder.evaluate {
            discriminator value:'one', column:'type'
        }

        assertEquals "one", mapping.discriminator
        assertEquals "type", mapping.discriminatorColumn.name

        mapping = builder.evaluate {
            discriminator value:'one', column:[name:'type', sqlType:'integer']
        }

        assertEquals "one", mapping.discriminator
        assertEquals "type", mapping.discriminatorColumn.name
        assertEquals "integer", mapping.discriminatorColumn.sqlType

    }

    void testAutoImport() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate { }

        assertTrue "default auto-import should be true", mapping.autoImport

        mapping = builder.evaluate {
            autoImport false
        }

        assertFalse "auto-import should be false", mapping.autoImport
    }

    void testTableWithCatalogueAndSchema() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table name:"table", catalog:"CRM", schema:"dbo"
        }

        assertEquals 'table',mapping.table.name
        assertEquals 'dbo',mapping.table.schema
        assertEquals 'CRM',mapping.table.catalog

    }

    void testIndexColumn() {

        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            things indexColumn:[name:"chapter_number", type:"string", length:3]
        }

        PropertyConfig pc = mapping.getPropertyConfig("things")
        assertEquals "chapter_number",pc.indexColumn.column
        assertEquals "string",pc.indexColumn.type
        assertEquals 3, pc.indexColumn.length
    }

    void testDynamicUpdate() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            dynamicUpdate true
            dynamicInsert true
        }

        assertTrue mapping.dynamicUpdate
        assertTrue mapping.dynamicInsert

        mapping = builder.evaluate {

        }

        assertFalse mapping.dynamicUpdate
        assertFalse mapping.dynamicInsert

    }
    void testBatchSizeConfig() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            batchSize 10

            things batchSize:15
        }

        assertEquals 10, mapping.batchSize
        assertEquals 15,mapping.getPropertyConfig('things').batchSize
    }

    void testChangeVersionColumn() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            version 'v_number'
        }

        assertEquals 'v_number', mapping.getPropertyConfig("version").column
    }

    void testClassSortOrder() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            sort "name"
            order "desc"
            columns {
                things sort:'name'
            }
        }


        assertEquals "name", mapping.sort
        assertEquals "desc", mapping.order
        assertEquals 'name',mapping.getPropertyConfig('things').sort


        mapping = builder.evaluate {
            sort name:'desc'

            columns {
                things sort:'name'
            }
        }

        assertEquals "name", mapping.sort
        assertEquals "desc", mapping.order
        assertEquals 'name',mapping.getPropertyConfig('things').sort


    }

    void testAssociationSortOrder() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            columns {
                things sort:'name'
            }
        }

        assertEquals 'name',mapping.getPropertyConfig('things').sort

    }

    void testLazy() {
      def builder = new HibernateMappingBuilder("Foo")
      def mapping = builder.evaluate {
          columns {
              things cascade:'save-update'
          }
      }

      assertTrue "should have been lazy",mapping.getPropertyConfig('things').lazy

      mapping = builder.evaluate {
          columns {
              things lazy:false
          }
      }

      assertFalse "shouldn't have been lazy", mapping.getPropertyConfig('things').lazy
    }

    void testCascades() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            columns {
                things cascade:'save-update'
            }
        }

        assertEquals 'save-update',mapping.getPropertyConfig('things').cascade

    }


    void testFetchModes() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            columns {
                things fetch:'join'
                others fetch:'select'
                mores column:'yuck'
            }
        }

        assertEquals FetchMode.JOIN,mapping.getPropertyConfig('things').fetch
        assertEquals FetchMode.SELECT,mapping.getPropertyConfig('others').fetch
        assertEquals FetchMode.DEFAULT,mapping.getPropertyConfig('mores').fetch

    }

    void testEnumType() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            columns {
                things column:'foo'
            }
        }

        assertEquals 'default',mapping.getPropertyConfig('things').enumType

        mapping = builder.evaluate {
            columns {
                things enumType:'ordinal'
            }
        }

        assertEquals 'ordinal',mapping.getPropertyConfig('things').enumType

    }

    void testCascadesWithColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
             things cascade:'save-update'
        }
        assertEquals 'save-update',mapping.getPropertyConfig('things').cascade
    }

    void testJoinTableMapping() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            columns {
                things joinTable:true
            }
        }

        assert mapping.getPropertyConfig('things')?.joinTable

        mapping = builder.evaluate {
            columns {
                things joinTable:'foo'
            }
        }

        PropertyConfig property = mapping.getPropertyConfig('things')
        assert property?.joinTable
        assertEquals "foo", property.joinTable.name

        mapping = builder.evaluate {
            columns {
                things joinTable:[name:'foo', key:'foo_id', column:'bar_id']
            }
        }

        property = mapping.getPropertyConfig('things')
        assert property?.joinTable
        assertEquals "foo", property.joinTable.name
        assertEquals "foo_id", property.joinTable.key.name
        assertEquals "bar_id", property.joinTable.column.name
    }

    void testJoinTableMappingWithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            things joinTable:true
        }

        assert mapping.getPropertyConfig('things')?.joinTable

        mapping = builder.evaluate {
            things joinTable:'foo'
        }

        PropertyConfig property = mapping.getPropertyConfig('things')
        assert property?.joinTable
        assertEquals "foo", property.joinTable.name

        mapping = builder.evaluate {
            things joinTable:[name:'foo', key:'foo_id', column:'bar_id']
        }

        property = mapping.getPropertyConfig('things')
        assert property?.joinTable
        assertEquals "foo", property.joinTable.name
        assertEquals "foo_id", property.joinTable.key.name
        assertEquals "bar_id", property.joinTable.column.name
    }


    void testCustomInheritanceStrategy() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            tablePerHierarchy false
        }

        assertEquals false, mapping.tablePerHierarchy

        mapping = builder.evaluate {
            table 'myTable'
            tablePerSubclass true
        }

        assertEquals false, mapping.tablePerHierarchy
    }

    void testAutoTimeStamp() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            autoTimestamp false
        }

        assertEquals false, mapping.autoTimestamp
    }

    void testCustomAssociationCachingConfig1() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            columns {
                firstName cache:[usage:'read-only', include:'non-lazy']
            }
        }

        def cc = mapping.getPropertyConfig('firstName')
        assertEquals 'read-only', cc.cache.usage
        assertEquals 'non-lazy', cc.cache.include
    }

    void testCustomAssociationCachingConfig1WithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            firstName cache:[usage:'read-only', include:'non-lazy']
        }

        def cc = mapping.getPropertyConfig('firstName')
        assertEquals 'read-only', cc.cache.usage
        assertEquals 'non-lazy', cc.cache.include
    }


    void testCustomAssociationCachingConfig2() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'

            columns {
                firstName cache:'read-only'
            }
        }

        def cc = mapping.getPropertyConfig('firstName')
        assertEquals 'read-only', cc.cache.usage

    }

    void testCustomAssociationCachingConfig2WithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            firstName cache:'read-only'
        }

        def cc = mapping.getPropertyConfig('firstName')
        assertEquals 'read-only', cc.cache.usage
    }


    void testAssociationCachingConfig() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'

            columns {
                firstName cache:true
            }
        }

        def cc = mapping.getPropertyConfig('firstName')
        assertEquals 'read-write', cc.cache.usage
        assertEquals 'all', cc.cache.include
    }

    void testAssociationCachingConfigWithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            firstName cache:true
        }

        def cc = mapping.getPropertyConfig('firstName')
        assertEquals 'read-write', cc.cache.usage
        assertEquals 'all', cc.cache.include
    }


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


     void testCustomCacheStrategy2() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             cache 'read-only'
         }

         assertEquals 'read-only', mapping.cache.usage
         assertEquals 'all', mapping.cache.include

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

     void testIdentityColumnMapping() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             id column:'foo_id', type:Integer
         }

         assertEquals Long, mapping.identity.type
         assertEquals 'foo_id', mapping.getPropertyConfig("id").column
         assertEquals Integer, mapping.getPropertyConfig("id").type
         assertEquals 'native', mapping.identity.generator

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
             id composite:['one','two'], compositeClass:HibernateMappingBuilder.class
         }

         assert mapping.identity instanceof CompositeIdentity
         assertEquals "one", mapping.identity.propertyNames[0]
         assertEquals "two", mapping.identity.propertyNames[1]
         assertEquals HibernateMappingBuilder, mapping.identity.compositeClass
     }

     void testSimpleColumnMappingsWithoutColumnsBlock() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             firstName column:'First_Name'
             lastName column:'Last_Name'
         }


        assertEquals "First_Name",mapping.getPropertyConfig('firstName').column
        assertEquals "Last_Name",mapping.getPropertyConfig('lastName').column
     }

     void testSimpleColumnMappings() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             columns {
                 firstName column:'First_Name'
                 lastName column:'Last_Name'
             }
         }


        assertEquals "First_Name",mapping.getPropertyConfig('firstName').column
        assertEquals "Last_Name",mapping.getPropertyConfig('lastName').column
     }


     void testComplexColumnMappings() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             columns {
                 firstName  column:'First_Name',
                            lazy:true,
                            unique:true,
                            type: java.sql.Clob,
                            length:255,
                            index:'foo',
                            sqlType: 'text'

                 lastName column:'Last_Name'
             }
         }


        assertEquals "First_Name",mapping.columns.firstName.column
        assertEquals true,mapping.columns.firstName.lazy
        assertEquals true,mapping.columns.firstName.unique
        assertEquals java.sql.Clob,mapping.columns.firstName.type
        assertEquals 255,mapping.columns.firstName.length
        assertEquals 'foo',mapping.columns.firstName.index
        assertEquals "text",mapping.columns.firstName.sqlType
        assertEquals "Last_Name",mapping.columns.lastName.column

     }

     void testComplexColumnMappingsWithoutColumnsBlock() {
         def builder = new HibernateMappingBuilder("Foo")
         def mapping = builder.evaluate {
             table 'myTable'
             version false
             firstName  column:'First_Name',
                        lazy:true,
                        unique:true,
                        type: java.sql.Clob,
                        length:255,
                        index:'foo',
                        sqlType: 'text'

             lastName column:'Last_Name'
         }


        assertEquals "First_Name",mapping.columns.firstName.column
        assertEquals true,mapping.columns.firstName.lazy
        assertEquals true,mapping.columns.firstName.unique
        assertEquals java.sql.Clob,mapping.columns.firstName.type
        assertEquals 255,mapping.columns.firstName.length
        assertEquals 'foo',mapping.columns.firstName.index
        assertEquals "text",mapping.columns.firstName.sqlType
        assertEquals "Last_Name",mapping.columns.lastName.column

     }

    void testPropertyWithMultipleColumns() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            amount type: MyUserType, {
                column name: "value"
                column name: "currency", sqlType: "char", length: 3
            }
        }

        assertEquals 2, mapping.columns.amount.columns.size()
        assertEquals "value", mapping.columns.amount.columns[0].name
        assertEquals "currency", mapping.columns.amount.columns[1].name
        assertEquals "char", mapping.columns.amount.columns[1].sqlType
        assertEquals 3, mapping.columns.amount.columns[1].length

        shouldFail { mapping.columns.amount.column }
        shouldFail { mapping.columns.amount.sqlType }
    }

    void testPropertyWithUserTypeAndNoParams() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            amount type: MyUserType
        }
		
        assertEquals MyUserType.class,mapping.getPropertyConfig('amount').type
        assertNull mapping.getPropertyConfig('amount').typeParams
    }
	
    void testPropertyWithUserTypeAndTypeParams() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            amount type: MyUserType, params : [ param1 : "amountParam1", param2 : 65 ]
            value type: MyUserType, params : [ param1 : "valueParam1", param2 : 21 ]
        }
		
        assertEquals MyUserType.class,mapping.getPropertyConfig('amount').type
        assertEquals "amountParam1",mapping.getPropertyConfig('amount').typeParams.param1
        assertEquals 65,mapping.getPropertyConfig('amount').typeParams.param2
        assertEquals MyUserType.class,mapping.getPropertyConfig('value').type
        assertEquals "valueParam1",mapping.getPropertyConfig('value').typeParams.param1
        assertEquals 21,mapping.getPropertyConfig('value').typeParams.param2
	}
}
