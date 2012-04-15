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

package org.gradle.api.tasks.bundling

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import static org.hamcrest.Matchers.equalTo

class WarTaskIntegrationTest extends AbstractIntegrationSpec {

    def canCreateAWarArchiveWithNoWebXml() {
        given:
        createDir('content') {
            content1 {
                file 'file1.jsp'
            }
        }
        createDir('web-inf') {
            webinf1 {
                file 'file1.txt'
            }
        }
        createDir('meta-inf') {
            metainf1 {
                file 'file2.txt'
            }
        }
        createDir('classes') {
            org {
                gradle {
                    file 'resource.txt'
                    file 'Person.class'
                }
            }
        }
        createZip("lib.jar") {
            file "Dependency.class"
        }
        and:
        buildFile << """
            task war(type: War) {
                from 'content'
                metaInf {
                    from 'meta-inf'
                }
                webInf {
                    from 'web-inf'
                }
                classpath 'classes'
                classpath 'lib.jar'
                destinationDir = buildDir
                archiveName = 'test.war'
            }
        """
        when:
        run "war"
        then:
        def expandDir = file('expanded')
        file('build/test.war').unzipTo(expandDir)
        expandDir.assertHasDescendants(
                'META-INF/MANIFEST.MF',
                'META-INF/metainf1/file2.txt',
                'content1/file1.jsp',
                'WEB-INF/lib/lib.jar',
                'WEB-INF/classes/org/gradle/resource.txt',
                'WEB-INF/classes/org/gradle/Person.class',
                'WEB-INF/webinf1/file1.txt')

        expandDir.file('META-INF/MANIFEST.MF').assertContents(equalTo('Manifest-Version: 1.0\r\n\r\n'))
    }

    def canCreateAWarArchiveWithWebXml() {
        given: file('some.xml') << '<web/>'
        createDir('web-inf') {
            webinf1 {
                file 'file1.txt'
            }
        }
        and:
        buildFile << """
            task war(type: War) {
                webInf {
                    from 'web-inf'
                    exclude '**/*.xml'
                }
                webXml = file('some.xml')
                destinationDir = buildDir
                archiveName = 'test.war'
            }
        """
        when:
        run "war"
        then:
        def expandDir = file('expanded')
        file('build/test.war').unzipTo(expandDir)
        expandDir.assertHasDescendants(
                'META-INF/MANIFEST.MF',
                'WEB-INF/web.xml',
                'WEB-INF/webinf1/file1.txt')
    }

    def canAddFilesToWebInfDir() {
        given:
        createDir('web-inf') {
            webinf1 {
                file 'file1.txt'
                file 'ignore.xml'
            }
        }
        createDir('web-inf2') {
            file 'file2.txt'
        }
        and:
        buildFile << """
            task war(type: War) {
                webInf {
                    from 'web-inf'
                    exclude '**/*.xml'
                }
                webInf {
                    from 'web-inf2'
                    into 'dir2'
                    include '**/file2*'
                }
                destinationDir = buildDir
                archiveName = 'test.war'
            }
        """
        when:
        run 'war'
        then:
        def expandDir = file('expanded')
        file('build/test.war').unzipTo(expandDir)
        expandDir.assertHasDescendants(
                'META-INF/MANIFEST.MF',
                'WEB-INF/webinf1/file1.txt',
                'WEB-INF/dir2/file2.txt')
    }
}
