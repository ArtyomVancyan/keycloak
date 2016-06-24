/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * This class provides loading of the testRealm called "test".  It also
 * provides a few utility methods for the testRealm.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class TestRealmKeycloakTest extends AbstractKeycloakTest {

    protected RealmResource testRealm() {
        return adminClient.realm("test");
    }

    protected UserRepresentation findUser(String userNameOrEmail) {
        List<UserRepresentation> repList = testRealm().users().search(userNameOrEmail, -1, -1);
        if (repList.size() != 1) throw new IllegalStateException("User search expected one result. Found " + repList.size() + " users.");
        return repList.get(0);
    }

    protected void updateUser(UserRepresentation user) {
        testRealm().users().get(user.getId()).update(user);
    }

    protected ClientRepresentation findTestApp(RealmRepresentation testRealm) {
        for (ClientRepresentation client : testRealm.getClients()) {
            if (client.getClientId().equals("test-app")) return client;
        }

        return null;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        testRealms.add(testRealm);

        configureTestRealm(testRealm);
    }

    /**
     * This allows a subclass to change the configuration of the testRealm before
     * it is imported.  This method will be called prior to any @Before methods
     * in the subclass.
     *
     * @param testRealm The realm read from /testrealm.json.
     */
    public abstract void configureTestRealm(RealmRepresentation testRealm);

}
