/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.authorization.infinispan.entities;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResourceServer implements ResourceServer {

    private final String id;
    private String clientId;
    private boolean allowRemoteResourceManagement;
    private PolicyEnforcementMode policyEnforcementMode;

    public CachedResourceServer(ResourceServer resourceServer) {
        this.id = resourceServer.getId();
        this.clientId = resourceServer.getClientId();
        this.allowRemoteResourceManagement = resourceServer.isAllowRemoteResourceManagement();
        this.policyEnforcementMode = resourceServer.getPolicyEnforcementMode();
    }

    public CachedResourceServer(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getClientId() {
        return this.clientId;
    }

    @Override
    public boolean isAllowRemoteResourceManagement() {
        return this.allowRemoteResourceManagement;
    }

    @Override
    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        this.allowRemoteResourceManagement = allowRemoteResourceManagement;
    }

    @Override
    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return this.policyEnforcementMode;
    }

    @Override
    public void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode) {
        this.policyEnforcementMode = enforcementMode;
    }
}
