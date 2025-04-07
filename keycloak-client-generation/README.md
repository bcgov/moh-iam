# Bulk generation of Keycloak Clients and their certs

This application handles bulk generation of Keycloak Clients and their certs. The clients are created with a default set of properties currently suited to creating
clients for use by PNet API clients. The main characteristic of these clients is that they authenticate by "Signed JWT".

## Configuring the application
The application is configured using the configuration.properties as a template and creating an environmetn specific configuration file named configuration-{ENV_NAME}.properties e.g. configuration-dev.properties. Valid environments are:
    - dev
    - test
    - prod

The Keycloak connection related properties must be provided by the AM team. See https://hlth.atlassian.net/wiki/spaces/HA/pages/515620798/PNET+API+-+Certificate+Creation+and+Rotation#PNETAPI-CertificateCreationandRotation-ProdConfiguration for further information on configuration.

A cooresponding client secret value must be set as a local System Environment Variable named CLIENT_GENERATION_CLIENT_SECRET_{ENV_NAME} e.g. CLIENT_GENERATION_CLIENT_SECRET_DEV

The Keycloak realm cert moh_applications.cer must be added to the local project under resources/input. This is a Keycloak cert used during PFX file creation. Check the expiry date and if it will expire before the expiry date of the certs being created then it must be updated with another from Keycloak that has an expiry date greater than the expiry date of the PFX files being created.

## Running the program

- Prerequisite
	- JDK 13 or higher
    - Maven

The program takes the following four arguments:
- The environment in which it should be run, valid vaues are:
    - dev
    - test
    - prod
- The batch number e.g. 3 if this is the third time it is being run in this environment.
- The number of clients to be created; may not exceed max allowed per batch, currently 200.
- The seed number for the first client. This value is suffixed to 'PNET-' to create the client ID. It should be based on the highest number from the previous run for that environment and incremented by 1. E.g. If the last client created was PNET-31632460 then the seed number would be 31632461. Note, the second to third digits can be used to track the batch number in the client IDs e.g. if the batch number is 7 then the seed number becomes 31732461.


It can be run using the maven exec command with provided arguments e.g.
	 mvn compile exec:java -Dexec.args="dev 4 1 520"

Follow any prompts for requested information and confirmations.

## Program output

Currently this program will output:
- A CVS file containing a record for each client created and it's associated information. The record includes:
	- Client ID for the newly created client
    - associated cert file name
    - cert file alias
    - key password
    - store password
    - cert valid from date
    - cert expiry date
- The client certificates will also be genearated and saved locally. 

The output location is specified in the properties file.

