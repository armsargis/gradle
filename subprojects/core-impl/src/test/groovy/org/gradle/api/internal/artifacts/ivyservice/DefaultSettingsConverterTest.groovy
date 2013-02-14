/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.gradle.internal.Factory
import spock.lang.Specification

class DefaultSettingsConverterTest extends Specification {
    final DependencyResolver defaultResolver = Mock()
    final IBiblioResolver testResolver = new IBiblioResolver()
    final IBiblioResolver testResolver2 = new IBiblioResolver()

    File testGradleUserHome = new File('gradleUserHome')

    final Factory<IvySettings> ivySettingsFactory = Mock()
    final IvySettings ivySettings = new IvySettings()

    DefaultSettingsConverter converter = new DefaultSettingsConverter(ivySettingsFactory)

    public void setup() {
        testResolver.name = 'resolver'
    }

    public void testConvertForResolve() {
        when:
        IvySettings settings = converter.convertForResolve(defaultResolver, [testResolver, testResolver2])

        then:
        1 * ivySettingsFactory.create() >> ivySettings
        1 * defaultResolver.setSettings(ivySettings)
        _ * defaultResolver.getName() >> 'default'
        0 * _._

        assert settings.is(ivySettings)

        assert settings.defaultResolver == defaultResolver
        assert settings.resolvers.size() == 3
        [testResolver, testResolver2].each { resolver ->
            assert settings.resolvers.any { it == resolver }
            assert settings.getResolver(resolver.name) == resolver
            assert settings == resolver.settings
            assert settings == resolver.repositoryCacheManager.settings
        }
    }

    public void shouldReuseResolveSettings() {
        when:
        IvySettings settings = converter.convertForResolve(defaultResolver, [testResolver, testResolver2])

        then:
        1 * ivySettingsFactory.create() >> ivySettings
        1 * defaultResolver.setSettings(ivySettings)
        _ * defaultResolver.getName() >> 'default'
        0 * _._

        assert settings.is(ivySettings)

        [testResolver, testResolver2].each { resolver ->
            assert settings.resolvers.any { it == resolver }
        }

        when:
        settings = converter.convertForResolve(defaultResolver, [testResolver])

        then:
        assert settings.is(ivySettings)

        assert settings.defaultResolver == defaultResolver
        assert settings.resolvers.size() == 2
        [testResolver].each { resolver ->
             assert settings.resolvers.any { it == resolver }
             assert settings.getResolver(resolver.name) == resolver
             assert settings == resolver.settings
             assert settings == resolver.repositoryCacheManager.settings
         }
    }

    public void testConvertForPublish() {
        when:
        IvySettings settings = converter.convertForPublish([testResolver, testResolver2])

        then:
        settings.is(ivySettings)

        and:
        [testResolver, testResolver2].each {
            it.settings == settings
            it.repositoryCacheManager.settings == settings
        }

        and:
        1 * ivySettingsFactory.create() >> ivySettings
        0 * _._
    }

    public void reusesPublishSettings() {
        when:
        IvySettings settings = converter.convertForPublish([testResolver])

        then:
        settings.is(ivySettings)

        and:
        1 * ivySettingsFactory.create() >> ivySettings
        0 * _._

        when:
        settings = converter.convertForPublish([testResolver, testResolver2])

        then:
        settings.is(ivySettings)

        and:
            [testResolver, testResolver2].each {
            it.settings == settings
            it.repositoryCacheManager.settings == settings
        }
    }
}
