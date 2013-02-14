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

package org.gradle.test.fixtures.ivy

class IvyDescriptorDependencyConfiguration {
    final dependencies = []

    void addDependency(String org, String module, String revision) {
        dependencies << [org: org, module: module, revision: revision]
    }

    void assertDependsOn(String org, String module, String revision) {
        def dep = [org: org, module: module, revision: revision]
        if (!dependencies.find { it == dep}) {
            throw new AssertionError("Could not find expected dependency $dep. Actual: $dependencies")
        }
    }

    void assertDependsOnModules(String... modules) {
        assert dependencies.collect { it.module } as Set == modules as Set
    }
}
