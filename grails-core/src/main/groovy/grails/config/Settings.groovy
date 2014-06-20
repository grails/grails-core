package grails.config

/**
 * Constants for names of settings in Grails
 *
 * @author Graeme Rocher
 * @since 3.0
 */
interface Settings {
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
     * Whether to include the jsessionid in the rendered links
     **/
    String GRAILS_VIEWS_ENABLE_JSESSIONID = "grails.views.enable.jsessionid";
    String GSP_DISABLE_CACHING_RESOURCES = "grails.gsp.disable.caching.resources";
    String GSP_ENABLE_RELOAD = "grails.gsp.enable.reload";
    String CLASS_RESOURCE_PATTERN = "/**/*.class"
}
