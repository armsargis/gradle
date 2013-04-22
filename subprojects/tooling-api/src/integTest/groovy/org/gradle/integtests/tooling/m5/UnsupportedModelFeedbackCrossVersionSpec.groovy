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

package org.gradle.integtests.tooling.m5

import org.gradle.integtests.tooling.fixture.MaxTargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.tooling.UnknownModelException
import org.gradle.tooling.model.idea.BasicIdeaProject
import org.gradle.tooling.model.idea.IdeaProject

@MaxTargetGradleVersion('1.0-milestone-4')
class UnsupportedModelFeedbackCrossVersionSpec extends ToolingApiSpecification {
    def "fails gracefully when unsupported model requested"() {
        when:
        maybeFailWithConnection { it.getModel(model) }

        then:
        UnknownModelException e = thrown()
        e.message.contains(model.simpleName)

        where:
        model << [IdeaProject, BasicIdeaProject]
    }
}
