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
package org.gradle.api.plugins.quality

import org.gradle.integtests.fixtures.WellBehavedPluginTest
import org.hamcrest.Matcher
import static org.gradle.util.Matchers.containsLine
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.startsWith

class FindBugsPluginIntegrationTest extends WellBehavedPluginTest {
    @Override
    String getMainTask() {
        return "check"
    }

    def setup() {
        writeBuildFile()
    }

    def "analyze good code"() {
        goodCode()
        expect:
        succeeds("check")
		file("build/reports/findbugs/main.xml").assertContents(containsClass("org.gradle.Class1"))
		file("build/reports/findbugs/test.xml").assertContents(containsClass("org.gradle.Class1Test"))
    }

    void "analyze bad code"() {
        file("src/main/java/org/gradle/Class1.java") << "package org.gradle; class Class1 { public boolean equals(Object arg) { return true; } }"

        expect:
        fails("check")
		failure.assertHasDescription("Execution failed for task ':findbugsMain'")
        failure.assertThatCause(startsWith("FindBugs rule violations were found. See the report at"))
		file("build/reports/findbugs/main.xml").assertContents(containsClass("org.gradle.Class1"))
    }

    def "is incremental"() {
        given:
        goodCode()

        expect:
        succeeds("findbugsMain") && ":findbugsMain" in nonSkippedTasks
        succeeds(":findbugsMain") && ":findbugsMain" in skippedTasks

        when:
        file("build/reports/findbugs/main.xml").delete()

        then:
        succeeds("findbugsMain") && ":findbugsMain" in nonSkippedTasks
    }

    def "cannot generate multiple reports"() {
        given:
        buildFile << """
            findbugsMain.reports {
                xml.enabled true
                html.enabled true
            }
        """

        and:
        goodCode()

        expect:
        fails "findbugsMain"

        failure.assertHasCause "Findbugs tasks can only have one report enabled"
    }
    
    def "can generate html reports"() {
        given:
        buildFile << """
            findbugsMain.reports {
                xml.enabled false
                html.enabled true
            }
        """
        
        and:
        goodCode()
        
        when:
        run "findbugsMain"
        
        then:
        file("build/reports/findbugs/main.html").exists()
    }

    def "can generate no reports"() {
        given:
        buildFile << """
            findbugsMain.reports {
                xml.enabled false
                html.enabled false
            }
        """

        and:
        goodCode()

        expect:
        succeeds "findbugsMain"

        and:
        !file("build/reports/findbugs/main.html").exists()
        !file("build/reports/findbugs/main.xml").exists()
    }

    private goodCode() {
        file("src/main/java/org/gradle/Class1.java") << "package org.gradle; class Class1 { public boolean isFoo(Object arg) { return true; } }"
        file("src/test/java/org/gradle/Class1Test.java") << "package org.gradle; class Class1Test { public boolean isFoo(Object arg) { return true; } }"
    }

    private Matcher<String> containsClass(String className) {
        containsLine(containsString(className.replace(".", File.separator)))
    }
  
    private void writeBuildFile() {
        file("build.gradle") << """
        apply plugin: "java"
        apply plugin: "findbugs"
        
        repositories {
            mavenCentral()
        }
        """
    }
}
