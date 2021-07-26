/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_ONLY;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.account.UserProfileAttributeMetadata;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;

/**
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class AccountRestServiceWithUserProfileTest extends AccountRestServiceTest {
    
    @Override
    @Before
    public void before() {
        super.before();
        enableDynamicUserProfile();
        setUserProfileConfiguration(null);
    }

    @Override
    protected boolean isDeclarativeUserProfile() {
        return true;
    }

    private static String UP_CONFIG_FOR_METADATA = "{\"attributes\": ["
            + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {\"scopes\":[\"profile\"]}, \"displayName\": \"${profile.firstName}\", \"validations\": {\"length\": { \"max\": 255 }}},"
            + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}, \"displayName\": \"Last name\", \"annotations\": {\"formHintKey\" : \"userEmailFormFieldHint\", \"anotherKey\" : 10, \"yetAnotherKey\" : \"some value\"}},"
            + "{\"name\": \"attr_with_scope_selector\"," + PERMISSIONS_ALL + ", \"selector\": {\"scopes\": [\"profile\"]}},"
            + "{\"name\": \"attr_required\"," + PERMISSIONS_ALL + ", \"required\": {}},"
            + "{\"name\": \"attr_required_by_role\"," + PERMISSIONS_ALL + ", \"required\": {\"roles\" : [\"user\"]}},"
            + "{\"name\": \"attr_required_by_scope\"," + PERMISSIONS_ALL + ", \"required\": {\"scopes\": [\"profile\"]}},"
            + "{\"name\": \"attr_not_required_due_to_role\"," + PERMISSIONS_ALL + ", \"required\": {\"roles\" : [\"admin\"]}},"
            + "{\"name\": \"attr_readonly\"," + PERMISSIONS_ADMIN_EDITABLE + "},"
            + "{\"name\": \"attr_no_permission\"," + PERMISSIONS_ADMIN_ONLY + "}"
            + "]}"; 
    
    @Test
    @Override
    public void testGetUserProfileMetadata_EditUsernameAllowed() throws IOException {

        setUserProfileConfiguration(UP_CONFIG_FOR_METADATA);
        
        UserRepresentation user = getUser();
        assertNotNull(user.getUserProfileMetadata());
        
        assertUserProfileAttributeMetadata(user, "username", "${username}", true, false);
        assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);
        
        UserProfileAttributeMetadata uam = assertUserProfileAttributeMetadata(user, "firstName", "${profile.firstName}", false, false);
        assertNull(uam.getAnnotations());
        Map<String, Object> vc = assertValidatorExists(uam, "length");
        assertEquals(255, vc.get("max"));
        
        uam = assertUserProfileAttributeMetadata(user, "lastName", "Last name", true, false);
        assertNotNull(uam.getAnnotations());
        assertEquals(3, uam.getAnnotations().size());
        assertAnnotationValue(uam, "formHintKey", "userEmailFormFieldHint");
        assertAnnotationValue(uam, "anotherKey", 10);
        
        assertUserProfileAttributeMetadata(user, "attr_with_scope_selector", "attr_with_scope_selector", false, false);
        
        assertUserProfileAttributeMetadata(user, "attr_required", "attr_required", true, false);
        assertUserProfileAttributeMetadata(user, "attr_required_by_role", "attr_required_by_role", true, false);
        
        assertUserProfileAttributeMetadata(user, "attr_required_by_scope", "attr_required_by_scope", false, false);
        
        assertUserProfileAttributeMetadata(user, "attr_not_required_due_to_role", "attr_not_required_due_to_role", false, false);
        assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);
        
        assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));
    }
    
    @Test
    @Override
    public void testGetUserProfileMetadata_EditUsernameDisallowed() throws IOException {
        
        try {
            RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
            realmRep.setEditUsernameAllowed(false);
            adminClient.realm("test").update(realmRep);

            setUserProfileConfiguration(UP_CONFIG_FOR_METADATA);
            
            UserRepresentation user = getUser();
            assertNotNull(user.getUserProfileMetadata());
            
            assertUserProfileAttributeMetadata(user, "username", "${username}", true, true);
            assertUserProfileAttributeMetadata(user, "email", "${email}", true, false);
            
            UserProfileAttributeMetadata uam = assertUserProfileAttributeMetadata(user, "firstName", "${profile.firstName}", false, false);
            assertNull(uam.getAnnotations());
            Map<String, Object> vc = assertValidatorExists(uam, "length");
            assertEquals(255, vc.get("max"));
            
            uam = assertUserProfileAttributeMetadata(user, "lastName", "Last name", true, false);
            assertNotNull(uam.getAnnotations());
            assertEquals(3, uam.getAnnotations().size());
            assertAnnotationValue(uam, "formHintKey", "userEmailFormFieldHint");
            assertAnnotationValue(uam, "anotherKey", 10);
            
            assertUserProfileAttributeMetadata(user, "attr_with_scope_selector", "attr_with_scope_selector", false, false);
            
            assertUserProfileAttributeMetadata(user, "attr_required", "attr_required", true, false);
            assertUserProfileAttributeMetadata(user, "attr_required_by_role", "attr_required_by_role", true, false);
            
            assertUserProfileAttributeMetadata(user, "attr_required_by_scope", "attr_required_by_scope", false, false);
            
            assertUserProfileAttributeMetadata(user, "attr_not_required_due_to_role", "attr_not_required_due_to_role", false, false);
            assertUserProfileAttributeMetadata(user, "attr_readonly", "attr_readonly", false, true);
            
            assertNull(getUserProfileAttributeMetadata(user, "attr_no_permission"));
        } finally {
            RealmRepresentation realmRep = testRealm().toRepresentation();
            realmRep.setEditUsernameAllowed(true);
            testRealm().update(realmRep);
        }
    }
    
    protected void assertAnnotationValue(UserProfileAttributeMetadata uam, String key, Object value) {
        assertNotNull("Missing annotations for attribute " + uam.getName(), uam.getAnnotations());
        assertEquals("Unexpexted value of the "+key+" annotation for attribute " + uam.getName(), value, uam.getAnnotations().get(key));
    }

    protected Map<String, Object> assertValidatorExists(UserProfileAttributeMetadata uam, String validatorId) {
        assertNotNull("Missing validators for attribute " + uam.getName(), uam.getValidators());
        assertTrue("Missing validtor "+validatorId+" for attribute " + uam.getName(), uam.getValidators().containsKey(validatorId));
        return uam.getValidators().get(validatorId);
    }
    
    @Test
    @Override
    public void testUpdateProfile() throws IOException {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"attr1\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"," + PERMISSIONS_ALL + "}"
                + "]}");
        super.testUpdateProfile();
    }
    
    @Test
    @Override
    public void testUpdateSingleField() throws IOException {
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}}"
                + "]}");
         super.testUpdateSingleField();
    }
    
    protected void setUserProfileConfiguration(String configuration) {
        VerifyProfileTest.setUserProfileConfiguration(testRealm(), configuration);
    }
   
    protected void enableDynamicUserProfile() {
        RealmRepresentation testRealm = testRealm().toRepresentation();
        
        VerifyProfileTest.enableDynamicUserProfile(testRealm);

        testRealm().update(testRealm);
    }

}
