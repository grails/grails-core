package grails.web

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Created by graemerocher on 28/05/14.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( [ ElementType.TYPE ] )
@GroovyASTTransformationClass("org.grails.compiler.web.ControllerArtefactTypeTransformation")
public @interface Controller {
}