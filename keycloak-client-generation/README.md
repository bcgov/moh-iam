# Bulk generation of Keycloak Clients and their certs

This application handles bulk generation of Keycloak Clients and their certs. The clients are created with a default set of properties currently suited to creating
clients for use by PPM API clients. The main characteristic of these clients is that they authenticate by "Signed JWT".

Currently this program will output:
- List of client IDs for the newly created clients.
- Their associated JWT information of PFX cert and passwords.
