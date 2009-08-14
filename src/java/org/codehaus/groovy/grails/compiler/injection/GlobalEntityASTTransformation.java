
package org.codehaus.groovy.grails.compiler.injection;

import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * A global AST transformation that injects methods and properties into classes in the grails-app/domain directory
 * 
 * @author Graeme Rocher
 * @since 1.2
 *
 *        <p/>
 *        Created: Dec 17, 2008
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GlobalEntityASTTransformation implements ASTTransformation{

	private GrailsDomainClassInjector domainClassInjector = new DefaultGrailsDomainClassInjector();
	
	public void visit(ASTNode[] nodes, SourceUnit source) {
		
		ASTNode astNode = nodes[0];
		
		if(astNode instanceof ModuleNode) {
			ModuleNode moduleNode = (ModuleNode) astNode;
			
			List classes = moduleNode.getClasses();
			if(classes.size()>0) {
				ClassNode classNode = (ClassNode) classes.get(0);
				domainClassInjector.performInjection(source, classNode);
			}
			
		}
		
	}

}
