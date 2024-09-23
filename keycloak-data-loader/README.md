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
# Configuration
* Properties are used as follows. KeePass file DevPasswords entry Keycloak -> IAM keycloak-data-loader user - * contains environment specific info for _url_, _client-id_ and _client-secret_ properties. _realm_, _application_ and _username-type_ are set based on the client requesting the upload:
	* url = The URL of the keycloak instance e.g. https://common-logon-dev.hlth.gov.bc.ca/auth
	* realm = The realm the upload will be run against e.g. moh_applications
	* client-id = Client ID of the service account that executes the script (BULK-USER-UPLOAD)
	* application = The application/client the info is being uploaded for e.g. MSPDirect-Service
	* username-type = This value will be used to indicate if a prefix or suffix is to be added to the supplied usernames. Valid values are specified in the UsernameTypeEnum, which defines the value to be added to the username and whether it is prefix or suffix e.g. IDIR defines a suffix of @idir so with supplied username 'test1' would result in username of 'test1@idir' being processed. If no change to the username is required then specify NONE and the username will processed as supplied.
	* data-file-location = The location of the file containing the keycloak user data to be uploaded. This file will be created based on the data in the clients request.
    * Additionally, developer needs to set an environment variable BULK-USER-UPLOAD-CLIENT-SECRET-*ENV* where client secret is stored.
# Run
* Create a file in format described above.
* Using the configuration.properties as a template set the properties for the environment against which the script will be run in a suitably named file e.g. configuration-dev.properties.
* Provide the Keycloak connection information, the application for which the user should be configure and the file location.
* Run the application, providing the following Java arguments.
	* The required environment. Valid environment values are DEV, TEST, PROD.	
