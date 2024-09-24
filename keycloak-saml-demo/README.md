## Project Overview
This is a simple Spring Boot application that allows for Keycloak authentication over the SAML protocol. The application supports user login and logout via Keycloak and displays the attributes passed in the SAML Response. To view the full AuthResponse, use the [SAML Tracer](https://addons.mozilla.org/en-CA/firefox/addon/saml-tracer/) browser tool.

**Tech stack:** Java 17, Keycloak 21

## How to Run
1. `mvn clean install`
2. `java -jar name-of-the-jar`
3. Alternatively, you can run it from your IDE.

## Terminology
- Asserting party (ap) = Identity provider (idp) = Keycloak
- Relying party (rp) = Service provider (sp) = Spring Boot application

## SAML Flow
- **POST Binding** is used for AuthnRequest, AuthnResponse, and SingleLogout.
- **AuthnRequest:** The SP generates an XML document, signs, and serializes it. The request is sent to the IDP as a POST body. (If HTTP redirect binding were used, the SAML request would be present in the URL.)
- **AuthnResponse:** The IDP receives the request, verifies it, and prepares a response. The response is sent to the Assertion Consumer Service URL `{baseURL}/login/saml2/sso/{registrationId}`.
- Both requests and responses are signed. Both the SP and IDP need each other's keys.
- Both assertions and documents can be signed. If only one is signed, it's better to sign the document as it contains the assertion. This sample app signs only the document.

## Setup Instructions

### Keycloak Client
The service provider is registered in the Keycloak DEV environment as a **saml-demo** client.
- **Client ID:** `saml-demo`. This value is expected in the Issuer field in incoming AuthnRequests.
	- In the Spring Security module, it is referenced as `entityId`. Without setting it explicitly in code, the default value is `entityId = {baseUrl}/saml2/service-provider-metadata/{registrationId}`.
- **Root URL:** `http://localhost:8080`
- **Home URL:** `http://localhost:8080`
- **Valid Redirect URIs:** `http://localhost:8080/*`
- **Valid Post Logout Redirect URIs:** `http://localhost:8080/*`
- **Logout Service POST Binding URL:** `/logout/saml2/slo`
	- `{baseURL}/logout/saml2/slo` is the endpoint that Spring exposes for SAML logout.
- Additionally, the client has three predefined and two custom mappers configured:
	- Predefined: X500 surname, givenName, email
	- Custom: email (user attribute mapper), OrganizationName (hardcoded value)
- The rest of the settings are left as default.

### Certificates
This application requires two certificates (pairs of public and private keys). Each party has the other's certificate to validate signatures.

#### For Signing AuthnRequests
- Credentials were generated using the command:
  ```shell
  openssl req -newkey rsa:2048 -nodes -keyout rp-key.key -x509 -days 365 -out rp-certificate.crt
  ```
- The certificate is stored in KeePass under `Dev saml-demo certificate`.
- Both files should be present in the `resources/signing` folder in the Spring Boot application.
- The .crt file was imported (as Certificate PEM type) to the **saml-demo** Keycloak client in the dev environment so that Keycloak can verify the AuthnRequest.

#### For Signing AuthnResponses
- The certificate can be found in [Keycloak's SAML metadata descriptor](https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications/protocol/saml/descriptor). (Note that this address returns an XML document, and you need the value of the `ds:X509Certificate` element.)
- The certificate needs to be copied to the Spring Boot app and placed in the `application.yml` file under the `signing-cert` property.

## Useful Links
- [SAML Certificates](https://support.pingidentity.com/s/article/Introduction-to-SAML-Certificates)
- [Spring Docs](https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html#servlet-saml2login-sp-initiated-factory)
- [Intro to SAML](https://goteleport.com/blog/how-saml-authentication-works/)