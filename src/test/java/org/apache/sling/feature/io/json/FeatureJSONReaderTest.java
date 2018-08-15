/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.io.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;
import org.apache.sling.feature.KeyValueMap;
import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class FeatureJSONReaderTest {

    @Test public void testRead() throws Exception {
        final Feature feature = U.readFeature("test");
        assertNotNull(feature);
        assertNotNull(feature.getId());
        assertEquals("org.apache.sling", feature.getId().getGroupId());
        assertEquals("test-feature", feature.getId().getArtifactId());
        assertEquals("1.1", feature.getId().getVersion());
        assertEquals("jar", feature.getId().getType());
        assertNull(feature.getId().getClassifier());

        assertEquals(2, feature.getConfigurations().size());
        final Configuration cfg1 = U.findConfiguration(feature.getConfigurations(), "my.pid");
        assertEquals(7, cfg1.getProperties().get("number"));
        final Configuration cfg2 = U.findFactoryConfiguration(feature.getConfigurations(), "my.factory.pid", "name");
        assertEquals("yeah", cfg2.getProperties().get("a.value"));

        assertEquals(3, feature.getCapabilities().size());
        Capability capability = U.findCapability(feature.getCapabilities(),"osgi.service");
        assertNotNull(capability.getAttributes().get("objectClass"));

        assertEquals(Arrays.asList("org.osgi.service.http.runtime.HttpServiceRuntime"), capability.getAttributes().get("objectClass"));

    }

    @Test public void testReadWithVariablesResolve() throws Exception {
        final Feature feature = U.readFeature("test2");

        List<Include> includes = feature.getIncludes();
        assertEquals(1, includes.size());
        Include include = includes.get(0);
        assertEquals("org.apache.sling:sling:9", include.getId().toMvnId());

        List<Requirement> reqs = feature.getRequirements();
        Requirement req = reqs.get(0);
        assertEquals("osgi.contract", req.getNamespace());
        assertEquals("(&(osgi.contract=JavaServlet)(&(version>=3.0)(!(version>=4.0))))",
                req.getDirectives().get("filter"));

        List<Capability> caps = feature.getCapabilities();
        Capability cap = null;
        for (Capability c : caps) {
            if ("osgi.service".equals(c.getNamespace())) {
                cap = c;
                break;
            }
        }
        assertEquals(Collections.singletonList("org.osgi.service.http.runtime.HttpServiceRuntime"),
                cap.getAttributes().get("objectClass"));
        assertEquals("org.osgi.service.http.runtime",
                cap.getDirectives().get("uses"));
        // TODO this seems quite broken: fix!
        // assertEquals("org.osgi.service.http.runtime,org.osgi.service.http.runtime.dto",
        //        cap.getDirectives().get("uses"));

        KeyValueMap fwProps = feature.getFrameworkProperties();
        assertEquals("Framework property substitution should not happen at resolve time",
                "${something}", fwProps.get("brave"));

        Bundles bundles = feature.getBundles();
        ArtifactId id = new ArtifactId("org.apache.sling", "foo-xyz", "1.2.3", null, null);
        assertTrue(bundles.containsExact(id));
        ArtifactId id2 = new ArtifactId("org.apache.sling", "bar-xyz", "1.2.3", null, null);
        assertTrue(bundles.containsExact(id2));

        Configurations configurations = feature.getConfigurations();
        Configuration config = configurations.getConfiguration("my.pid2");
        Dictionary<String, Object> props = config.getProperties();
        assertEquals("Configuration substitution should not happen at resolve time",
                "aa${ab_config}", props.get("a.value"));
        assertEquals("${ab_config}bb", props.get("b.value"));
        assertEquals("c${c_config}c", props.get("c.value"));
    }

    @Test public void testReadRepoInitExtension() throws Exception {
        Feature feature = U.readFeature("repoinit");
        Extensions extensions = feature.getExtensions();
        assertEquals(1, extensions.size());
        Extension ext = extensions.iterator().next();
        assertEquals("some repo init\ntext", ext.getText());
    }

    @Test public void testReadRepoInitExtensionArray() throws Exception {
        Feature feature = U.readFeature("repoinit2");
        Extensions extensions = feature.getExtensions();
        assertEquals(1, extensions.size());
        Extension ext = extensions.iterator().next();
        assertEquals("[\"some repo init\",\"text\"]", ext.getJSON());
    }

    @Test public void testReadArtifactsExtensions() throws Exception {
        final Feature feature = U.readFeature("artifacts-extension");
        ArtifactsExtensions.testReadArtifactsExtensions(feature);
    }
}
