package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;

import java.util.List;

import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;

public abstract class AbstractBrokerTest extends AbstractKeycloakTest {

    protected abstract RealmRepresentation createProviderRealm();
    protected abstract RealmRepresentation createConsumerRealm();

    protected abstract List<ClientRepresentation> createProviderClients();
    protected abstract List<ClientRepresentation> createConsumerClients();

    protected abstract IdentityProviderRepresentation setUpIdentityProvider();

    protected abstract String providerRealmName();
    protected abstract String consumerRealmName();

    protected abstract String getUserLogin();
    protected abstract String getUserPassword();
    protected abstract String getUserEmail();

    protected abstract String getIDPAlias();

    @Page
    protected LoginPage accountLoginPage;
    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation providerRealm = createProviderRealm();
        RealmRepresentation consumerRealm = createConsumerRealm();

        testRealms.add(providerRealm);
        testRealms.add(consumerRealm);
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(getUserLogin());
        user.setEmail(getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(providerRealmName());
        String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + consumerRealmName());

        RealmResource realm = adminClient.realm(consumerRealmName());
        realm.identityProviders().create(setUpIdentityProvider());
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = createProviderClients();
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + providerRealmName());

                providerRealm.clients().create(client);
            }
        }

        clients = createConsumerClients();
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + consumerRealmName());

                consumerRealm.clients().create(client);
            }
        }
    }

    protected String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    protected IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    @Test
    public void tryToLogInAsUserInIDP() {
        driver.navigate().to(getAuthRoot() + "/auth/realms/" + consumerRealmName() + "/account");

        accountLoginPage.clickSocial(getIDPAlias());

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"));

        accountLoginPage.login(getUserLogin(), getUserPassword());

        Assert.assertTrue("We must be on update user profile page right now",
                updateAccountInformationPage.isCurrent());

        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/"));

        updateAccountInformationPage.updateAccountInformation("Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(consumerRealmName()).users();
        List<UserRepresentation> users = consumerUsers.search("", 0, 5);
        Assert.assertTrue("There must be at least one user", users.size() > 0);

        boolean foundUser = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(getUserLogin()) && user.getEmail().equals(getUserEmail())) {
                foundUser = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + getUserLogin() + " in realm " + consumerRealmName(),
                foundUser);
    }
}
