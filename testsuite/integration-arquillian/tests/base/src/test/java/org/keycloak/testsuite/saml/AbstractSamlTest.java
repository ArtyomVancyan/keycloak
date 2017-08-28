package org.keycloak.testsuite.saml;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.util.SamlClient;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import java.net.URI;
import java.util.List;

import static org.keycloak.testsuite.util.IOUtil.loadRealm;

/**
 * @author mhajas
 */
public abstract class AbstractSamlTest extends AbstractAuthTest {

    protected static final String REALM_NAME = "demo";
    protected static final String REALM_PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    protected static final String REALM_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    protected static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST = "http://localhost:8080/sales-post/";
    protected static final String SAML_CLIENT_ID_SALES_POST = "http://localhost:8081/sales-post/";

    protected static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST2 = "http://localhost:8080/sales-post2/";
    protected static final String SAML_CLIENT_ID_SALES_POST2 = "http://localhost:8081/sales-post2/";

    protected static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST_SIG = "http://localhost:8081/sales-post-sig/";
    protected static final String SAML_CLIENT_ID_SALES_POST_SIG = "http://localhost:8081/sales-post-sig/";
    protected static final String SAML_CLIENT_ID_SALES_POST_SIG_PRIVATE_KEY = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAL72RWA8WmyGNLZpPaDnaWpO/aWpvWOC9dlWTuIUXgsA7pJ0JcpdFkjhK7MhGRU2kTVrjZXWRBGVqjOgWSWC9fmyN7y2Slr3KWr5vEZcz1FHJypG7YH6hZRXsCqrsGzRe53RZ4dBhFwSgKOqwUobLrmqQtEN1PvQVMmClIe2z6APAgMBAAECgYA1TV5+BzqiMi/Cfsux/wYAo33PYPq5LRPcj2fDWTYK0j7FaGAoBSW0QA3HmUR8FFgh1hyWJ1GmquTwNiDMBKsNhMdfOpUEl8CTVWsAamrJGr5M0wtQeReJOJIM5DpaqFf3dQYyYCBCbVGwtrr5/LUE78HbLzn5h/Y5TKWwpLpq8QJBAOyVEOI0V20SN2/I4vbS1Dv5b54pXFXEoDJDeAWMggOLMQtm34mrVbPpdX02ErUkW2VW6B4PW2Vb/4rXp8SYNhcCQQDOoqg38l4ZvbSt9CV0BRZ1BtvSOtABiWChJT/19gf1hUlPy+eR+E8FPAjD0f165/X/+VE/+MeW0d3lyHjbfRjJAkEA3es6SiW0+IAE9lum4saDBLsHA4JitaVaa6u0EuhpMK/JUpuuBfJs0vWkGs61H6u5+8ZYt5HKNrrkazW9joEFAwJBAJOw2r8yMmP/nbZ/vI1SXZzDjDaU5rtSb4h+UVsBwOqRm7a3LQq+CezZ3gHog15nkQKmNpacwDtiQVHNmeR3Y1ECQHyURjnpJyc2KChmSSO0RR3Z6N2Fk9i5L1Ip+/3MfA34j4MTa9UVnPwY+r+ItwzHXc+Rlphok1DgW1Bj+FEtKfk=";
    protected static final String SAML_CLIENT_ID_SALES_POST_SIG_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+9kVgPFpshjS2aT2g52lqTv2lqb1jgvXZVk7iFF4LAO6SdCXKXRZI4SuzIRkVNpE1a42V1kQRlaozoFklgvX5sje8tkpa9ylq+bxGXM9RRycqRu2B+oWUV7Aqq7Bs0Xud0WeHQYRcEoCjqsFKGy65qkLRDdT70FTJgpSHts+gDwIDAQAB";

    protected static final String SAML_ASSERTION_CONSUMER_URL_SALES_POST_ENC = "http://localhost:8080/sales-post-enc/";
    protected static final String SAML_CLIENT_ID_SALES_POST_ENC = "http://localhost:8081/sales-post-enc/";
    protected static final String SAML_CLIENT_SALES_POST_ENC_PRIVATE_KEY = "MIICXQIBAAKBgQDb7kwJPkGdU34hicplwfp6/WmNcaLh94TSc7Jyr9Undp5pkyLgb0DE7EIE+6kSs4LsqCb8HDkB0nLD5DXbBJFd8n0WGoKstelvtg6FtVJMnwN7k7yZbfkPECWH9zF70VeOo9vbzrApNRnct8ZhH5fbflRB4JMA9L9R+LbURdoSKQIDAQABAoGBANtbZG9bruoSGp2s5zhzLzd4hczT6Jfk3o9hYjzNb5Z60ymN3Z1omXtQAdEiiNHkRdNxK+EM7TcKBfmoJqcaeTkW8cksVEAW23ip8W9/XsLqmbU2mRrJiKa+KQNDSHqJi1VGyimi4DDApcaqRZcaKDFXg2KDr/Qt5JFD/o9IIIPZAkEA+ZENdBIlpbUfkJh6Ln+bUTss/FZ1FsrcPZWu13rChRMrsmXsfzu9kZUWdUeQ2Dj5AoW2Q7L/cqdGXS7Mm5XhcwJBAOGZq9axJY5YhKrsksvYRLhQbStmGu5LG75suF+rc/44sFq+aQM7+oeRr4VY88Mvz7mk4esdfnk7ae+cCazqJvMCQQCx1L1cZw3yfRSn6S6u8XjQMjWE/WpjulujeoRiwPPY9WcesOgLZZtYIH8nRL6ehEJTnMnahbLmlPFbttxPRUanAkA11MtSIVcKzkhp2KV2ipZrPJWwI18NuVJXb+3WtjypTrGWFZVNNkSjkLnHIeCYlJIGhDd8OL9zAiBXEm6kmgLNAkBWAg0tK2hCjvzsaA505gWQb4X56uKWdb0IzN+fOLB3Qt7+fLqbVQNQoNGzqey6B4MoS1fUKAStqdGTFYPG/+9t";
    protected static final String SAML_CLIENT_SALES_POST_ENC_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDb7kwJPkGdU34hicplwfp6/WmNcaLh94TSc7Jyr9Undp5pkyLgb0DE7EIE+6kSs4LsqCb8HDkB0nLD5DXbBJFd8n0WGoKstelvtg6FtVJMnwN7k7yZbfkPECWH9zF70VeOo9vbzrApNRnct8ZhH5fbflRB4JMA9L9R+LbURdoSKQIDAQAB";


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    protected AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, String realmName) {
        return SamlClient.createLoginRequestDocument(issuer, assertionConsumerURL, getAuthServerSamlEndpoint(realmName));
    }

    protected URI getAuthServerSamlEndpoint(String realm) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .build(realm, SamlProtocol.LOGIN_PROTOCOL);
    }
}
