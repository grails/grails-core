package grails.rest.render.errors

import grails.converters.JSON
import grails.rest.render.ContainerRenderer
import grails.rest.render.RenderContext
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

/**
 * @author Graeme Rocher
 */
@CompileStatic
class VndErrorJsonRenderer implements ContainerRenderer<Errors, Object> {
    public static final String CONTENT_TYPE = "application/vnd.error+json"

    @Autowired
    MessageSource messageSource

    @Autowired
    LinkGenerator linkGenerator

    @Override
    Class<Errors> getTargetType() {
        Errors
    }

    @Override
    MimeType[] getMimeTypes() {
        return [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }

    @Override
    void render(Errors object, RenderContext context) {
        if (messageSource == null) throw new IllegalStateException("messageSource property null")
        if (object instanceof BeanPropertyBindingResult) {

            context.setContentType(CONTENT_TYPE)
            Locale locale = context.locale
            final target = object.target


            def jsonMap = []
            for(ObjectError oe in object.allErrors) {
                final msg = messageSource.getMessage(oe, locale)

                jsonMap << [
                    logref: getObjectId(target),
                    message: msg,
                    '_links': [
                        resource:[href: linkGenerator.link(resource: target, method:"GET", absolute: true)]
                    ]
                ]
            }

            def json = jsonMap as JSON

            json.render(context.getWriter())
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Object getObjectId(target) {
        target.id
    }


    @Override
    Class<Object> getComponentType() {
        Object
    }
}
