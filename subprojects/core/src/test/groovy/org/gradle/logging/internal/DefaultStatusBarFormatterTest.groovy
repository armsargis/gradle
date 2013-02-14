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

package org.gradle.logging.internal

import spock.lang.Specification
import org.gradle.internal.nativeplatform.console.ConsoleMetaData

class DefaultStatusBarFormatterTest extends Specification {

    ConsoleMetaData consoleMetaData = Mock()
    private final StatusBarFormatter statusBarFormatter = new DefaultStatusBarFormatter(consoleMetaData)

    def "formats multiple operations"(){
        expect:
        "> status1 > status2" == statusBarFormatter.format(Arrays.asList(new ConsoleBackedProgressRenderer.Operation("shortDescr1", "status1"), new ConsoleBackedProgressRenderer.Operation("shortDescr2", "status2")))
    }

    def "uses shortDescr if no status available"(){
        expect:
        "> shortDescr1" == statusBarFormatter.format(Arrays.asList(new ConsoleBackedProgressRenderer.Operation("shortDescr1", null)))
        "> shortDescr2" == statusBarFormatter.format(Arrays.asList(new ConsoleBackedProgressRenderer.Operation("shortDescr2", '')))
    }

    def "trims output to one less than the max console width"(){
        when:
        _ * consoleMetaData.getCols() >> 10
        then:
        "> these a" == statusBarFormatter.format(Arrays.asList(new ConsoleBackedProgressRenderer.Operation("shortDescr1", "these are more than 10 characters")))
    }
}
