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

package org.keycloak.models.sessions.infinispan.initializer;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CacheInitializer {

    private static final Logger log = Logger.getLogger(CacheInitializer.class);

    public void initCache() {
    }

    public void loadSessions() {
        while (!isFinished()) {
            if (!isCoordinator()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    log.error("Interrupted", ie);
                }
            } else {
                startLoading();
            }
        }
    }


    protected abstract boolean isFinished();

    protected abstract boolean isCoordinator();

    /**
     * Just coordinator will run this
     */
    protected abstract void startLoading();
}
