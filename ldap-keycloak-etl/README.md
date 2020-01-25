# LDAP to Keycloak ETL Tool

This command line Java application is designed to work with the MoH LDAP and a Keycloak realm that has been synced with the LDAP. It takes all of the existing user roles for a single application in LDAP (defined by objectClass) and creates them for that same application (defined by client) in KeyCloak. It will then also add the role to same users that had the role in LDAP. User matching is done by matching LDAP uid to Keycloak username. 

The application requires a configuration file. The path to this file can be provided as a command line argument or will otherwise default to the template configuration file at `src/main/resources`. Please see the [template configuration file](https://github.com/bcgov/moh-iam/blob/master/ldap-keycloak-etl/src/main/resources/configuration.properties) for the required configuration properties. 

## Usage guide

Prerequisites:

* Apache Maven 3.6.1.
* Java 11.
* A [configuration file](https://github.com/bcgov/moh-iam/blob/master/ldap-keycloak-etl/src/main/resources/configuration.properties).

Steps:

1. Download the project.
2. Use the command line and navigate to `moh-iam/ldap-keycloak-etl`.
3. Run `mvn package` to build the executable jar.
4. Run `java -jar target/ldap-keycloak-etl-jar-with-dependencies.jar "C:\Users\david.a.sharpe\Desktop\MoHAA\configuration.properties"`.

## History and justification

We are aware that Keycloak provides [LDAP Mappers](https://www.keycloak.org/docs/latest/server_admin/#_ldap_mappers), but unfortunately these could be not be used because the MoH LDAP schema stores roles in an unconventional way. A conventional LDAP schema stores roles in a group, and membership is specified with a `member` attribute on the role. The MoH LDAP does not store roles, instead each user specifies their role as a text attribute.

For example, a conventional schema specifies roles like this (taken from an [Keycloak example LDIF](https://github.com/keycloak/keycloak/blob/master/examples/ldap/ldap-example-users.ldif)):
```
dn: cn=ldap-user,ou=RealmRoles,dc=keycloak,dc=org
objectclass: top
objectclass: groupOfNames
cn: ldap-user
member: uid=jbrown,ou=People,dc=keycloak,dc=org
member: uid=bwilson,ou=People,dc=keycloak,dc=org
```

MoH LDAP example:
```
dn: uid=some.user, o=Ministry of Health, o=HNET, st=BC, c=CA
objectclass: fmdbuser
objectclass: inetOrgPerson
objectclass: organizationalPerson
objectclass: person
objectclass: top
cn: Some User
sn: User
fmdbuserrole: PSDADMIN
```
