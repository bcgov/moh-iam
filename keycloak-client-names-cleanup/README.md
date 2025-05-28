# Identify and Fix Users with Invalid Characters in Name Attributes

---
keycloak uses validator to check if the value is a valid person name as an additional barrier for attacks such as script injection. 
The validation is based on a default RegEx pattern that blocks characters not common in person names.

## Regular expression

The following regular expression is used based on Keycloak 26 code.

 - ^ [^ <>&\"\\v$%!#?§;*~/\\\\|^=\\[\\]{}()\\p{Cntrl}]+$

## Users Identification

Run the following SQL query on the Database and save the result to csv file.
 
~~~~sql
	 
	 select id,realm_id
		from user_entity
	 where (
			REGEXP_LIKE (last_name, '[<>#(){}]+')
			or
			 REGEXP_LIKE (first_name, '[<>#(){}]+'));
~~~~			 

## Fix

#### Update config.properties

* **_INPUT_FILE_PATH_**  path of the result file
* **_KEYCLOAK_URL_** keycloak instance URL
* **_CLIENT_ID_** contains client ID 
* **_CLIENT_SECRET_** contains client secret
* **_REALM_**  REALM_ID 
* **_SIMULATION_MODE_**  Simulation (true) Real (false)

#### Steps

1. SIMULATION_MODE shoud be true for the first Run
2. Run main method in KeycloakUserNameUpdater class
3. Validate the result in the Console
4. If the result are good update SIMULATION_MODE to false and do run step 2 again



