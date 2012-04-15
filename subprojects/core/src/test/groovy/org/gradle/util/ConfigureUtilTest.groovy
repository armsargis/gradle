/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.util

import org.gradle.api.internal.ThreadGlobalInstantiator
import org.gradle.util.ConfigureUtil.IncompleteInputException
import org.junit.Test
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.sameInstance
import static org.junit.Assert.assertThat
import static org.junit.Assert.fail

class ConfigureUtilTest {
    @Test
    public void canConfigureObjectUsingClosure() {
        List obj = []
        def cl = {
            add('a');
            assertThat(size(), equalTo(1));
            assertThat(obj, equalTo(['a']))
        }
        ConfigureUtil.configure(cl, obj)
        assertThat(obj, equalTo(['a']))
    }

    @Test
    public void passesConfiguredObjectToClosureAsParameter() {
        List obj = []
        def cl = {
            assertThat(it, sameInstance(obj))
        }
        def cl2 = {List list ->
            assertThat(list, sameInstance(obj))
        }
        def cl3 = {->
            assertThat(delegate, sameInstance(obj))
        }
        ConfigureUtil.configure(cl, obj)
        ConfigureUtil.configure(cl2, obj)
        ConfigureUtil.configure(cl3, obj)
    }

    @Test
    public void canConfigureObjectPropertyUsingMap() {
        Bean obj = new Bean()

        ConfigureUtil.configureByMap(obj, prop: 'value')
        assertThat(obj.prop, equalTo('value'))

        ConfigureUtil.configureByMap(obj, method: 'value2')
        assertThat(obj.prop, equalTo('value2'))
    }

    @Test
    public void canConfigureAndValidateObjectUsingMap() {
        Bean obj = new Bean()

        try {
            //when
            ConfigureUtil.configureByMap([prop: 'value'], obj, ['foo'])
            //then
            fail();
        } catch (IncompleteInputException e) {
            assert e.missingKeys.contains('foo')
        }

        //when
        ConfigureUtil.configureByMap([prop: 'value'], obj, ['prop'])
        //then
        assert obj.prop == 'value'
    }

    @Test
    public void throwsExceptionForUnknownProperty() {
        Bean obj = new Bean()

        try {
            ConfigureUtil.configureByMap(obj, unknown: 'value')
            fail()
        } catch (MissingPropertyException e) {
            assertThat(e.type, equalTo(Bean.class))
            assertThat(e.property, equalTo('unknown'))
        }
    }
    
    static class TestConfigurable implements Configurable {
        def props = [:]
        
        TestConfigurable configure(Closure closure) {
            props.with(closure)
            this
        }
    }
    
    @Test
    void testConfigurableAware() {
        def c = new TestConfigurable()
        ConfigureUtil.configure({ a = 1 }, c)
        assert c.props.a == 1
    }
    
    @Test
    void configureByMapTriesMethodForExtensibleObjects() {
        Bean bean = ThreadGlobalInstantiator.getOrCreate().newInstance(Bean)
        ConfigureUtil.configureByMap(bean, method:  "foo")
        assert bean.prop == "foo"
    }
    
}

class Bean {
    String prop
    def method(String value) {
        prop = value
    }
}
