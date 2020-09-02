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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleContainerModel {

    interface RoleRemovedEvent extends ProviderEvent {
        RoleModel getRole();
        KeycloakSession getKeycloakSession();
    }

    String getId();

    RoleModel getRole(String name);

    RoleModel addRole(String name);

    RoleModel addRole(String id, String name);

    boolean removeRole(RoleModel role);

    @Deprecated
    default Set<RoleModel> getRoles() {
        return getRolesStream().collect(Collectors.toSet());
    }

    Stream<RoleModel> getRolesStream();

    @Deprecated
    default Set<RoleModel> getRoles(Integer firstResult, Integer maxResults) {
        return getRolesStream(firstResult, maxResults).collect(Collectors.toSet());
    }

    Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults);

    @Deprecated
    default Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
        return searchForRolesStream(search, first, max).collect(Collectors.toSet());
    }

    Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max);

    /**
     * Returns all the default role names of this object.
     * @return List of the default role names of this object. Never returns {@code null}.
     * @deprecated use the stream variant instead
     */
    @Deprecated
    default List<String> getDefaultRoles() {
        return getDefaultRolesStream().collect(Collectors.toList());
    }

    /**
     * Returns all default role names of this object as a stream.
     * @return stream of default role names of this object. Never returns {@code null}.
     */
    Stream<String> getDefaultRolesStream();

    /**
     * Adds a role with given name to default roles of this object. If the role
     * doesn't exist a new role is created.
     * @param name of the role to be (created and ) added
     */
    void addDefaultRole(String name);

    /**
     * Updates default roles of this object. It removes all default roles which
     * are not specified by {@code defaultRoles} and adds all which weren't
     * present in original default roles. In other words it's the same as calling 
     * {@code Set.retainAll} and {@code Set.addAll}.
     * @param defaultRoles Array of realm roles to be updated
     */
    default void updateDefaultRoles(String... defaultRoles) {
        List<String> defaultRolesArray = Arrays.asList(defaultRoles);
        Collection<String> entities = getDefaultRolesStream().collect(Collectors.toList());
        Set<String> already = new HashSet<>();
        ArrayList<String> remove = new ArrayList<>();
        for (String rel : entities) {
            if (! defaultRolesArray.contains(rel)) {
                remove.add(rel);
            } else {
                already.add(rel);
            }
        }
        removeDefaultRoles(remove.toArray(new String[] {}));

        for (String roleName : defaultRoles) {
            if (!already.contains(roleName)) {
                addDefaultRole(roleName);
            }
        }
    }

    /**
     * Removes default roles from this object according to {@code defaultRoles}.
     * @param defaultRoles Role names to be removed from default roles of this object.
     */
    void removeDefaultRoles(String... defaultRoles);

}
