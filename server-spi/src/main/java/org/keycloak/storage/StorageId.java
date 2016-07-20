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
package org.keycloak.storage;

import org.keycloak.models.UserModel;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StorageId implements Serializable {
    private String id;
    private String providerId;
    private String storageId;


    public StorageId(String id) {
        this.id = id;
        if (!id.startsWith("f:")) {
            storageId = id;
            return;
        }
        int providerIndex = id.indexOf(':', 2);
        providerId = id.substring(2, providerIndex);
        storageId = id.substring(providerIndex + 1);

    }

    public StorageId(String providerId, String storageId) {
        this.id = "f:" + providerId + ":" + storageId;
        this.providerId = providerId;
        this.storageId = storageId;
    }

    public static String resolveProviderId(UserModel user) {
        return new StorageId(user.getId()).getProviderId();
    }
    public static boolean isLocalStorage(UserModel user) {
        return new StorageId(user.getId()).getProviderId() == null;
    }

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getStorageId() {
        return storageId;
    }


}
