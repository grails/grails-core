grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"

grails.dependency.resolution = {
    // log level of Ivy resolver
    log "info"
    repositories {
        grailsHome()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/
    }
    dependencies {

        // dependencies needed by the Grails build system
         build "org.tmatesoft.svnkit:svnkit:1.2.0",
 			  "org.apache.ant:ant:1.7.1",
 			  "org.apache.ant:ant-launcher:1.7.1",
              "org.apache.ant:ant-junit:1.7.1",
              "org.apache.ant:ant-nodeps:1.7.1",
              "org.apache.ant:ant-trax:1.7.1",
              "radeox:radeox:1.0-b2",
              "apache-tomcat:jasper-compiler:5.5.15",
              "jline:jline:0.9.91",
              "xalan:serializer:2.7.1",
              "org.grails:grails-scripts:$grailsVersion",
              "org.grails:grails-core:$grailsVersion",
              "org.grails:grails-resources:$grailsVersion",
              "org.grails:grails-web:$grailsVersion",
              "org.sl4j:slf4j-log4j12:1.5.6"

        // dependencies needed during development, but not for deployment
        provided "javax.servlet:servlet-api:2.5",
                 "javax.servlet:jsp-api:2.1",
                 "javax.servlet:jstl:1.1.2"

        // dependencies needed for compilation
        compile "aopalliance:aopalliance:1.0",
                "commons-validator:commons-validator:1.3.1",
                "commons-el:commons-el:1.0",
                "commons-beanutils:commons-beanutils:1.8.0",
                "commons-collections:commons-collections:3.2.1",
                "commons-io:commons-io:1.4",
                "commons-lang:commons-lang:2.4",
                "javax.transaction:jta:1.1",
                "opensymphony:sitemesh:2.4",
                "org.grails:grails-core:$grailsVersion",                
                "org.grails:grails-crud:$grailsVersion",
                "org.grails:grails-docs:$grailsVersion",
                "org.grails:grails-gorm:$grailsVersion",
                "org.grails:grails-resources:$grailsVersion",
                "org.grails:grails-spring:$grailsVersion",
                "org.grails:grails-web:$grailsVersion",
                "org.springframework:org.springframework.core:3.0.0.M4",
                "org.springframework:org.springframework.aop:3.0.0.M4",
                "org.springframework:org.springframework.aspects:3.0.0.M4",
                "org.springframework:org.springframework.asm:3.0.0.M4",
                "org.springframework:org.springframework.beans:3.0.0.M4",
                "org.springframework:org.springframework.context:3.0.0.M4",
                "org.springframework:org.springframework.context.support:3.0.0.M4",
                "org.springframework:org.springframework.expression:3.0.0.M4",
                "org.springframework:org.springframework.instrument:3.0.0.M4",
                "org.springframework:org.springframework.instrument.classloading:3.0.0.M4",
                "org.springframework:org.springframework.jdbc:3.0.0.M4",
                "org.springframework:org.springframework.jms:3.0.0.M4",
                "org.springframework:org.springframework.orm:3.0.0.M4",
                "org.springframework:org.springframework.oxm:3.0.0.M4",                
                "org.springframework:org.springframework.transaction:3.0.0.M4",
                "org.springframework:org.springframework.web:3.0.0.M4",
                "org.springframework:org.springframework.web.servlet:3.0.0.M4",
                [transitive:false]

        // dependencies needed for running tests
        test "junit:junit:3.8.2",
             "org.grails:grails-test:$grailsVersion",
             "org.springframework:org.springframework.integration-tests:3.0.0.M4",
             "org.springframework:org.springframework.test:3.0.0.M4"

        // dependencies needed at runtime only
        runtime "aspectj:aspectjweaver:1.6.2",
                "aspectj:aspectjrt:1.6.2",
                "cglib:cglib-nodep:2.1_3",
                "commons-fileupload:commons-fileupload:1.2.1",
                "oro:oro:2.0.8"

        // data source
        runtime "commons-dbcp:commons-dbcp:1.2.2",
                "commons-pool:commons-pool:1.5.2",
                "hsqldb:hsqldb:1.8.0.10"

        // caching
        runtime ("net.sf.ehcache:ehcache:1.6.1",
                 "opensymphony:oscache:2.4.1") {
            excludes 'jms', 'commons-logging', 'servlet-api'
        }

        // logging
        runtime  "log4j:log4j:1.2.15",
                 "org.slf4j:jcl-over-slf4j:1.5.6",
                 "org.slf4j:jul-to-slf4j:1.5.6",
                 "org.slf4j:slf4j-api:1.5.6",
                 "org.sl4j:slf4j-log4j12:1.5.6"

        // JSP support
        runtime "apache-taglibs:standard:1.1.2",
                "xpp3:xpp3_min:1.1.3.4.O"


        // override per plugin dependencies
//        plugin("hibernate") {
//            runtime "org.hibernate:hibernate-core:3.2.6"
//        }
    }
}

