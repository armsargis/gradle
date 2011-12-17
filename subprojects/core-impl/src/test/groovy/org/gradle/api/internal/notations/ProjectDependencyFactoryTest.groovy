/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.internal.notations;


import org.gradle.api.internal.DirectInstantiator
import org.gradle.api.internal.artifacts.ProjectDependenciesBuildInstruction
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.GUtil
import spock.lang.Specification

/**
 * @author Hans Dockter
 */
public class ProjectDependencyFactoryTest extends Specification {

    def ProjectDependenciesBuildInstruction projectDependenciesBuildInstruction = new ProjectDependenciesBuildInstruction(false);
    def ProjectDependencyFactory factory = new ProjectDependencyFactory(projectDependenciesBuildInstruction, new DirectInstantiator());
    def ProjectFinder projectFinder = Mock(ProjectFinder.class);
    def ProjectInternal projectDummy = Mock(ProjectInternal.class);

    def testCreateProjectDependencyWithMapNotation() {
        given:
        boolean expectedTransitive = false;
        final Map<String, Object> mapNotation = GUtil.map("path", ":foo:bar", "configuration", "compile", "transitive", expectedTransitive);

        and:
        projectFinder.getProject(':foo:bar') >> projectDummy

        when:
        def projectDependency = factory.createFromMap(projectFinder, mapNotation);

        then:
        projectDependency.getDependencyProject() == projectDummy
        projectDependency.getConfiguration() == "compile"
        projectDependency.isTransitive() == expectedTransitive
    }
}
