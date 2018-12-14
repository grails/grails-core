package org.grails.plugins.web.mime

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties('grails.mime')
class MimeConfig {

    Map<String, List<String>> types
}
