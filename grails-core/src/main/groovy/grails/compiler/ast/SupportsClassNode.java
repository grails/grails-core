package grails.compiler.ast;

import org.codehaus.groovy.ast.ClassNode;

/**
 * Indicates whether the Transformation/TraitInjector supports given ClassNode
 *
 * @author Sudhir Nimavat
 */
public interface SupportsClassNode {

	boolean supports(ClassNode classNode);

}
