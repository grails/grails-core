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
import org.codehaus.groovy.grails.orm.hibernate.cfg.ColumnConfig

class HibernateMappingBuilderTests extends GroovyTestCase {

    void testCascades() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            columns {
                things cascade:'save-update'
            }
        }

        assertEquals 'save-update',mapping.getColumn('things').cascade

    }

    void testCascadesWithColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
             things cascade:'save-update'
        }
        assertEquals 'save-update',mapping.getColumn('things').cascade
    }

    void testJoinTableMapping() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            columns {
                things joinTable:true
            }
        }

        assert mapping.getColumn('things')?.joinTable

        mapping = builder.evaluate {
            columns {
                things joinTable:'foo'
            }
        }

        ColumnConfig column = mapping.getColumn('things')
        assert column?.joinTable
        assertEquals "foo", column.joinTable.name

        mapping = builder.evaluate {
            columns {
                things joinTable:[name:'foo', key:'foo_id', column:'bar_id']
            }
        }

        column = mapping.getColumn('things')
        assert column?.joinTable
        assertEquals "foo", column.joinTable.name
        assertEquals "foo_id", column.joinTable.key
        assertEquals "bar_id", column.joinTable.column
    }

    void testJoinTableMappingWithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            things joinTable:true
        }

        assert mapping.getColumn('things')?.joinTable

        mapping = builder.evaluate {
            things joinTable:'foo'
        }

        ColumnConfig column = mapping.getColumn('things')
        assert column?.joinTable
        assertEquals "foo", column.joinTable.name

        mapping = builder.evaluate {
            things joinTable:[name:'foo', key:'foo_id', column:'bar_id']
        }

        column = mapping.getColumn('things')
        assert column?.joinTable
        assertEquals "foo", column.joinTable.name
        assertEquals "foo_id", column.joinTable.key
        assertEquals "bar_id", column.joinTable.column
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

        def cc = mapping.getColumn('firstName')
        assertEquals 'read-only', cc.cache.usage
        assertEquals 'non-lazy', cc.cache.include
    }

    void testCustomAssociationCachingConfig1WithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            firstName cache:[usage:'read-only', include:'non-lazy']
        }

        def cc = mapping.getColumn('firstName')
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

        def cc = mapping.getColumn('firstName')
        assertEquals 'read-only', cc.cache.usage
        
    }

    void testCustomAssociationCachingConfig2WithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            firstName cache:'read-only'
        }

        def cc = mapping.getColumn('firstName')
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

        def cc = mapping.getColumn('firstName')
        assertEquals 'read-write', cc.cache.usage
        assertEquals 'all', cc.cache.include
    }

    void testAssociationCachingConfigWithoutColumnsBlock() {
        def builder = new HibernateMappingBuilder("Foo")
        def mapping = builder.evaluate {
            table 'myTable'
            firstName cache:true
        }

        def cc = mapping.getColumn('firstName')
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
         assertEquals 'foo_id', mapping.getColumn("id").column
         assertEquals Integer, mapping.getColumn("id").type
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


        assertEquals "First_Name",mapping.getColumn('firstName').column
        assertEquals "Last_Name",mapping.getColumn('lastName').column
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

        
        assertEquals "First_Name",mapping.getColumn('firstName').column
        assertEquals "Last_Name",mapping.getColumn('lastName').column
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
                            index:'foo'
                            
                 lastName column:'Last_Name'
             }
         }


        assertEquals "First_Name",mapping.columns.firstName.column
        assertEquals true,mapping.columns.firstName.lazy
        assertEquals true,mapping.columns.firstName.unique
        assertEquals java.sql.Clob,mapping.columns.firstName.type
        assertEquals 255,mapping.columns.firstName.length
        assertEquals 'foo',mapping.columns.firstName.index
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
                        index:'foo'

             lastName column:'Last_Name'
         }


        assertEquals "First_Name",mapping.columns.firstName.column
        assertEquals true,mapping.columns.firstName.lazy
        assertEquals true,mapping.columns.firstName.unique
        assertEquals java.sql.Clob,mapping.columns.firstName.type
        assertEquals 255,mapping.columns.firstName.length
        assertEquals 'foo',mapping.columns.firstName.index
        assertEquals "Last_Name",mapping.columns.lastName.column

     }
}