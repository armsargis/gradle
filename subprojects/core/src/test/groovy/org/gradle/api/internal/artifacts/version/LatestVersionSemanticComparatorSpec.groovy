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

package org.gradle.api.internal.artifacts.version

import spock.lang.Specification

/**
 * by Szczepan Faber, created at: 10/9/12
 */
class LatestVersionSemanticComparatorSpec extends Specification {

    private comparator = new LatestVersionSemanticComparator()

    def "compares versions"() {
        expect:
        comparator.compare(a, b) < 0
        comparator.compare(b, a) > 0
        comparator.compare(a, a) == 0
        comparator.compare(b, b) == 0

        where:
        a                   | b
        '1.0'               | '2.0'
        '1.2'               | '1.10'
        '1.0'               | '1.0.1'
        '1.0-rc-1'          | '1.0-rc-2'
        '1.0-alpha'         | '1.0'
        '1.0-alpha'         | '1.0-beta'
        '1.0-1'             | '1.0-2'
        '1.0.a'             | '1.0.b'
        '1.0.alpha'         | '1.0.b'
    }

    def "equal"() {
        expect:
        comparator.compare(a, b) == 0
        comparator.compare(b, a) == 0

        //some of the comparison are not working hence commented out.
        //consider updating the implementation when we port the ivy comparison mechanism.
        where:
        a                   | b
        '1.0'               | '1.0'
        '5.0'               | '5.0'
//        '1.0.0'             | '1.0'
//        '1.0.0'             | '1'
//        '1.0-alpha'         | '1.0-ALPHA'
//        '1.0.alpha'         | '1.0-alpha'
    }

    def "not equal"() {
        expect:
        comparator.compare(a, b) != 0
        comparator.compare(b, a) != 0

        where:
        a                   | b
        '1.0'               | ''
        '1.0'               | null
        '1.0'               | 'hey joe'
    }
}
