# LDAP to Keycloak ETL Tool Usage Guide

This tool is designed to work with the MoH LDAP and a Keycloak realm that has been synched with the LDAP. It takes all of the existing user roles for a single application in LDAP (defined by objectClass) and creates them for that same application (defined by client) in KeyCloak. It will then also add the role to same users that had the role in LDAP. User matching is done by matching LDAP uid to Keycloak username. 

The application requires a properties file for configuration. The path to this file can be provided as a command line argument or will otherwise default to the template configuration file at src/main/resources. Please see the template configuration file for the required configuration properities. 

This application is designed to be run with Java 11. 


