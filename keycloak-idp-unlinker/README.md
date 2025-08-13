# Keycloak IDP Unlinker

This utility is used to fix a specific identity conflict in Keycloak where users attempting to sign in with a Health Authority ID (via the PHSA Entra ID tenant) were incorrectly registered with email-based usernames instead of UPNs.

These accounts must have their `phsa_aad` identity provider link removed so that on next login, Keycloak can correctly link the user to their UPN-based account — or create a new one as needed.

## Why this is needed

Keycloak was previously misconfigured to use email as the username for PHSA_AAD logins. This caused duplicate accounts and sign-in failures for Panorama users. Updating the username mapper alone causes a conflict unless these old identity links are removed first.

## Usage

```bash
java KeycloakIdpUnlinker --input bad_user_ids.txt --env TEST
java KeycloakIdpUnlinker --input bad_user_ids.txt --env PROD --real
