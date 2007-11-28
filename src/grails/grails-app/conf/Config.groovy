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
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text-plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      cvs: 'text/csv',
                      all: '*/*',
                      json: 'text/json'
                    ]
// The default codec used to encode data with ${}
grails.views.default.codec="none" // none, html, base64

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

// log4j configuration
log4j = '''
appender.stdout = org.apache.log4j.ConsoleAppender
appender.stdout.layout=org.apache.log4j.PatternLayout
appender.stdout.layout.ConversionPattern=[%r] %c{2} %m%n
rootLogger=error,stdout

# This logger is for your own application artefact logs
# Artefacts are logged by their type and optionally class name i.e:
#
# log4j.logger.grails.app.controller.HelloController=debug, stdout
# ...will control the logs from just that controller
#
# log4j.logger.grails.app.domain=trace, stdout
# ...will control the logs for all domain classes
#
# At the time of writing, supported artefact type ids include:
# domain (aka Domain Class), service, dataSource,
# controller, tagLib, urlMappings, codec, bootstrap
#
# The default "info" level for all artefacts is set here
log4j.logger.grails.app=error

# This logger is for Grails' public APIs within the grails. package
logger.grails=error

# This logger is useful if you just want to see what Grails
# configures with Spring at runtime. Setting to debug will show
# each bean that is configured
log4j.logger.org.codehaus.groovy.grails.commons.spring=error

# This logger covers all of Grails' internals
# Enable to see whats going on underneath.
log4j.logger.org.codehaus.groovy.grails=error

# Enable this logger to log Hibernate output
# handy to see its database interaction activity
log4j.logger.org.hibernate=error

# Enable this logger to see what Spring does, occasionally useful
#log4j.logger.org.springframework=error

'''

