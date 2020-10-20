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

package org.keycloak.models;

import org.keycloak.provider.ProviderEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserModel extends RoleMapperModel {
    String USERNAME = "username";
    String FIRST_NAME = "firstName";
    String LAST_NAME = "lastName";
    String EMAIL = "email";
    String EMAIL_VERIFIED = "emailVerified";
    String LOCALE = "locale";
    String ENABLED = "enabled";
    String IDP_ALIAS = "keycloak.session.realm.users.query.idp_alias";
    String IDP_USER_ID = "keycloak.session.realm.users.query.idp_user_id";
    String INCLUDE_SERVICE_ACCOUNT = "keycloak.session.realm.users.query.include_service_account";
    String GROUPS = "keycloak.session.realm.users.query.groups";
    String SEARCH = "keycloak.session.realm.users.query.search";
    String EXACT = "keycloak.session.realm.users.query.exact";

    interface UserRemovedEvent extends ProviderEvent {
        RealmModel getRealm();
        UserModel getUser();
        KeycloakSession getKeycloakSession();
    }

    String getId();

    // No default method here to allow Abstract subclasses where the username is provided in a different manner
    String getUsername();

    // No default method here to allow Abstract subclasses where the username is provided in a different manner
    void setUsername(String username);

    /**
     * Get timestamp of user creation. May be null for old users created before this feature introduction.
     */
    Long getCreatedTimestamp();
    
    void setCreatedTimestamp(Long timestamp);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Set single value of specified attribute. Remove all other existing values of this attribute
     *
     * @param name
     * @param value
     */
    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    /**
     * @param name
     * @return null if there is not any value of specified attribute or first value otherwise. Don't throw exception if there are more values of the attribute
     */
    String getFirstAttribute(String name);

    /**
     * @param name
     * @return list of all attribute values or empty list if there are not any values. Never return null
     * @deprecated Use {@link #getAttributeStream(String) getAttributeStream} instead.
     */
    @Deprecated
    default List<String> getAttribute(String name) {
        return this.getAttributeStream(name).collect(Collectors.toList());
    }

    /**
     * Obtains all values associated with the specified attribute name.
     *
     * @param name the name of the attribute.
     * @return a non-null {@code Stream} of attribute values.
     */
    Stream<String> getAttributeStream(final String name);

    Map<String, List<String>> getAttributes();

    /**
     * @deprecated Use {@link #getRequiredActionsStream() getRequiredActionsStream} instead.
     */
    @Deprecated
    default Set<String> getRequiredActions() {
        return this.getRequiredActionsStream().collect(Collectors.toSet());
    }

    /**
     * Obtains the names of required actions associated with the user.
     *
     * @return a non-null {@code Stream} of required action names.
     */
    Stream<String> getRequiredActionsStream();

    void addRequiredAction(String action);

    void removeRequiredAction(String action);

    void addRequiredAction(RequiredAction action);

    void removeRequiredAction(RequiredAction action);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getEmail();

    void setEmail(String email);

    boolean isEmailVerified();

    void setEmailVerified(boolean verified);

    /**
     * @deprecated Use {@link #getGroupsStream() getGroupsStream} instead.
     */
    @Deprecated
    default Set<GroupModel> getGroups() {
        return getGroupsStream().collect(Collectors.toSet());
    }

    /**
     * Obtains the groups associated with the user.
     *
     * @return a non-null {@code Stream} of groups.
     */
    Stream<GroupModel> getGroupsStream();

    /**
     * @deprecated Use {@link #getGroupsStream(String, Integer, Integer) getGroupsStream} instead.
     */
    @Deprecated
    default Set<GroupModel> getGroups(int first, int max) {
        return getGroupsStream(null, first, max).collect(Collectors.toSet());
    }

    /**
     * @deprecated Use {@link #getGroupsStream(String, Integer, Integer) getGroupsStream} instead.
     */
    @Deprecated
    default Set<GroupModel> getGroups(String search, int first, int max) {
        return getGroupsStream(search, first, max)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns a paginated stream of groups within this realm with search in the name
     *
     * @param search Case insensitive string which will be searched for. Ignored if null.
     * @param first Index of first group to return. Ignored if negative or {@code null}.
     * @param max Maximum number of records to return. Ignored if negative or {@code null}.
     * @return Stream of desired groups.
     */
    default Stream<GroupModel> getGroupsStream(String search, Integer first, Integer max) {
        if (search != null) search = search.toLowerCase();
        final String finalSearch = search;
        Stream<GroupModel> groupModelStream = getGroupsStream()
                .filter(group -> finalSearch == null || group.getName().toLowerCase().contains(finalSearch));

        if (first != null && first > 0) {
            groupModelStream = groupModelStream.skip(first);
        }

        if (max != null && max >= 0) {
            groupModelStream = groupModelStream.limit(max);
        }

        return groupModelStream;
    }

    default long getGroupsCount() {
        return getGroupsCountByNameContaining(null);
    }
    
    default long getGroupsCountByNameContaining(String search) {
        if (search == null) {
            return getGroupsStream().count();
        }

        String s = search.toLowerCase();
        return getGroupsStream().filter(group -> group.getName().toLowerCase().contains(s)).count();
    }

    void joinGroup(GroupModel group);
    void leaveGroup(GroupModel group);
    boolean isMemberOf(GroupModel group);

    String getFederationLink();
    void setFederationLink(String link);

    String getServiceAccountClientLink();
    void setServiceAccountClientLink(String clientInternalId);

    enum RequiredAction {
        VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS
    }
}
