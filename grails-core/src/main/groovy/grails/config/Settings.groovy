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
    String SPRING_PLACEHOLDER_PREFIX = "grails.spring.placeholder.prefix";

    /**
     * Which plugins to include in the plugin manager
     */
    String PLUGIN_INCLUDES = "grails.plugin.includes";
    /**
     * Which plugins to exclude from the plugin manager
     */
    String PLUGIN_EXCLUDES = "grails.plugin.excludes";


    /**
     * Whether to include the jsessionid in the rendered links
     **/
    String GRAILS_VIEWS_ENABLE_JSESSIONID = "grails.views.enable.jsessionid";

    String VIEWS_FILTERING_CODEC_FOR_CONTENT_TYPE = "grails.views.filteringCodecForContentType"

    String GSP_DISABLE_CACHING_RESOURCES = "grails.gsp.disable.caching.resources";
    String GSP_ENABLE_RELOAD = "grails.gsp.enable.reload";
    String CLASS_RESOURCE_PATTERN = "/**/*.class"

    /**
     * The default configured constraints for the application
     */
    String GORM_DEFAULT_CONSTRAINTS = 'grails.gorm.default.constraints'
    /**
     * The configured mime types
     */
    String MIME_TYPES = 'grails.mime.types'
    /**
     * Whether to use the accept header for content negotiation
     */
    String MIME_USE_ACCEPT_HEADER = 'grails.mime.use.accept.header'

    String MIME_DISABLE_ACCEPT_HEADER_FOR_USER_AGENTS = 'grails.mime.disable.accept.header.userAgents'


}
