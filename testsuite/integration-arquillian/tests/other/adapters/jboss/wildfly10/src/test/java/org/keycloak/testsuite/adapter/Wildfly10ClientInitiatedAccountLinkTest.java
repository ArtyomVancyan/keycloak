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
package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.AbstractClientInitiatedAccountLinkTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
@AppServerContainer("app-server-wildfly10")
public class Wildfly10ClientInitiatedAccountLinkTest extends AbstractClientInitiatedAccountLinkTest {

}
