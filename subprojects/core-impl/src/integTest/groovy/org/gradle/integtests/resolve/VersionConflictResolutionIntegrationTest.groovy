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
package org.gradle.integtests.resolve

import org.gradle.api.GradleException
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Ignore
import spock.lang.Issue

import static org.gradle.util.TextUtil.toPlatformLineSeparators
import static org.hamcrest.Matchers.containsString

/**
 * @author Szczepan Faber, @date 03.03.11
 */
class VersionConflictResolutionIntegrationTest extends AbstractIntegrationSpec {

    void "strict conflict resolution should fail due to conflict"() {
        mavenRepo.module("org", "foo", '1.3.3').publish()
        mavenRepo.module("org", "foo", '1.4.4').publish()

        settingsFile << "include 'api', 'impl', 'tool'"

        buildFile << """
allprojects {
	apply plugin: 'java'
	repositories {
		maven { url "${mavenRepo.uri}" }
	}
}

project(':api') {
	dependencies {
		compile (group: 'org', name: 'foo', version:'1.3.3')
	}
}

project(':impl') {
	dependencies {
		compile (group: 'org', name: 'foo', version:'1.4.4')
	}
}

project(':tool') {
	dependencies {
		compile project(':api')
		compile project(':impl')
	}

	configurations.compile.resolutionStrategy.failOnVersionConflict()
}
"""

        expect:
        runAndFail("tool:dependencies")
        failure.assertThatCause(containsString('A conflict was found between the following modules:'))
    }

    void "strict conflict resolution should pass when no conflicts"() {
        mavenRepo.module("org", "foo", '1.3.3').publish()

        settingsFile << "include 'api', 'impl', 'tool'"

        buildFile << """
allprojects {
	apply plugin: 'java'
	repositories {
		maven { url "${mavenRepo.uri}" }
	}
}

project(':api') {
	dependencies {
		compile (group: 'org', name: 'foo', version:'1.3.3')
	}
}

project(':impl') {
	dependencies {
		compile (group: 'org', name: 'foo', version:'1.3.3')
	}
}

project(':tool') {
	dependencies {
		compile project(':api')
		compile project(':impl')
	}

	configurations.all { resolutionStrategy.failOnVersionConflict() }
}
"""

        expect:
        run("tool:dependencies")
    }

    void "resolves to the latest version by default"() {
        mavenRepo.module("org", "foo", '1.3.3').publish()
        mavenRepo.module("org", "foo", '1.4.4').publish()

        settingsFile << "include 'api', 'impl', 'tool'"

        buildFile << """
allprojects {
	apply plugin: 'java'
	repositories {
		maven { url "${mavenRepo.uri}" }
	}
}

project(':api') {
	dependencies {
		compile (group: 'org', name: 'foo', version:'1.3.3')
	}
}

project(':impl') {
	dependencies {
		compile (group: 'org', name: 'foo', version:'1.4.4')
	}
}

project(':tool') {
	dependencies {
		compile project(':api')
		compile project(':impl')
	}
    task checkDeps(dependsOn: configurations.compile) << {
        assert configurations.compile*.name == ['api.jar', 'impl.jar', 'foo-1.4.4.jar']
    }
}
"""

        expect:
        run("tool:checkDeps")
    }

    void "does not attempt to resolve an evicted dependency"() {
        mavenRepo.module("org", "external", "1.2").publish()
        mavenRepo.module("org", "dep", "2.2").dependsOn("org", "external", "1.0").publish()

        buildFile << """
repositories {
    maven { url "${mavenRepo.uri}" }
}

configurations { compile }

dependencies {
    compile 'org:external:1.2'
    compile 'org:dep:2.2'
}

task checkDeps << {
    assert configurations.compile*.name == ['external-1.2.jar', 'dep-2.2.jar']
}
"""

        expect:
        run("checkDeps")
    }

    void "resolves dynamic dependency before resolving conflict"() {
        mavenRepo.module("org", "external", "1.2").publish()
        mavenRepo.module("org", "external", "1.4").publish()
        mavenRepo.module("org", "dep", "2.2").dependsOn("org", "external", "1.+").publish()

        def buildFile = file("build.gradle")
        buildFile << """
repositories {
    maven { url "${mavenRepo.uri}" }
}

configurations { compile }

dependencies {
    compile 'org:external:1.2'
    compile 'org:dep:2.2'
}

task checkDeps << {
    assert configurations.compile*.name == ['dep-2.2.jar', 'external-1.4.jar']
}
"""

        expect:
        run("checkDeps")
    }

    void "fails when version selected by conflict resolution does not exist"() {
        mavenRepo.module("org", "external", "1.2").publish()
        mavenRepo.module("org", "dep", "2.2").dependsOn("org", "external", "1.4").publish()

        buildFile << """
repositories {
    maven { url "${mavenRepo.uri}" }
}

configurations { compile }

dependencies {
    compile 'org:external:1.2'
    compile 'org:dep:2.2'
}

task checkDeps << {
    assert configurations.compile*.name == ['external-1.2.jar', 'dep-2.2.jar']
}
"""

        expect:
        runAndFail("checkDeps")
        failure.assertHasCause("Could not find org:external:1.4.")
    }

    void "does not fail when evicted version does not exist"() {
        mavenRepo.module("org", "external", "1.4").publish()
        mavenRepo.module("org", "dep", "2.2").dependsOn("org", "external", "1.4").publish()

        buildFile << """
repositories {
    maven { url "${mavenRepo.uri}" }
}

configurations { compile }

dependencies {
    compile 'org:external:1.2'
    compile 'org:dep:2.2'
}

task checkDeps << {
    assert configurations.compile*.name == ['dep-2.2.jar', 'external-1.4.jar']
}
"""

        expect:
        run("checkDeps")
    }

    void "takes newest dynamic version when dynamic version forced"() {
        mavenRepo.module("org", "foo", '1.3.0').publish()

        mavenRepo.module("org", "foo", '1.4.1').publish()
        mavenRepo.module("org", "foo", '1.4.4').publish()
        mavenRepo.module("org", "foo", '1.4.9').publish()

        mavenRepo.module("org", "foo", '1.6.0').publish()

        settingsFile << "include 'api', 'impl', 'tool'"

        buildFile << """
allprojects {
	apply plugin: 'java'
	repositories {
		maven { url "${mavenRepo.uri}" }
	}
}

project(':api') {
	dependencies {
		compile 'org:foo:1.4.4'
	}
}

project(':impl') {
	dependencies {
		compile 'org:foo:1.4.1'
	}
}

project(':tool') {

	dependencies {
		compile project(':api'), project(':impl'), 'org:foo:1.3.0'
	}

	configurations.all {
	    resolutionStrategy {
	        force 'org:foo:1.4+'
	        failOnVersionConflict()
	    }
	}

	task checkDeps << {
        assert configurations.compile*.name.contains('foo-1.4.9.jar')
    }
}

"""

        expect:
        run("tool:checkDeps")
    }

    void "parent pom does not participate in forcing mechanism"() {
        mavenRepo.module("org", "foo", '1.3.0').publish()
        mavenRepo.module("org", "foo", '2.4.0').publish()

        def parent = mavenRepo.module("org", "someParent", "1.0")
        parent.type = 'pom'
        parent.dependsOn("org", "foo", "1.3.0")
        parent.publish()

        def otherParent = mavenRepo.module("org", "someParent", "2.0")
        otherParent.type = 'pom'
        otherParent.dependsOn("org", "foo", "2.4.0")
        otherParent.publish()

        def module = mavenRepo.module("org", "someArtifact", '1.0')
        module.parentPomSection = """
<parent>
  <groupId>org</groupId>
  <artifactId>someParent</artifactId>
  <version>1.0</version>
</parent>
"""
        module.publish()

        buildFile << """
apply plugin: 'java'
repositories {
    maven { url "${mavenRepo.uri}" }
}

dependencies {
    compile 'org:someArtifact:1.0'
}

configurations.all {
    resolutionStrategy {
        force 'org:someParent:2.0'
        failOnVersionConflict()
    }
}

task checkDeps << {
    def deps = configurations.compile*.name
    assert deps.contains('someArtifact-1.0.jar')
    assert deps.contains('foo-1.3.0.jar')
    assert deps.size() == 2
}
"""

        expect:
        run("checkDeps")
    }

    void "previously evicted nodes should contain correct target version"() {
        /*
        a1->b1
        a2->b2->a1

        resolution process:

        1. stop resolution, resolve conflict a1 vs a2
        2. select a2, restart resolution
        3. stop, resolve b1 vs b2
        4. select b2, restart
        5. resolve b2 dependencies, a1 has been evicted previously but it should show correctly on the report
           ('dependencies' report pre 1.2 would not show the a1 dependency leaf for this scenario)
        */

        ivyRepo.module("org", "b", '1.0').publish()
        ivyRepo.module("org", "a", '1.0').dependsOn("org", "b", '1.0').publish()
        ivyRepo.module("org", "b", '2.0').dependsOn("org", "a", "1.0").publish()
        ivyRepo.module("org", "a", '2.0').dependsOn("org", "b", '2.0').publish()

        buildFile << """
            repositories {
                ivy { url "${ivyRepo.uri}" }
            }

            configurations {
                conf
            }
            dependencies {
                conf 'org:a:1.0', 'org:a:2.0'
            }
            task checkDeps << {
                assert configurations.conf*.name == ['a-2.0.jar', 'b-2.0.jar']
                def result = configurations.conf.incoming.resolutionResult
                assert result.allModuleVersions.size() == 3
                def root = result.root
                assert root.dependencies*.toString() == ['org:a:1.0 -> org:a:2.0', 'org:a:2.0']
                def a = result.allModuleVersions.find { it.id.name == 'a' }
                assert a.dependencies*.toString() == ['org:b:2.0']
                def b = result.allModuleVersions.find { it.id.name == 'b' }
                assert b.dependencies*.toString() == ['org:a:1.0 -> org:a:2.0']
            }
        """

        expect:
        run("checkDeps")
    }

    @Issue("GRADLE-2555")
    void "can deal with transitive with parent in conflict"() {
        /*
            Graph looks like…

            \--- org:a:1.0
                 \--- org:in-conflict:1.0 -> 2.0
                      \--- org:target:1.0
                           \--- org:target-child:1.0
            \--- org:b:1.0
                 \--- org:b-child:1.0
                      \--- org:in-conflict:2.0 (*)

            This is the simplest structure I could boil it down to that produces the error.
            - target *must* have a child
            - Having "b" depend directly on "in-conflict" does not produce the error, needs to go through "b-child"
         */

        mavenRepo.module("org", "target-child", "1.0").publish()
        mavenRepo.module("org", "target", "1.0").dependsOn("org", "target-child", "1.0").publish()
        mavenRepo.module("org", "in-conflict", "1.0").dependsOn("org", "target", "1.0").publish()
        mavenRepo.module("org", "in-conflict", "2.0").dependsOn("org", "target", "1.0").publish()

        mavenRepo.module("org", "a", '1.0').dependsOn("org", "in-conflict", "1.0").publish()

        mavenRepo.module("org", "b-child", '1.0').dependsOn("org", "in-conflict", "2.0").publish()

        mavenRepo.module("org", "b", '1.0').dependsOn("org", "b-child", "1.0").publish()

        buildFile << """
            repositories { maven { url "${mavenRepo.uri}" } }

            configurations { conf }

            dependencies {
                conf "org:a:1.0", "org:b:1.0"
            }

        task checkDeps << {
            assert configurations.conf*.name == ['a-1.0.jar', 'b-1.0.jar', 'target-child-1.0.jar', 'target-1.0.jar', 'in-conflict-2.0.jar', 'b-child-1.0.jar']
            def result = configurations.conf.incoming.resolutionResult
            assert result.allModuleVersions.size() == 7
            def a = result.allModuleVersions.find { it.id.name == 'a' }
            assert a.dependencies*.toString() == ['org:in-conflict:1.0 -> org:in-conflict:2.0']
            def bChild = result.allModuleVersions.find { it.id.name == 'b-child' }
            assert bChild.dependencies*.toString() == ['org:in-conflict:2.0']
            def target = result.allModuleVersions.find { it.id.name == 'target' }
            assert target.dependents*.from*.toString() == ['org:in-conflict:2.0']
        }
        """

        expect:
        run("checkDeps")
    }

    @Issue("GRADLE-2555")
    void "batched up conflicts with conflicted parent and child"() {
        /*
        Dependency tree:

        a->c1
        b->c2->x1
        d->x1
        f->x2

        Everything is resolvable but not x2

        Scenario:
         - We have batched up conflicts
         - parent of one conflicted version is also conflicted
         - conflicted parent is positioned on the conflicts queue after the conflicted child (the order of declaring dependencies matters)
         - winning parent depends on a child that previously was evicted
         - finally, the winning child is an unresolved dependency
        */
        mavenRepo.module("org", "c", '1.0').publish()
        mavenRepo.module("org", "x", '1.0').publish()
        mavenRepo.module("org", "c", '2.0').dependsOn("org", "x", '1.0').publish()
        mavenRepo.module("org", "a").dependsOn("org", "c", "1.0").publish()
        mavenRepo.module("org", "b").dependsOn("org", "c", "2.0").publish()
        mavenRepo.module("org", "d").dependsOn("org", "x", "1.0").publish()
        mavenRepo.module("org", "f").dependsOn("org", "x", "2.0").publish()

        buildFile << """
            repositories { maven { url "${mavenRepo.uri}" } }
            configurations {
                childFirst
                parentFirst
            }
            dependencies {
                //conflicted child is resolved first
                childFirst 'org:d:1.0', 'org:f:1.0', 'org:a:1.0', 'org:b:1.0'
                //conflicted parent is resolved first
                parentFirst 'org:a:1.0', 'org:b:1.0', 'org:d:1.0', 'org:f:1.0'
            }
        """

        when:
        run("dependencies")

        then:
        output.contains(toPlatformLineSeparators("""
childFirst
+--- org:d:1.0
|    \\--- org:x:1.0 -> 2.0 FAILED
+--- org:f:1.0
|    \\--- org:x:2.0 FAILED
+--- org:a:1.0
|    \\--- org:c:1.0 -> 2.0
|         \\--- org:x:1.0 -> 2.0 FAILED
\\--- org:b:1.0
     \\--- org:c:2.0 (*)

parentFirst
+--- org:a:1.0
|    \\--- org:c:1.0 -> 2.0
|         \\--- org:x:1.0 -> 2.0 FAILED
+--- org:b:1.0
|    \\--- org:c:2.0 (*)
+--- org:d:1.0
|    \\--- org:x:1.0 -> 2.0 FAILED
\\--- org:f:1.0
     \\--- org:x:2.0 FAILED"""))
    }

    @Issue("GRADLE-2738")
    @Ignore("Not yet implemented")
    def "incorrect resolution of dynamic versions"() {
        given:
        //only 1.5 published:
        mavenRepo.module("org", "leaf", "1.5").publish()

        //problematic dynamic constraint:
        mavenRepo.module("org", "c", "1.0").dependsOn("org", "leaf", "2.0+").publish()

        //other participants of conflict resolution. Commenting one of them makes Gradle behave correctly for this scenario (!).
        mavenRepo.module("org", "a", "1.0").dependsOn("org", "leaf", "1.0").publish()
        mavenRepo.module("org", "b", "1.0").dependsOn("org", "leaf", "[1.5,1.9]").publish()


        file("build.gradle") << """
            repositories {
                maven { url "${mavenRepo.uri}" }
            }
            configurations {
                conf
            }
            dependencies {
                conf 'org:a:1.0', 'org:b:1.0', 'org:c:1.0'
            }
            task resolve << {
                configurations.conf.files
            }
        """

        when:
        run "resolve"

        then:
        //needs more assertions. This should fail with resolution failure but it passes.
        thrown(GradleException)
    }
}
