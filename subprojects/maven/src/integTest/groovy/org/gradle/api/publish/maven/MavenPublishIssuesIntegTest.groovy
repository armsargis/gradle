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

package org.gradle.api.publish.maven
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.maven.M2Installation
import org.spockframework.util.TextUtil
import spock.lang.Issue
/**
 * Tests for bugfixes to maven publishing scenarios
 */
class MavenPublishIssuesIntegTest extends AbstractIntegrationSpec {

    @Issue("GRADLE-2456")
    def "generates SHA1 file with leading zeros"() {
        given:
        def module = mavenRepo.module("org.gradle", "publish", "2")
        byte[] jarBytes = [0, 0, 0, 5]
        def artifactFile = file("testfile.bin")
        artifactFile << jarBytes
        def artifactPath = TextUtil.escape(artifactFile.path)

        and:
        settingsFile << 'rootProject.name = "publish"'
        buildFile << """
    apply plugin: 'maven-publish'

    group = "org.gradle"
    version = '2'

    publishing {
        repositories {
            maven { url "${mavenRepo.uri}" }
        }
        publications {
            pub(MavenPublication) {
                artifact file("${artifactPath}")
            }
        }
    }
    """

        when:
        succeeds 'publish'

        then:
        def shaOneFile = module.moduleDir.file("publish-2.bin.sha1")
        shaOneFile.exists()
        shaOneFile.text == "00e14c6ef59816760e2c9b5a57157e8ac9de4012"
    }

    @Issue("GRADLE-2681")
    def "gradle ignores maven mirror configuration for uploading archives"() {
        given:
        TestFile m2Home = temporaryFolder.createDir("m2_home");
        M2Installation m2Installation = new M2Installation(m2Home)

        m2Installation.globalSettingsFile << """
<settings>
  <mirrors>
    <mirror>
      <id>ACME</id>
      <name>ACME Central</name>
      <url>http://acme.maven.org/maven2</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
</settings>
"""

        and:
        settingsFile << "rootProject.name = 'root'"
        buildFile << """
apply plugin: 'java'
apply plugin: 'maven-publish'
group = 'group'
version = '1.0'

publishing {
    repositories {
        maven { url "${mavenRepo.uri}" }
    }
    publications {
        maven(MavenPublication) {
            from components.java
        }
    }
}
   """
        when:
        using m2Installation

        then:
        succeeds "publish"
    }

}
