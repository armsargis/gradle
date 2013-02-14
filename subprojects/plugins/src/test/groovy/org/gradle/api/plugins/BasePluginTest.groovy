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
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.HelperUtil
import spock.lang.Specification
import static org.gradle.util.Matchers.dependsOn
import static org.hamcrest.Matchers.instanceOf

/**
 * @author Hans Dockter
 */
class BasePluginTest extends Specification {
    private final Project project = HelperUtil.createRootProject()
    private final BasePlugin plugin = new BasePlugin()

    public void addsConventionObjects() {
        when:
        plugin.apply(project)

        then:
        project.convention.plugins.base instanceof BasePluginConvention
        project.extensions.findByType(DefaultArtifactPublicationSet) != null
    }

    public void createsTasksAndAppliesMappings() {
        when:
        plugin.apply(project)

        then:
        def clean = project.tasks[BasePlugin.CLEAN_TASK_NAME]
        clean instanceOf(Delete)
        clean dependsOn()
        clean.targetFiles.files == [project.buildDir] as Set

        and:
        def assemble = project.tasks[BasePlugin.ASSEMBLE_TASK_NAME]
        assemble instanceOf(DefaultTask)
    }

    public void assembleTaskBuildsThePublishedArtifacts() {
        given:
        def someJar = project.tasks.add('someJar', Jar)

        when:
        plugin.apply(project)
        project.artifacts.archives someJar

        then:
        def assemble = project.tasks[BasePlugin.ASSEMBLE_TASK_NAME]
        assemble dependsOn('someJar')
    }

    public void addsRulesWhenAConfigurationIsAdded() {
        when:
        plugin.apply(project)

        then:
        !project.tasks.rules.empty
    }

    public void addsImplicitTasksForConfiguration() {
        given:
        def someJar = project.tasks.add('someJar', Jar)

        when:
        plugin.apply(project)
        project.artifacts.archives someJar

        then:
        def buildArchives = project.tasks['buildArchives']
        buildArchives instanceOf(DefaultTask)
        buildArchives dependsOn('someJar')

        and:
        def uploadArchives = project.tasks['uploadArchives']
        uploadArchives instanceOf(Upload)
        uploadArchives dependsOn('someJar')

        when:
        project.configurations.add('conf')
        project.artifacts.conf someJar

        then:
        def buildConf = project.tasks['buildConf']
        buildConf instanceOf(DefaultTask)
        buildConf dependsOn('someJar')

        and:
        def uploadConf = project.tasks['uploadConf']
        uploadConf instanceOf(Upload)
        uploadConf dependsOn('someJar')
        uploadConf.configuration == project.configurations.conf
    }

    public void addsACleanRule() {
        given:
        Task test = project.task('test')
        test.outputs.files(project.buildDir)

        when:
        plugin.apply(project)

        then:
        Task cleanTest = project.tasks['cleanTest']
        cleanTest instanceOf(Delete)
        cleanTest.delete == [test.outputs.files] as Set
    }

    public void cleanRuleIsCaseSensitive() {
        given:
        project.task('testTask')
        project.task('12')

        when:
        plugin.apply(project)

        then:
        project.tasks.findByName('cleantestTask') == null
        project.tasks.findByName('cleanTesttask') == null
        project.tasks.findByName('cleanTestTask') instanceof Delete
        project.tasks.findByName('clean12') instanceof Delete
    }

    public void appliesMappingsForArchiveTasks() {
        when:
        plugin.apply(project)
        project.version = '1.0'

        then:
        def someJar = project.tasks.add('someJar', Jar)
        someJar.destinationDir == project.libsDir
        someJar.version == project.version
        someJar.baseName == project.archivesBaseName

        and:
        def someZip = project.tasks.add('someZip', Zip)
        someZip.destinationDir == project.distsDir
        someZip.version == project.version
        someZip.baseName == project.archivesBaseName

        and:
        def someTar = project.tasks.add('someTar', Tar)
        someTar.destinationDir == project.distsDir
        someTar.version == project.version
        someTar.baseName == project.archivesBaseName
    }

    public void usesNullVersionWhenProjectVersionNotSpecified() {
        when:
        plugin.apply(project)

        then:
        def task = project.tasks.add('someJar', Jar)
        task.version == null

        when:
        project.version = '1.0'

        then:
        task.version == '1.0'
    }

    public void addsConfigurationsToTheProject() {
        when:
        plugin.apply(project)

        then:
        def defaultConfig = project.configurations[Dependency.DEFAULT_CONFIGURATION]
        defaultConfig.extendsFrom == [] as Set
        defaultConfig.visible
        defaultConfig.transitive

        and:
        def archives = project.configurations[Dependency.ARCHIVES_CONFIGURATION]
        defaultConfig.extendsFrom == [] as Set
        archives.visible
        archives.transitive
    }

    public void addsEveryPublishedArtifactToTheArchivesConfiguration() {
        PublishArtifact artifact = Mock()

        when:
        plugin.apply(project)
        project.configurations.add("custom").artifacts.add(artifact)

        then:
        project.configurations[Dependency.ARCHIVES_CONFIGURATION].artifacts.contains(artifact)
    }
}
