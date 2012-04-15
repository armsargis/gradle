/*
 * Copyright 2012 the original author or authors.
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
package org.gradle.integtests.resolve.custom

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.IvyRepository
import org.gradle.integtests.fixtures.SFTPServer
import org.junit.Rule

class IvySFtpResolverIntegrationTest extends AbstractIntegrationSpec {

    @Rule
    public final SFTPServer server = new SFTPServer(distribution.temporaryFolder)

    def "setup"() {
        requireOwnUserHomeDir()
    }

    public void "can resolve and cache dependencies from an SFTP Ivy repository"() {
        given:
        def repo = ivyRepo()
        def module = repo.module('group', 'projectA', '1.2')
        module.publish();

        and:
        buildFile << """
repositories {
    add(new org.apache.ivy.plugins.resolver.SFTPResolver()) {
        name = "sftprepo"
        host = "${server.hostAddress}"
        port = ${server.port}
        user = "simple"
        userPassword = "simple"
        addIvyPattern "repos/libs/[organization]/[module]/[revision]/ivy-[revision].xml"
        addArtifactPattern "repos/libs/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"
    }
}
configurations { compile }
dependencies { compile 'group:projectA:1.2' }
task listJars << {
    assert configurations.compile.collect { it.name } == ['projectA-1.2.jar']
}
"""
        when:
        withDebugLogging()

        succeeds 'listJars'

        then:
        server.fileRequests == ["repos/libs/group/projectA/1.2/ivy-1.2.xml",
                                "repos/libs/group/projectA/1.2/projectA-1.2.jar"
                               ] as Set

        when:
        server.clearRequests()
        succeeds 'listJars'

        then:
        server.fileRequests.empty
    }

    IvyRepository ivyRepo() {
            return new IvyRepository(server.file("repos/libs/"))
    }
}