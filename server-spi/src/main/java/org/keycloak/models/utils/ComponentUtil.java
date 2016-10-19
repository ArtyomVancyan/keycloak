/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.utils;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ComponentUtil {

    public static Map<String, ProviderConfigProperty> getComponentConfigProperties(KeycloakSession session, ComponentRepresentation component) {
        return getComponentConfigProperties(session, component.getProviderType(), component.getProviderId());
    }

    public static Map<String, ProviderConfigProperty> getComponentConfigProperties(KeycloakSession session, ComponentModel component) {
        return getComponentConfigProperties(session, component.getProviderType(), component.getProviderId());
    }

    public static ComponentFactory getComponentFactory(KeycloakSession session, ComponentRepresentation component) {
        return getComponentFactory(session, component.getProviderType(), component.getProviderId());
    }

    public static ComponentFactory getComponentFactory(KeycloakSession session, ComponentModel component) {
        return getComponentFactory(session, component.getProviderType(), component.getProviderId());
    }

    private static Map<String, ProviderConfigProperty> getComponentConfigProperties(KeycloakSession session, String providerType, String providerId) {
        try {
            List<ProviderConfigProperty> l = getComponentFactory(session, providerType, providerId).getConfigProperties();
            Map<String, ProviderConfigProperty> properties = new HashMap<>();
            for (ProviderConfigProperty p : l) {
                properties.put(p.getName(), p);
            }
            return properties;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ComponentFactory getComponentFactory(KeycloakSession session, String providerType, String providerId) {
        Class<? extends Provider> provider = session.getProviderClass(providerType);
        if (provider == null) {
            throw new RuntimeException("Invalid provider type '" + providerType + "'");
        }

        ProviderFactory<? extends Provider> f = session.getKeycloakSessionFactory().getProviderFactory(provider, providerId);
        if (f == null) {
            throw new RuntimeException("No such provider '" + providerId + "'");
        }

        ComponentFactory cf = (ComponentFactory) f;
        return cf;
    }

}
