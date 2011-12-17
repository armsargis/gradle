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
package org.gradle.api.internal.notations.parsers

import spock.lang.Specification
import org.gradle.api.internal.notations.api.UnsupportedNotationException
import org.gradle.api.InvalidUserDataException

class MapNotationParserTest extends Specification {
    final DummyParser parser = new DummyParser()
    
    def "parses map with required keys"() {
        expect:
        def object = parser.parseNotation([name: 'name', version: 'version'])
        object.key1 == 'name'
        object.key2 == 'version'
        object.prop1 == null
    }

    def "parses map with required and optional keys"() {
        expect:
        def object = parser.parseNotation([name: 'name', version: 'version', optional: '1.2'])
        object.key1 == 'name'
        object.key2 == 'version'
        object.optional == '1.2'
        object.prop1 == null
    }

    def "configures properties of converted object using extra keys"() {
        expect:
        def object = parser.parseNotation([name: 'name', version: 'version', prop1: 'prop1', optional: '1.2'])
        object.key1 == 'name'
        object.key2 == 'version'
        object.prop1 == 'prop1'
    }

    def "does not parse map with missing keys"() {
        when:
        parser.parseNotation([name: 'name'])

        then:
        InvalidUserDataException e = thrown()
        e.message == 'Required keys [version] are missing from map {name=name}.'
    }

    def "does not parse notation that is not a map"() {
        when:
        parser.parseNotation('string')

        then:
        thrown(UnsupportedNotationException)
    }
    
    static class DummyParser extends MapNotationParser<TargetObject> {
        @Override
        protected Collection<String> getRequiredKeys() {
            return ['name', 'version']
        }

        @Override
        protected Collection<String> getOptionalKeys() {
            return ['optional']
        }

        @Override
        protected TargetObject parseMap(Map<String, Object> values) {
            return new TargetObject(key1:  values.name, key2:  values.version, optional:  values.optional)
        }
    }

    static class TargetObject {
        String key1;
        String key2;
        String optional;
        String prop1;
    }
}
