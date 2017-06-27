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
package org.keycloak.broker.saml;

import org.keycloak.models.IdentityProviderModel;

import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProviderConfig extends IdentityProviderModel {

    public static final XmlKeyInfoKeyNameTransformer DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER = XmlKeyInfoKeyNameTransformer.NONE;

    public static final String ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO = "addExtensionsElementWithKeyInfo";
    public static final String BACKCHANNEL_SUPPORTED = "backchannelSupported";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    public static final String FORCE_AUTHN = "forceAuthn";
    public static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
    public static final String POST_BINDING_AUTHN_REQUEST = "postBindingAuthnRequest";
    public static final String POST_BINDING_LOGOUT = "postBindingLogout";
    public static final String POST_BINDING_RESPONSE = "postBindingResponse";
    public static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String SIGNING_CERTIFICATE_KEY = "signingCertificate";
    public static final String SINGLE_LOGOUT_SERVICE_URL = "singleLogoutServiceUrl";
    public static final String SINGLE_SIGN_ON_SERVICE_URL = "singleSignOnServiceUrl";
    public static final String VALIDATE_SIGNATURE = "validateSignature";
    public static final String WANT_ASSERTIONS_ENCRYPTED = "wantAssertionsEncrypted";
    public static final String WANT_ASSERTIONS_SIGNED = "wantAssertionsSigned";
    public static final String WANT_AUTHN_REQUESTS_SIGNED = "wantAuthnRequestsSigned";
    public static final String XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER = "xmlSigKeyInfoKeyNameTransformer";

    public SAMLIdentityProviderConfig() {
    }

    public SAMLIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public String getSingleSignOnServiceUrl() {
        return getConfig().get(SINGLE_SIGN_ON_SERVICE_URL);
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        getConfig().put(SINGLE_SIGN_ON_SERVICE_URL, singleSignOnServiceUrl);
    }

    public String getSingleLogoutServiceUrl() {
        return getConfig().get(SINGLE_LOGOUT_SERVICE_URL);
    }

    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        getConfig().put(SINGLE_LOGOUT_SERVICE_URL, singleLogoutServiceUrl);
    }

    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get(VALIDATE_SIGNATURE));
    }

    public void setValidateSignature(boolean validateSignature) {
        getConfig().put(VALIDATE_SIGNATURE, String.valueOf(validateSignature));
    }

    public boolean isForceAuthn() {
        return Boolean.valueOf(getConfig().get(FORCE_AUTHN));
    }

    public void setForceAuthn(boolean forceAuthn) {
        getConfig().put(FORCE_AUTHN, String.valueOf(forceAuthn));
    }

    /**
     * @deprecated Prefer {@link #getSigningCertificates()}}
     * @param signingCertificate
     */
    public String getSigningCertificate() {
        return getConfig().get(SIGNING_CERTIFICATE_KEY);
    }

    /**
     * @deprecated Prefer {@link #addSigningCertificate(String)}}
     * @param signingCertificate
     */
    public void setSigningCertificate(String signingCertificate) {
        getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
    }

    public void addSigningCertificate(String signingCertificate) {
        String crt = getConfig().get(SIGNING_CERTIFICATE_KEY);
        if (crt == null || crt.isEmpty()) {
            getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
        } else {
            // Note that "," is not coding character per PEM format specification:
            // see https://tools.ietf.org/html/rfc1421, section 4.3.2.4 Step 4: Printable Encoding
            getConfig().put(SIGNING_CERTIFICATE_KEY, crt + "," + signingCertificate);
        }
    }

    public String[] getSigningCertificates() {
        String crt = getConfig().get(SIGNING_CERTIFICATE_KEY);
        if (crt == null || crt.isEmpty()) {
            return new String[] { };
        }
        // Note that "," is not coding character per PEM format specification:
        // see https://tools.ietf.org/html/rfc1421, section 4.3.2.4 Step 4: Printable Encoding
        return crt.split(",");
    }

    public String getNameIDPolicyFormat() {
        return getConfig().get(NAME_ID_POLICY_FORMAT);
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        getConfig().put(NAME_ID_POLICY_FORMAT, nameIDPolicyFormat);
    }

    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_AUTHN_REQUESTS_SIGNED));
    }

    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        getConfig().put(WANT_AUTHN_REQUESTS_SIGNED, String.valueOf(wantAuthnRequestsSigned));
    }

    public boolean isAddExtensionsElementWithKeyInfo() {
        return Boolean.valueOf(getConfig().get(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO));
    }

    public void setAddExtensionsElementWithKeyInfo(boolean addExtensionsElementWithKeyInfo) {
        getConfig().put(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO, String.valueOf(addExtensionsElementWithKeyInfo));
    }

    public String getSignatureAlgorithm() {
        return getConfig().get(SIGNATURE_ALGORITHM);
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        getConfig().put(SIGNATURE_ALGORITHM, signatureAlgorithm);
    }

    public String getEncryptionPublicKey() {
        return getConfig().get(ENCRYPTION_PUBLIC_KEY);
    }

    public void setEncryptionPublicKey(String encryptionPublicKey) {
        getConfig().put(ENCRYPTION_PUBLIC_KEY, encryptionPublicKey);
    }

    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_AUTHN_REQUEST));
    }

    public void setPostBindingAuthnRequest(boolean postBindingAuthnRequest) {
        getConfig().put(POST_BINDING_AUTHN_REQUEST, String.valueOf(postBindingAuthnRequest));
    }

    public boolean isPostBindingResponse() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_RESPONSE));
    }

    public void setPostBindingResponse(boolean postBindingResponse) {
        getConfig().put(POST_BINDING_RESPONSE, String.valueOf(postBindingResponse));
    }

    public boolean isBackchannelSupported() {
        return Boolean.valueOf(getConfig().get(BACKCHANNEL_SUPPORTED));
    }

    public void setBackchannelSupported(boolean backchannel) {
        getConfig().put(BACKCHANNEL_SUPPORTED, String.valueOf(backchannel));
    }

    /**
     * Always returns non-{@code null} result.
     * @return Configured ransformer of {@link #DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER} if not set.
     */
    public XmlKeyInfoKeyNameTransformer getXmlSigKeyInfoKeyNameTransformer() {
        return XmlKeyInfoKeyNameTransformer.from(getConfig().get(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER), DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER);
    }

    public void setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer xmlSigKeyInfoKeyNameTransformer) {
        getConfig().put(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER,
          xmlSigKeyInfoKeyNameTransformer == null
            ? null
            : xmlSigKeyInfoKeyNameTransformer.name());
    }

}
