# Bulk generation of Keycloak Clients and their certs

This application handles bulk generation of Keycloak Clients and their certs. The clients are created with a default set of properties currently suited to creating
clients for use by PPM API clients. The main characteristic of these clients is that they authenticate by "Signed JWT".

## Running the program

The program takes the following four arguments:
- The environment in which it should be run, valid vaues are:
    - dev
    - prod
- The batch number e.g. 3 if this is the third time it is being run in this environment.
- The number of clients to be created; currently allows max of 50 per batch.
- The seed number for the first client.

It can be run using the maven exec command with provided arguments e.g.
	 mvn compile exec:java -Dexec.args="dev 4 1 520"

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

