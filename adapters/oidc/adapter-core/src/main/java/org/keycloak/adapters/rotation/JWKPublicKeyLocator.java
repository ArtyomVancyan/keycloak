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

package org.keycloak.adapters.rotation;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.methods.HttpGet;
import org.jboss.logging.Logger;
import org.keycloak.adapters.HttpAdapterUtils;
import org.keycloak.adapters.HttpClientAdapterException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.util.JWKSUtils;

/**
 * When needed, publicKeys are downloaded by sending request to realm's jwks_url
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWKPublicKeyLocator implements PublicKeyLocator {

    private static final Logger log = Logger.getLogger(JWKPublicKeyLocator.class);

    private Map<String, PublicKey> currentKeys = new ConcurrentHashMap<>();

    private volatile int lastRequestTime = 0;

    @Override
    public PublicKey getPublicKey(JWSInput input, KeycloakDeployment deployment) {
        String kid = input.getHeader().getKeyId();
        return getPublicKey(kid, deployment);
    }


    private PublicKey getPublicKey(String kid, KeycloakDeployment deployment) {
        int minTimeBetweenRequests = deployment.getMinTimeBetweenJwksRequests();

        // Check if key is in cache.
        PublicKey publicKey = currentKeys.get(kid);
        if (publicKey != null) {
            return publicKey;
        }

        int currentTime = Time.currentTime();

        // Check if we are allowed to send request
        if (currentTime > lastRequestTime + minTimeBetweenRequests) {
            synchronized (this) {
                currentTime = Time.currentTime();
                if (currentTime > lastRequestTime + minTimeBetweenRequests) {
                    sendRequest(deployment);
                    lastRequestTime = currentTime;
                } else {
                    log.debugf("Won't send request to realm jwks url. Last request time was %d", lastRequestTime);
                }
            }
        }

        return currentKeys.get(kid);

    }


    private void sendRequest(KeycloakDeployment deployment) {
        if (log.isTraceEnabled()) {
            log.tracef("Going to send request to retrieve new set of realm public keys for client %s", deployment.getResourceName());
        }

        HttpGet getMethod = new HttpGet(deployment.getJwksUrl());
        try {
            JSONWebKeySet jwks = HttpAdapterUtils.sendJsonHttpRequest(deployment, getMethod, JSONWebKeySet.class);

            Map<String, PublicKey> publicKeys = JWKSUtils.getKeysForUse(jwks, JWK.Use.SIG);

            if (log.isDebugEnabled()) {
                log.debugf("Realm public keys successfully retrieved for client %s. New kids: %s", deployment.getResourceName(), publicKeys.keySet().toString());
            }

            // Update current keys
            currentKeys.clear();
            currentKeys.putAll(publicKeys);

        } catch (HttpClientAdapterException e) {
            log.error("Error when sending request to retrieve realm keys", e);
        }
    }
}
