# LDAP to Keycloak ETL Tool Usage Guide

This command line Java application is designed to work with the MoH LDAP and a Keycloak realm that has been synced with the LDAP. It takes all of the existing user roles for a single application in LDAP (defined by objectClass) and creates them for that same application (defined by client) in KeyCloak. It will then also add the role to same users that had the role in LDAP. User matching is done by matching LDAP uid to Keycloak username. 

The application requires a configuration file. The path to this file can be provided as a command line argument or will otherwise default to the template configuration file at `src/main/resources`. Please see the [template configuration file](https://github.com/bcgov/moh-iam/blob/master/ldap-keycloak-etl/src/main/resources/configuration.properties) for the required configuration properities. 

This application requires Java 11.

## How to use

Prequisites:

* Apache Maven 3.6.1.
* Java 11.
* A [configuration file](https://github.com/bcgov/moh-iam/blob/master/ldap-keycloak-etl/src/main/resources/configuration.properties).

Steps:

1. Download the project.
2. Use the command line and navigate to `moh-iam/ldap-keycloak-etl`.
3. Run `mvn package` to build the executable jar.
4. Run `java -jar target/ldap-keycloak-etl-jar-with-dependencies.jar "C:\Users\david.a.sharpe\Desktop\MoHAA\configuration.properties"`.
