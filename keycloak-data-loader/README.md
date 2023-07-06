# Bulk upload of user info to Keycloak

This application handles updating Keycloak user info in a bulk upload. The program accepts a csv file with fields:
  	User - Roles
  	e.g. 
  	testuser1,"role_1, role_2"
  	testuser2,"role_1, role_3"
  
Currently this program will:
* Add users if they don't exist
* Add the specified roles to the user. No roles are removed
 
The program does not remove any user configuration and the same file can be imported multiple times to produce the same end result.

### How to use this script:

* Create a file in format described above.
* Using the configuration.properties set the properties for the environment against which the script will be run in a suitably name file e.g. configuration-dev.properties
* Provide the Keycloak connection information, the application for which the user should be configure and the file location.
* Run the application, providing the required environment as a Java argument e.g. 'dev'