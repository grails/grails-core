/*
 * Copyright 2012-2019 the original author or authors.
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

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;

import org.grails.cli.compiler.grape.DependencyResolutionContext;
import org.springframework.core.annotation.Order;

/**
 * {@link ASTTransformation} to apply
 * {@link CompilerAutoConfiguration#applyDependencies(DependencyCustomizer) dependency
 * auto-configuration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 */
@Order(DependencyAutoConfigurationTransformation.ORDER)
public class DependencyAutoConfigurationTransformation implements ASTTransformation {

	/**
	 * The order of the transformation.
	 */
	public static final int ORDER = DependencyManagementBomTransformation.ORDER + 100;

	private final GroovyClassLoader loader;

	private final DependencyResolutionContext dependencyResolutionContext;

	private final Iterable<CompilerAutoConfiguration> compilerAutoConfigurations;

	public DependencyAutoConfigurationTransformation(GroovyClassLoader loader,
			DependencyResolutionContext dependencyResolutionContext,
			Iterable<CompilerAutoConfiguration> compilerAutoConfigurations) {
		this.loader = loader;
		this.dependencyResolutionContext = dependencyResolutionContext;
		this.compilerAutoConfigurations = compilerAutoConfigurations;

	}

	@Override
	public void visit(ASTNode[] nodes, SourceUnit source) {
		for (ASTNode astNode : nodes) {
			if (astNode instanceof ModuleNode) {
				visitModule((ModuleNode) astNode);
			}
		}
	}

	private void visitModule(ModuleNode module) {
		DependencyCustomizer dependencies = new DependencyCustomizer(this.loader, module,
				this.dependencyResolutionContext);
		for (ClassNode classNode : module.getClasses()) {
			for (CompilerAutoConfiguration autoConfiguration : this.compilerAutoConfigurations) {
				if (autoConfiguration.matches(classNode)) {
					autoConfiguration.applyDependencies(dependencies);
				}
			}
		}
	}

}
