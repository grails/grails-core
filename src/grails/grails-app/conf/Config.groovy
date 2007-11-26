// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.types = [ xml: ['text/xml', 'application/xml'],
                      text: 'text-plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      cvs: 'text/csv',
                      all: '*/*',
                      json: 'text/json',
                      html: ['text/html','application/xhtml+xml']
                    ]
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

// log4j configuration
log4j = '''
appender.stdout = org.apache.log4j.ConsoleAppender
appender.stdout.layout=org.apache.log4j.PatternLayout
appender.stdout.layout.ConversionPattern=[%r] %c{2} %m%n
rootLogger=error,stdout
logger.grails=error
org.codehaus.groovy.grails.web.servlet=error
org.codehaus.groovy.grails.web.errors=error
org.codehaus.groovy.grails.web.pages=error
org.codehaus.groovy.grails.web.sitemesh=error
org.codehaus.groovy.grails.web.mapping.filter=error
org.codehaus.groovy.grails.web.mapping=error
org.codehaus.groovy.grails.commons=info
org.codehaus.groovy.grails.plugins=error
org.codehaus.groovy.grails.orm.hibernate=error
org.springframework=error
org.hibernate=error
'''

