package org.codehaus.groovy.grails.compiler.injection
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that for classes that transform Grails classes at the AST level.
 *
 * @since 2.0
 * @author Graeme Rocher
 * @deprecated Use {@link grails.compiler.ast.AstTransformer} instead
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE])
public @interface AstTransformer {
}
