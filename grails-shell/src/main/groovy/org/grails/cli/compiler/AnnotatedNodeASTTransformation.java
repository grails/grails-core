/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.cli.compiler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;

/**
 * A base class for {@link ASTTransformation AST transformations} that are solely
 * interested in {@link AnnotatedNode AnnotatedNodes}.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public abstract class AnnotatedNodeASTTransformation implements ASTTransformation {

	private final Set<String> interestingAnnotationNames;

	private final boolean removeAnnotations;

	private SourceUnit sourceUnit;

	protected AnnotatedNodeASTTransformation(Set<String> interestingAnnotationNames, boolean removeAnnotations) {
		this.interestingAnnotationNames = interestingAnnotationNames;
		this.removeAnnotations = removeAnnotations;
	}

	@Override
	public void visit(ASTNode[] nodes, SourceUnit source) {
		this.sourceUnit = source;
		List<AnnotationNode> annotationNodes = new ArrayList<>();
		ClassVisitor classVisitor = new ClassVisitor(source, annotationNodes);
		for (ASTNode node : nodes) {
			if (node instanceof ModuleNode) {
				ModuleNode module = (ModuleNode) node;
				visitAnnotatedNode(module.getPackage(), annotationNodes);
				for (ImportNode importNode : module.getImports()) {
					visitAnnotatedNode(importNode, annotationNodes);
				}
				for (ImportNode importNode : module.getStarImports()) {
					visitAnnotatedNode(importNode, annotationNodes);
				}
				module.getStaticImports()
					.forEach((name, importNode) -> visitAnnotatedNode(importNode, annotationNodes));
				module.getStaticStarImports()
					.forEach((name, importNode) -> visitAnnotatedNode(importNode, annotationNodes));
				for (ClassNode classNode : module.getClasses()) {
					visitAnnotatedNode(classNode, annotationNodes);
					classNode.visitContents(classVisitor);
				}
			}
		}
		processAnnotationNodes(annotationNodes);
	}

	protected SourceUnit getSourceUnit() {
		return this.sourceUnit;
	}

	protected abstract void processAnnotationNodes(List<AnnotationNode> annotationNodes);

	private void visitAnnotatedNode(AnnotatedNode annotatedNode, List<AnnotationNode> annotatedNodes) {
		if (annotatedNode != null) {
			Iterator<AnnotationNode> annotationNodes = annotatedNode.getAnnotations().iterator();
			while (annotationNodes.hasNext()) {
				AnnotationNode annotationNode = annotationNodes.next();
				if (this.interestingAnnotationNames.contains(annotationNode.getClassNode().getName())) {
					annotatedNodes.add(annotationNode);
					if (this.removeAnnotations) {
						annotationNodes.remove();
					}
				}
			}
		}
	}

	private class ClassVisitor extends ClassCodeVisitorSupport {

		private final SourceUnit source;

		private final List<AnnotationNode> annotationNodes;

		ClassVisitor(SourceUnit source, List<AnnotationNode> annotationNodes) {
			this.source = source;
			this.annotationNodes = annotationNodes;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return this.source;
		}

		@Override
		public void visitAnnotations(AnnotatedNode node) {
			visitAnnotatedNode(node, this.annotationNodes);
		}

	}

}
