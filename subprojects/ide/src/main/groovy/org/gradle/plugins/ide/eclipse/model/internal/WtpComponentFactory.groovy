/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.plugins.ide.eclipse.model.internal

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.plugins.ide.eclipse.model.EclipseWtpComponent
import org.gradle.plugins.ide.eclipse.model.WbDependentModule
import org.gradle.plugins.ide.eclipse.model.WbResource
import org.gradle.plugins.ide.eclipse.model.WtpComponent
import org.gradle.api.artifacts.Configuration

/**
 * @author Hans Dockter
 */
class WtpComponentFactory {
    void configure(EclipseWtpComponent wtp, WtpComponent component) {
        def entries = getEntriesFromSourceDirs(wtp)
        entries.addAll(wtp.resources)
        entries.addAll(wtp.properties)
        // for ear files root deps are NOT transitive; wars don't use root deps so this doesn't hurt them
        // TODO: maybe do this in a more explicit way, via config or something
        entries.addAll(getEntriesFromConfigurations(wtp.rootConfigurations, wtp.minusConfigurations, wtp, '/', false))
        entries.addAll(getEntriesFromConfigurations(wtp.libConfigurations, wtp.minusConfigurations, wtp, wtp.libDeployPath, true))

        component.configure(wtp.deployName, wtp.contextPath, entries)
    }

    private List getEntriesFromSourceDirs(EclipseWtpComponent wtp) {
        wtp.sourceDirs.findAll { it.isDirectory() }.collect { dir ->
            new WbResource(wtp.classesDeployPath, wtp.project.relativePath(dir))
        }
    }

    private List getEntriesFromConfigurations(Set plusConfigurations, Set minusConfigurations, EclipseWtpComponent wtp, String deployPath, boolean transitive) {
        (getEntriesFromProjectDependencies(plusConfigurations, minusConfigurations, deployPath, transitive) as List) +
                (getEntriesFromLibraries(plusConfigurations, minusConfigurations, wtp, deployPath) as List)
    }

    // must include transitive project dependencies
    private Set getEntriesFromProjectDependencies(Set plusConfigurations, Set minusConfigurations, String deployPath, boolean transitive) {
        def dependencies = getDependencies(plusConfigurations, minusConfigurations,
                { it instanceof org.gradle.api.artifacts.ProjectDependency })

        def projects = dependencies*.dependencyProject

        def allProjects = [] as LinkedHashSet
        allProjects.addAll(projects)
        if (transitive) {
            projects.each { collectDependedUponProjects(it, allProjects) }
        }

        allProjects.collect { project ->
            new WbDependentModule(deployPath, "module:/resource/" + project.name + "/" + project.name)
        }
    }

    // TODO: might have to search all class paths of all source sets for project dependencies, not just runtime configuration
    private void collectDependedUponProjects(org.gradle.api.Project project, LinkedHashSet result) {
        def runtimeConfig = project.configurations.findByName("runtime")
        if (runtimeConfig) {
            def projectDeps = runtimeConfig.allDependencies.withType(org.gradle.api.artifacts.ProjectDependency)
            def dependedUponProjects = projectDeps*.dependencyProject
            result.addAll(dependedUponProjects)
            for (dependedUponProject in dependedUponProjects) {
                collectDependedUponProjects(dependedUponProject, result)
            }
        }
    }

    // must NOT include transitive library dependencies
    private Set getEntriesFromLibraries(Set plusConfigurations, Set minusConfigurations, EclipseWtpComponent wtp, String deployPath) {
        Set declaredDependencies = getDependencies(plusConfigurations, minusConfigurations,
                { it instanceof ExternalDependency})

        Set libFiles = wtp.project.configurations.detachedConfiguration((declaredDependencies as Dependency[])).files +
                getSelfResolvingFiles(getDependencies(plusConfigurations, minusConfigurations,
                        { it instanceof SelfResolvingDependency && !(it instanceof org.gradle.api.artifacts.ProjectDependency)}))

        libFiles.collect { file ->
            createWbDependentModuleEntry(file, wtp.fileReferenceFactory, deployPath)
        }
    }

    private LinkedHashSet getSelfResolvingFiles(LinkedHashSet<SelfResolvingDependency> dependencies) {
        dependencies.collect { it.resolve() }.flatten() as LinkedHashSet
    }

    private WbDependentModule createWbDependentModuleEntry(File file, FileReferenceFactory fileReferenceFactory, String deployPath) {
        def ref = fileReferenceFactory.fromFile(file)
        def handleSnippet
        if (ref.relativeToPathVariable) {
            handleSnippet = "var/$ref.path"
        } else {
            handleSnippet = "lib/${ref.path}"
        }
        return new WbDependentModule(deployPath, "module:/classpath/$handleSnippet")
    }

    private LinkedHashSet getDependencies(Set plusConfigurations, Set minusConfigurations, Closure filter) {
        def declaredDependencies = new LinkedHashSet()
        plusConfigurations.each { Configuration configuration ->
            declaredDependencies.addAll(configuration.allDependencies.matching(filter))
        }
        minusConfigurations.each { Configuration configuration ->
            declaredDependencies.removeAll(configuration.allDependencies.matching(filter))
        }
        return declaredDependencies
    }
}
