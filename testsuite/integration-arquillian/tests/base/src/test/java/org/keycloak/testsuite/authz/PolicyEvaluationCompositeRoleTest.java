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
package org.keycloak.testsuite.authz;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyEvaluationCompositeRoleTest extends AbstractKeycloakTest {
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    public static void setup(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        ClientModel client = session.realms().addClient(realm, "myclient");
        RoleModel role1 = client.addRole("client-role1");


        AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().create(client.getId());
        Policy policy = createRolePolicy(authz, resourceServer, role1);

        Scope scope = authz.getStoreFactory().getScopeStore().create("myscope", resourceServer);
        Resource resource = authz.getStoreFactory().getResourceStore().create("myresource", resourceServer, resourceServer.getClientId());
        addScopePermission(authz, resourceServer, "mypermission", resource, scope, policy);

        RoleModel composite = realm.addRole("composite");
        composite.addCompositeRole(role1);

        UserModel user = session.users().addUser(realm, "user");
        user.grantRole(composite);
    }

    private static Policy addScopePermission(AuthorizationProvider authz, ResourceServer resourceServer, String name, Resource resource, Scope scope, Policy policy) {
        Policy permission = authz.getStoreFactory().getPolicyStore().create(name, "scope", resourceServer);
        String resources = "[\"" + resource.getId() + "\"]";
        String scopes = "[\"" + scope.getId() + "\"]";
        String applyPolicies = "[\"" + policy.getId() + "\"]";
        Map<String, String> config = new HashMap<>();
        config.put("resources", resources);
        config.put("scopes", scopes);
        config.put("applyPolicies", applyPolicies);
        permission.setConfig(config);
        permission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        permission.setLogic(Logic.POSITIVE);
        permission.addResource(resource);
        permission.addScope(scope);
        permission.addAssociatedPolicy(policy);
        return permission;
    }


    private static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role) {
        Policy policy = authz.getStoreFactory().getPolicyStore().create(role.getName(), "role", resourceServer);

        String roleValues = "[{\"id\":\"" + role.getId() + "\",\"required\": true}]";
        policy.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        policy.setLogic(Logic.POSITIVE);
        Map<String, String> config = new HashMap<>();
        config.put("roles", roleValues);
        policy.setConfig(config);
        return policy;
    }


    @Test
    public void testCreate() throws Exception {
        testingClient.server().run(PolicyEvaluationCompositeRoleTest::setup);

        RealmResource realm = adminClient.realm(TEST);
        String resourceServerId = realm.clients().findByClientId("myclient").get(0).getId();
        UserRepresentation user = realm.users().search("user").get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource("myresource", "myscope");
        PolicyEvaluationResponse result = realm.clients().get(resourceServerId).authorization().policies().evaluate(request);
        Assert.assertEquals(result.getStatus(), DecisionEffect.PERMIT);
    }


}
