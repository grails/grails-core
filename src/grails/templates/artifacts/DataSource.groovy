dataSource {
    pooling = false                          
    driverClassName = "org.hsqldb.jdbcDriver"	
    username = "sa"
    password = ""				
}
environments {
    development {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'createeate-drop','update'
            url = "jdbc:hsqldb:mem:devDB"
        }
    }   
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:hsqldb:mem:testDb"
        }
    }   
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:hsqldb:file:prodDb;shutdown=true"
        }
    }
}