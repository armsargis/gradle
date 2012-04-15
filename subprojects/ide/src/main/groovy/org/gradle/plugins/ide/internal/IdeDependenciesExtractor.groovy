/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.plugins.ide.internal

import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.specs.Spec
import org.gradle.api.specs.Specs
import org.gradle.api.artifacts.*

/**
 * @author: Szczepan Faber, created at: 7/1/11
 */
class IdeDependenciesExtractor {

    static class IdeDependency {
        Configuration declaredConfiguration
    }

    static class IdeLocalFileDependency extends IdeDependency {
        File file
    }

    static class IdeRepoFileDependency extends IdeDependency {
        File file
        File sourceFile
        File javadocFile
    }

    static class UnresolvedIdeRepoFileDependency extends IdeRepoFileDependency {
        Exception problem
    }

    static class IdeProjectDependency extends IdeDependency {
        Project project
    }

    List<IdeProjectDependency> extractProjectDependencies(Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations) {
        LinkedHashMap<ProjectDependency, Configuration> depToConf = [:]
        for (plusConfiguration in plusConfigurations) {
            for (ProjectDependency dependency in plusConfiguration.allDependencies.findAll({ it instanceof ProjectDependency })) {
                depToConf[dependency] = plusConfiguration
            }
        }
        for (minusConfiguration in minusConfigurations) {
            for(minusDep in minusConfiguration.allDependencies.findAll({ it instanceof ProjectDependency })) {
                depToConf.remove(minusDep)
            }
        }
        return depToConf.collect { projectDependency, conf ->
            new IdeProjectDependency(project: projectDependency.dependencyProject, declaredConfiguration: conf)
        }
    }

    List<IdeRepoFileDependency> extractRepoFileDependencies(ConfigurationContainer confContainer,
                                                           Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations,
                                                           boolean downloadSources, boolean downloadJavadoc) {
        def out = []

        def allResolvedDependencies = resolveDependencies(plusConfigurations, minusConfigurations)

        Set sourceDependencies = getResolvableDependenciesForAllResolvedDependencies(allResolvedDependencies) { dependency ->
            addSourceArtifact(dependency)
        }

        Map<String, File> sourceFiles = downloadSources ? getFiles(confContainer.detachedConfiguration(sourceDependencies as Dependency[]), "sources") : [:]

        Set javadocDependencies = getResolvableDependenciesForAllResolvedDependencies(allResolvedDependencies) { dependency ->
            addJavadocArtifact(dependency)
        }

        Map<String, File> javadocFiles = downloadJavadoc ? getFiles(confContainer.detachedConfiguration(javadocDependencies as Dependency[]), "javadoc") : [:]

        resolvedExternalDependencies(plusConfigurations, minusConfigurations).each { File binaryFile, Configuration conf ->
            File sourceFile = sourceFiles[binaryFile.name]
            File javadocFile = javadocFiles[binaryFile.name]
            out << new IdeRepoFileDependency( file: binaryFile, sourceFile: sourceFile, javadocFile: javadocFile, declaredConfiguration: conf)
        }

        unresolvedExternalDependencies(plusConfigurations, minusConfigurations) { config, dep ->
            out << new UnresolvedIdeRepoFileDependency(problem: dep.problem, file: new File("unresolved dependency - $dep.id"), declaredConfiguration: config)
        }

        out
    }

    private void unresolvedExternalDependencies(Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations, Closure action) {
        def unresolved = new LinkedHashMap<String, Map>()
        for (c in plusConfigurations) {
            def deps = c.resolvedConfiguration.lenientConfiguration.unresolvedModuleDependencies
            deps.each { unresolved[it.id] = [dep: it, config: c] }
        }
        for (c in minusConfigurations) {
            def deps = c.resolvedConfiguration.lenientConfiguration.unresolvedModuleDependencies
            deps.each { unresolved.remove(it.id) }
        }
        unresolved.values().each {
            action.call(it.config, it.dep)
        }
    }

    List<IdeLocalFileDependency> extractLocalFileDependencies(Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations) {
        LinkedHashMap<File, Configuration> fileToConf = [:]
        def filter = { it instanceof SelfResolvingDependency && !(it instanceof org.gradle.api.artifacts.ProjectDependency)}

        for (plusConfiguration in plusConfigurations) {
            def deps = plusConfiguration.allDependencies.findAll(filter)
            def files = deps.collect { it.resolve() }.flatten()
            files.each { fileToConf[it] = plusConfiguration }
        }
        for (minusConfiguration in minusConfigurations) {
            def deps = minusConfiguration.allDependencies.findAll(filter)
            def files = deps.collect { it.resolve() }.flatten()
            files.each { fileToConf.remove(it) }
        }
        return fileToConf.collect { file, conf ->
            new IdeLocalFileDependency( file: file, declaredConfiguration: conf)
        }
    }

    private Map<File, Configuration> resolvedExternalDependencies(Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations) {
        LinkedHashMap<File, Configuration> fileToConf = [:]
        for (plusConfiguration in plusConfigurations) {
            for (file in plusConfiguration.resolvedConfiguration.lenientConfiguration.getFiles( { it instanceof ExternalDependency } as Spec)) {
                fileToConf[file] = plusConfiguration
            }
        }
        for (minusConfiguration in minusConfigurations) {
            for (file in minusConfiguration.resolvedConfiguration.lenientConfiguration.getFiles({ it instanceof ExternalDependency } as Spec)) {
                fileToConf.remove(file)
            }
        }
        fileToConf
    }

    private Set<ResolvedDependency> resolveDependencies(Collection<Configuration> plusConfigurations, Collection<Configuration> minusConfigurations) {
        def result = new LinkedHashSet()
        for (plusConfiguration in plusConfigurations) {
            result.addAll(getAllDeps(plusConfiguration.resolvedConfiguration.lenientConfiguration.getFirstLevelModuleDependencies({ it instanceof ExternalDependency } as Spec)))
        }
        for (minusConfiguration in minusConfigurations) {
            result.removeAll(getAllDeps(minusConfiguration.resolvedConfiguration.lenientConfiguration.getFirstLevelModuleDependencies({ it instanceof ExternalDependency } as Spec)))
        }
        result
    }

    private Set getAllDeps(Collection deps, Set allDeps = new LinkedHashSet()) {
        deps.each { ResolvedDependency resolvedDependency ->
            def notSeenBefore = allDeps.add(resolvedDependency)
            if (notSeenBefore) { // defend against circular dependencies
                getAllDeps(resolvedDependency.children, allDeps)
            }
        }
        allDeps
    }

    private List getResolvableDependenciesForAllResolvedDependencies(Set allResolvedDependencies, Closure configureClosure) {
        return allResolvedDependencies.collect { ResolvedDependency resolvedDependency ->
            def dependency = new DefaultExternalModuleDependency(resolvedDependency.moduleGroup, resolvedDependency.moduleName, resolvedDependency.moduleVersion,
                    resolvedDependency.configuration)
            dependency.transitive = false
            configureClosure.call(dependency)
            dependency
        }
    }

    private void addSourceArtifact(DefaultExternalModuleDependency dependency) {
        dependency.artifact { artifact ->
            artifact.name = dependency.name
            artifact.type = 'source'
            artifact.extension = 'jar'
            artifact.classifier = 'sources'
        }
    }

    private void addJavadocArtifact(DefaultExternalModuleDependency dependency) {
        dependency.artifact { artifact ->
            artifact.name = dependency.name
            artifact.type = 'javadoc'
            artifact.extension = 'jar'
            artifact.classifier = 'javadoc'
        }
    }

    private Map getFiles(Configuration configuration, String classifier) {
        return (Map) configuration.resolvedConfiguration.lenientConfiguration.getFiles(Specs.satisfyAll()).inject([:]) { result, sourceFile ->
            String key = sourceFile.name.replace("-${classifier}.jar", '.jar')
            result[key] = sourceFile
            result
        }
    }
}
