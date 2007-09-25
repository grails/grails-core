// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// log4j configuration
log4j {
    appender.stdout = "org.apache.log4j.ConsoleAppender"
    appender.'stdout.layout'="org.apache.log4j.PatternLayout"
    appender.'stdout.layout.ConversionPattern'='[%r] %c{2} %m%n'
    rootLogger="error,stdout"
    logger {
        grails="error,stdout"
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
        org {
            codehaus.groovy.grails=false
            springframework=false
            hibernate=false
        }
    }
}