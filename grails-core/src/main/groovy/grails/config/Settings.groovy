/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.config

/**
 * Constants for names of settings in Grails
 *
 * @author Graeme Rocher
 * @since 3.0
 */
interface Settings {
    /**
     *  The active Grails profile
     */
    String PROFILE = "grails.profile"
    /**
     *  Packages to scan for Spring beans
     */
    String SPRING_BEAN_PACKAGES = "grails.spring.bean.packages"
    /**
     * Whether to disable AspectJ explicitly
     */
    String SPRING_DISABLE_ASPECTJ = "grails.spring.disable.aspectj.autoweaving"
    /**
     * The prefix to use for property placeholders
     */
    String SPRING_PLACEHOLDER_PREFIX = "grails.spring.placeholder.prefix"

    /**
     * Whether to enable Spring proxy based transaction management. Since {@code @Transactional} uses an AST transform, this makes Spring proxy based transaction management redundant.
     * However, if Spring proxies are prefer
     */
    String SPRING_TRANSACTION_MANAGEMENT = "grails.spring.transactionManagement.proxies"

    /**
     * Which plugins to include in the plugin manager
     */
    String PLUGIN_INCLUDES = "grails.plugin.includes"
    /**
     * Which plugins to exclude from the plugin manager
     */
    String PLUGIN_EXCLUDES = "grails.plugin.excludes"

    /**
     * Whether to include the jsessionid in the rendered links
     **/
    String GRAILS_VIEWS_ENABLE_JSESSIONID = "grails.views.enable.jsessionid"

    String VIEWS_FILTERING_CODEC_FOR_CONTENT_TYPE = "grails.views.filteringCodecForContentType"

    /**
     * Whether to disable caching of resources in GSP
     */
    String GSP_DISABLE_CACHING_RESOURCES = "grails.gsp.disable.caching.resources"
    /**
     * Whether to enable GSP reload in production
     */
    String GSP_ENABLE_RELOAD = "grails.gsp.enable.reload"

    /**
     * Thew views directory for GSP
     */
    String GSP_VIEWS_DIR = "grails.gsp.view.dir"

    /**
     * The encoding to use for GSP views, defaults to UTF-8
     */
    String GSP_VIEW_ENCODING = "grails.views.gsp.encoding"

    /**
     * Pattern to use for class scanning
     */
    String CLASS_RESOURCE_PATTERN = "/**/*.class"

    /**
     * The default configured constraints for the application
     */
    String GORM_DEFAULT_CONSTRAINTS = 'grails.gorm.default.constraints'

    /**
     * Whether to autowire instances
     */
    String GORM_AUTOWIRE_INSTANCES = "grails.gorm.autowire"

    /**
     * Whether to translate GORM events into reactor events
     */
    String GORM_REACTOR_EVENTS = "grails.gorm.reactor.events"
    /**
     * The configured mime types
     */
    String MIME_TYPES = 'grails.mime.types'
    /**
     * Whether to use the accept header for content negotiation
     */
    String MIME_USE_ACCEPT_HEADER = 'grails.mime.use.accept.header'

    /**
     * Which user agents should have accept header processing disabled
     */
    String MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS = 'grails.mime.disable.accept.header.userAgents'

    /**
     * XHR requests will ignore MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS by default, enable to override default
     */
    String MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS_XHR = 'grails.mime.disable.accept.header.userAgentsXhr'

    /**
     * The default scope for controllers
     */
    String CONTROLLERS_DEFAULT_SCOPE = "grails.controllers.defaultScope"

    /**
     * The upload directory for controllers, defaults to java.tmp.dir
     */
    String CONTROLLERS_UPLOAD_LOCATION = "grails.controllers.upload.location"

    /**
     * The maximum file size
     */
    String CONTROLLERS_UPLOAD_MAX_FILE_SIZE = "grails.controllers.upload.maxFileSize"

    /**
     * The maximum request size
     */
    String CONTROLLERS_UPLOAD_MAX_REQUEST_SIZE = "grails.controllers.upload.maxRequestSize"

    /**
     * The file size threshold
     */
    String CONTROLLERS_UPLOAD_FILE_SIZE_THRESHOLD = "grails.controllers.upload.fileSizeThreshold"

    /**
     * The encoding to use for filters, default to UTF-8
     */
    String FILTER_ENCODING = 'grails.filter.encoding'

    /**
     * The encoding to use for filters, default to UTF-8
     */
    String FILTER_FORCE_ENCODING = 'grails.filter.forceEncoding'

    /**
     * Whether the H2 dbconsole is enabled or not
     */
    String DBCONSOLE_ENABLED = 'grails.dbconsole.enabled'

    /**
     * The converter to use for creating URL tokens in URL mapping. Defaults to camel case.
     */
    String WEB_URL_CONVERTER = "grails.web.url.converter"

    /**
     * Whether to cache links generated by the link generator
     */
    String WEB_LINK_GENERATOR_USE_CACHE = "grails.web.linkGenerator.useCache"

    /**
     * The path to the Grails servlet. Defaults to '/'
     */
    String WEB_SERVLET_PATH = "grails.web.servlet.path"

    /**
     * The URL of the server
     */
    String SERVER_URL = 'grails.serverURL'

    /**
     * The suffix used during scaffolding for the domain
     */
    String SCAFFOLDING_DOMAIN_SUFFIX = 'grails.scaffolding.templates.domainSuffix'

    /**
     * The amount of time to cache static resource requests
     */
    String RESOURCES_CACHE_PERIOD = 'grails.resources.cachePeriod'

    /**
     * Whether serving static HTML pages from src/main/resources/public is enabled
     */
    String RESOURCES_ENABLED = 'grails.resources.enabled'

    /**
     * The path pattern to serve static resources under
     */
    String RESOURCES_PATTERN = 'grails.resources.pattern'

    /**
     * The default pattern for static resources
     */
    String DEFAULT_RESOURCE_PATTERN = '/static/**'

    /**
     * The default servlet path
     */
    String DEFAULT_WEB_SERVLET_PATH = '/*'

    /**
     * The default servlet path
     */
    String DEFAULT_TOMCAT_SERVLET_PATH = '/'

    /**
     * The default encoding
     */
    String DEFAULT_ENCODING = System.getProperty('file.encoding',"UTF-8")

    /**
     * Whether to log request parameters in the console
     */
    String SETTING_LOG_REQUEST_PARAMETERS = "grails.exceptionresolver.logRequestParameters"
    /**
     * The parameters to exclude from logging
     */
    String SETTING_EXCEPTION_RESOLVER_PARAM_EXCLUDES = "grails.exceptionresolver.params.exclude"
    /**
     * The class to use for stacktrace filtering. Should be an instanceof {@link org.grails.exceptions.reporting.StackTraceFilterer}
     */
    String SETTING_LOGGING_STACKTRACE_FILTER_CLASS = "grails.logging.stackTraceFiltererClass"
    /**
     * Whether to use the legacy JSON builder
     */
    String SETTING_LEGACY_JSON_BUILDER = "grails.json.legacy.builder"
    /**
     * Whether to execute Bootstrap classes
     */
    String SETTING_SKIP_BOOTSTRAP = "grails.bootstrap.skip"
    /**
     * Whether to load cors configuration via a filter (true) or interceptor(false)
     */
    String SETTING_CORS_FILTER = "grails.cors.filter"

    String TRIM_STRINGS = 'grails.databinding.trimStrings'

    String CONVERT_EMPTY_STRINGS_TO_NULL = 'grails.databinding.convertEmptyStringsToNull'

    String AUTO_GROW_COLLECTION_LIMIT = 'grails.databinding.autoGrowCollectionLimit'

    String DATE_FORMATS = 'grails.databinding.dateFormats'

    String DATE_LENIENT_PARSING = 'grails.databinding.dateParsingLenient'

    String I18N_CACHE_SECONDS = 'grails.i18n.cache.seconds'

    String I18N_FILE_CACHE_SECONDS = 'grails.i18n.filecache.seconds'
}
