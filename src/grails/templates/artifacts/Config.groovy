// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]


// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// log4j configuration
log4j {
    appender.stdout = "org.apache.log4j.ConsoleAppender"
    appender.'stdout.layout'="org.apache.log4j.PatternLayout"
    appender.'stdout.layout.ConversionPattern'='[%r] %c{2} %m%n'
    appender.errors = "org.apache.log4j.FileAppender"
    appender.'errors.layout'="org.apache.log4j.PatternLayout"
    appender.'errors.layout.ConversionPattern'='[%r] %c{2} %m%n'
    appender.'errors.File'="stacktrace.log"
    rootLogger="error,stdout"
    logger {
        grails="error,stdout"
        StackTrace="error,errors"
        org {
            codehaus.groovy.grails.web.servlet="error,stdout"  //  controllers
            codehaus.groovy.grails.web.pages="error,stdout" //  GSP
            codehaus.groovy.grails.web.sitemesh="error,stdout" //  layouts
            codehaus.groovy.grails."web.mapping.filter"="error,stdout" // URL mapping
            codehaus.groovy.grails."web.mapping"="error,stdout" // URL mapping
            codehaus.groovy.grails.commons="info,stdout" // core / classloading
            codehaus.groovy.grails.plugins="error,stdout" // plugins
            codehaus.groovy.grails.orm.hibernate="error,stdout" // hibernate integration
            springframework="off,stdout"
            hibernate="off,stdout"
        }
    }

    additivity.'default' = false
    additivity {
        grails=false
        StackTrace=false
        org {
            codehaus.groovy.grails=false
            springframework=false
            hibernate=false
        }
    }
}