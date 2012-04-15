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



package org.gradle.integtests.tooling.rc1

import org.gradle.integtests.tooling.fixture.ConfigurableOperation
import org.gradle.integtests.tooling.fixture.MinTargetGradleVersion
import org.gradle.integtests.tooling.fixture.MinToolingApiVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException
import org.gradle.tooling.model.GradleProject

@MinToolingApiVersion('current')
@MinTargetGradleVersion('current')
class PassingCommandLineArgumentsCrossVersionSpec extends ToolingApiSpecification {

//    We don't want to validate *all* command line options here, just enough to make sure passing through works.

    def "understands project properties for building model"() {
        given:
        dist.file("build.gradle") << """
        description = project.getProperty('theDescription')
"""

        when:
        GradleProject project = withConnection { ProjectConnection it ->
            it.model(GradleProject).withArguments('-PtheDescription=heyJoe').get()
        }

        then:
        project.description == 'heyJoe'
    }

    def "understands system properties"() {
        given:
        dist.file("build.gradle") << """
        task printProperty << {
            file('sysProperty.txt') << System.getProperty('sysProperty')
        }
"""

        when:
        withConnection { ProjectConnection it ->
            it.newBuild().forTasks('printProperty').withArguments('-DsysProperty=welcomeToTheJungle').run()
        }

        then:
        dist.file('sysProperty.txt').text.contains('welcomeToTheJungle')
    }

    def "can use custom build file"() {
        given:
        dist.file("foo.gradle") << """
        task someCoolTask
"""

        when:
        withConnection { ProjectConnection it ->
            it.newBuild().forTasks('someCoolTask').withArguments('-b', 'foo.gradle').run()
        }

        then:
        noExceptionThrown()

    }

    def "can use custom log level"() {
        //logging infrastructure is not installed when running in-process to avoid issues
        toolingApi.isEmbedded = false
        toolingApi.verboseLogging = false

        given:
        dist.file("build.gradle") << """
        logger.debug("debugging stuff")
        logger.info("infoing stuff")
"""

        when:
        def debug = withConnection {
            def build = it.newBuild().withArguments('-d')
            def op = new ConfigurableOperation(build)
            build.run()
            op.standardOutput
        }

        and:
        def info = withConnection {
            def build = it.newBuild().withArguments('-i')
            def op = new ConfigurableOperation(build)
            build.run()
            op.standardOutput
        }

        then:
        debug.count("debugging stuff") == 1
        debug.count("infoing stuff") == 1

        and:
        info.count("debugging stuff") == 0
        info.count("infoing stuff") == 1
    }

    def "gives decent feedback for invalid option"() {
        when:
        def ex = maybeFailWithConnection { ProjectConnection it ->
            it.newBuild().withArguments('--foreground').run()
        }

        then:
        ex instanceof UnsupportedBuildArgumentException
        ex.message.contains('--foreground')
    }

    def "can overwrite project dir via build arguments"() {
        given:
        dist.file('otherDir').createDir()
        dist.file('build.gradle') << "assert projectDir.name.endsWith('otherDir')"

        when:
        withConnection { 
            it.newBuild().withArguments('-p', 'otherDir').run()
        }

        then:
        noExceptionThrown()
    }

    def "can overwrite gradle user home via build arguments"() {
        given:
        dist.file('.myGradle').createDir()
        dist.file('build.gradle') << "assert gradle.gradleUserHomeDir.name.endsWith('.myGradle')"

        when:
        withConnection {
            it.newBuild().withArguments('-p', '.myGradle').run()
        }

        then:
        noExceptionThrown()
    }

    def "can overwrite searchUpwards via build arguments"() {
        given:
        dist.file('build.gradle') << "assert !gradle.startParameter.searchUpwards"

        when:
        toolingApi.withConnector { it.searchUpwards(true) }
        withConnection {
            it.newBuild().withArguments('-u').run()
        }

        then:
        noExceptionThrown()
    }

    def "can overwrite task names via build arguments"() {
        given:
        dist.file('build.gradle') << """
task foo << { assert false }
task bar << { assert true }
"""

        when:
        withConnection {
            it.newBuild().forTasks('foo').withArguments('bar').run()
        }

        then:
        noExceptionThrown()
    }
}
