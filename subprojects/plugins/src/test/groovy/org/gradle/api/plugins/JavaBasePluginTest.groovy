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
package org.gradle.api.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.HelperUtil
import org.gradle.util.Matchers
import org.gradle.util.SetSystemProperties
import org.junit.Rule
import spock.lang.Specification

import static org.gradle.util.Matchers.sameCollection
import static org.gradle.util.WrapUtil.toLinkedSet

/**
 * @author Hans Dockter
 */

class JavaBasePluginTest extends Specification {
    @Rule
    public SetSystemProperties sysProperties = new SetSystemProperties()
    private final Project project = HelperUtil.createRootProject()
    private final JavaBasePlugin javaBasePlugin = new JavaBasePlugin(project.services.get(Instantiator))

    void appliesBasePluginsAndAddsConventionObject() {
        when:
        javaBasePlugin.apply(project)

        then:
        project.getPlugins().hasPlugin(ReportingBasePlugin)
        project.getPlugins().hasPlugin(BasePlugin)
        project.convention.plugins.java instanceof JavaPluginConvention
    }

    void createsTasksAndAppliesMappingsForNewSourceSet() {
        when:
        javaBasePlugin.apply(project)
        project.sourceSets.add('custom')
        
        then:
        def set = project.sourceSets.custom
        set.java.srcDirs == toLinkedSet(project.file('src/custom/java'))
        set.resources.srcDirs == toLinkedSet(project.file('src/custom/resources'))
        set.output.classesDir == new File(project.buildDir, 'classes/custom')
        Matchers.builtBy('customClasses').matches(set.output)

        def processResources = project.tasks['processCustomResources']
        processResources.description == 'Processes the custom resources.'
        processResources instanceof Copy
        Matchers.dependsOn().matches(processResources)
        processResources.destinationDir == project.sourceSets.custom.output.resourcesDir
        def resources = processResources.source
        resources sameCollection(project.sourceSets.custom.resources)

        def compileJava = project.tasks['compileCustomJava']
        compileJava.description == 'Compiles the custom Java source.'
        compileJava instanceof JavaCompile
        Matchers.dependsOn().matches(compileJava)
        compileJava.classpath.is(project.sourceSets.custom.compileClasspath)
        compileJava.destinationDir == project.sourceSets.custom.output.classesDir
        def sources = compileJava.source
        sources sameCollection(project.sourceSets.custom.java)
        def classes = project.tasks['customClasses']
        classes.description == 'Assembles the custom classes.'
        classes instanceof DefaultTask
        Matchers.dependsOn('processCustomResources', 'compileCustomJava').matches(classes)
        classes.dependsOn.contains project.sourceSets.custom.output.dirs
    }
    
    void tasksReflectChangesToSourceSetConfiguration() {
        def classesDir = project.file('target/classes')
        def resourcesDir = project.file('target/resources')

        when:
        javaBasePlugin.apply(project)
        project.sourceSets.add('custom')
        project.sourceSets.custom.output.classesDir = classesDir
        project.sourceSets.custom.output.resourcesDir = resourcesDir

        then:
        def processResources = project.tasks['processCustomResources']
        processResources.destinationDir == resourcesDir

        def compileJava = project.tasks['compileCustomJava']
        compileJava.destinationDir == classesDir
    }

    void createsConfigurationsForNewSourceSet() {
        when:
        javaBasePlugin.apply(project)
        def sourceSet = project.sourceSets.add('custom')

        then:
        def compile = project.configurations.customCompile
        compile.transitive
        !compile.visible
        compile.extendsFrom == [] as Set
        compile.description == 'Classpath for compiling the custom sources.'

        and:
        def runtime = project.configurations.customRuntime
        runtime.transitive
        !runtime.visible
        runtime.extendsFrom == [compile] as Set
        runtime.description == 'Classpath for running the compiled custom classes.'

        and:
        def runtimeClasspath = sourceSet.runtimeClasspath
        def compileClasspath = sourceSet.compileClasspath
        compileClasspath == compile
        runtimeClasspath sameCollection(sourceSet.output + runtime)
    }

    void appliesMappingsToTasksDefinedByBuildScript() {
        when:
        javaBasePlugin.apply(project)

        then:
        def compile = project.task('customCompile', type: JavaCompile)
        compile.sourceCompatibility == project.sourceCompatibility.toString()

        def test = project.task('customTest', type: Test.class)
        test.workingDir == project.projectDir
        test.testResultsDir == project.testResultsDir
        test.testReportDir == project.testReportDir
        test.testReport //by default (JUnit), the report is 'on'

        def javadoc = project.task('customJavadoc', type: Javadoc)
        javadoc.destinationDir == project.file("$project.docsDir/javadoc")
        javadoc.title == project.extensions.getByType(ReportingExtension).apiDocTitle
    }

    void "configures test task for testNG"() {
        given:
        javaBasePlugin.apply(project)
        def test = project.task('customTest', type: Test.class)

        when:
        test.useTestNG()

        then:
        assert test.testReport

        when:
        test.testReport = false
        test.useTestNG()

        then:
        assert !test.testReport
    }

    void appliesMappingsToCustomJarTasks() {
        when:
        javaBasePlugin.apply(project)
        def task = project.task('customJar', type: Jar)

        then:
        Matchers.dependsOn().matches(task)
        task.destinationDir == project.libsDir
    }

    void createsLifecycleBuildTasks() {
        when:
        javaBasePlugin.apply(project)

        then:
        def build = project.tasks[JavaBasePlugin.BUILD_TASK_NAME]
        Matchers.dependsOn(JavaBasePlugin.CHECK_TASK_NAME, BasePlugin.ASSEMBLE_TASK_NAME).matches(build)

        def buildDependent = project.tasks[JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME]
        Matchers.dependsOn(JavaBasePlugin.BUILD_TASK_NAME).matches(buildDependent)

        def buildNeeded = project.tasks[JavaBasePlugin.BUILD_NEEDED_TASK_NAME]
        Matchers.dependsOn(JavaBasePlugin.BUILD_TASK_NAME).matches(buildNeeded)
    }

    def configuresTestTaskWhenDebugSystemPropertyIsSet() {
        javaBasePlugin.apply(project)
        def task = project.tasks.add('test', Test.class)

        when:
        System.setProperty("test.debug", "true")
        project.projectEvaluationBroadcaster.afterEvaluate(project, null)

        then:
        task.debug
    }

    def configuresTestTaskWhenSingleTestSystemPropertyIsSet() {
        javaBasePlugin.apply(project)
        def task = project.tasks.add('test', Test.class)
        task.include 'ignoreme'

        when:
        System.setProperty("test.single", "pattern")
        project.projectEvaluationBroadcaster.afterEvaluate(project, null)

        then:
        task.includes == ['**/pattern*.class'] as Set
    }

}