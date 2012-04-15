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
package org.gradle.integtests.resolve.artifactreuse

import org.gradle.integtests.resolve.AbstractDependencyResolutionTest
import org.gradle.util.SetSystemProperties
import org.hamcrest.Matchers
import org.junit.Rule

class ResolutionOverrideIntegrationTest extends AbstractDependencyResolutionTest {
    @Rule
    public SetSystemProperties systemProperties = new SetSystemProperties()

    public void "will non-changing module when run with --refresh-dependencies"() {
        given:
        server.start()
        def module = mavenRepo().module('org.name', 'projectA', '1.2').publish()

        and:
        buildFile << """
repositories {
    maven { url "http://localhost:${server.port}/repo" }
}
configurations { compile }
dependencies { compile 'org.name:projectA:1.2' }
task retrieve(type: Sync) {
    into 'libs'
    from configurations.compile
}
"""
        and:
        server.allowGet('/repo', mavenRepo().rootDir)

        when:
        succeeds 'retrieve'
        
        then:
        file('libs').assertHasDescendants('projectA-1.2.jar')
        def snapshot = file('libs/projectA-1.2.jar').snapshot()

        when:
        module.publishWithChangedContent()

        and:
        executer.withArguments('--refresh-dependencies')
        succeeds 'retrieve'
        
        then:
        file('libs/projectA-1.2.jar').assertIsCopyOf(module.artifactFile).assertHasChangedSince(snapshot)
    }

    public void "will recover from missing module when run with --refresh-dependencies"() {
        server.start()

        given:
        def module = mavenRepo().module('org.name', 'projectA', '1.2').publish()

        buildFile << """
repositories {
    maven {
        url "http://localhost:${server.port}"
    }
}
configurations { missing }
dependencies {
    missing 'org.name:projectA:1.2'
}
task showMissing << { println configurations.missing.files }
"""

        when:
        server.expectGetMissing('/org/name/projectA/1.2/projectA-1.2.pom')
        server.expectHeadMissing('/org/name/projectA/1.2/projectA-1.2.jar')

        then:
        fails("showMissing")

        when:
        server.resetExpectations()
        server.expectGet('/org/name/projectA/1.2/projectA-1.2.pom', module.pomFile)
        server.expectGet('/org/name/projectA/1.2/projectA-1.2.jar', module.artifactFile)

        then:
        executer.withArguments("--refresh-dependencies")
        succeeds('showMissing')
    }

    public void "will recover from missing artifact when run with --refresh-dependencies"() {
        server.start()

        given:
        buildFile << """
repositories {
    maven {
        url "http://localhost:${server.port}"
    }
}
configurations { compile }
dependencies {
    compile 'org.name:projectA:1.2'
}
task retrieve(type: Sync) {
    from configurations.compile
    into 'libs'
}
"""

        and:
        def module = mavenRepo().module('org.name', 'projectA', '1.2').publish()

        when:
        server.expectGet('/org/name/projectA/1.2/projectA-1.2.pom', module.pomFile)
        server.expectGetMissing('/org/name/projectA/1.2/projectA-1.2.jar')

        then:
        fails "retrieve"

        when:
        server.resetExpectations()
        module.expectPomHead(server)
        module.expectArtifactGet(server)

        then:
        executer.withArguments("--refresh-dependencies")
        succeeds 'retrieve'
        file('libs').assertHasDescendants('projectA-1.2.jar')
    }

    public void "will not expire cache entries when run with offline flag"() {

        given:
        server.start()
        def module = mavenRepo().module("org.name", "unique", "1.0-SNAPSHOT").publish()

        and:
        buildFile << """
repositories {
    maven { url "http://localhost:${server.port}/repo" }
}
configurations { compile }
configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}
dependencies {
    compile "org.name:unique:1.0-SNAPSHOT"
}
task retrieve(type: Sync) {
    into 'libs'
    from configurations.compile
}
"""

        when:  "Server handles requests"
        server.allowGet("/repo", mavenRepo().rootDir)

        and: "We resolve dependencies"
        run 'retrieve'

        then:
        file('libs').assertHasDescendants('unique-1.0-SNAPSHOT.jar')
        def snapshot = file('libs/unique-1.0-SNAPSHOT.jar').snapshot()

        when:
        module.publishWithChangedContent()

        and: "We resolve again, offline"
        server.resetExpectations()
        executer.withArguments('--offline')
        run 'retrieve'

        then:
        file('libs').assertHasDescendants('unique-1.0-SNAPSHOT.jar')
        file('libs/unique-1.0-SNAPSHOT.jar').assertHasNotChangedSince(snapshot)
    }

    public void "does not attempt to contact server when run with offline flag"() {
        given:
        server.start()

        and:
        buildFile << """
repositories {
    maven { url "http://localhost:${server.port}/repo" }
}
configurations { compile }
dependencies { compile 'org.name:projectA:1.2' }
task listJars << {
    assert configurations.compile.collect { it.name } == ['projectA-1.2.jar']
}
"""

        when:
        executer.withArguments("--offline")

        then:
        fails 'listJars'

        and:
        failure.assertHasDescription('Execution failed for task \':listJars\'.')
        failure.assertHasCause('Could not resolve all dependencies for configuration \':compile\'.')
        failure.assertThatCause(Matchers.containsString('No cached version available for offline mode'))
    }
}
