## Theme Generator Application
This Java application reads configurations from theme-input.txt, copies the theme-template folder to theme-output under different names specified in the input file, and updates the scripts.js file in each copied folder, so that only specified identity providers are shown.

### Usage
1. Make sure the theme-template folder is located inside the src/main/resources and has the following structure:
    ```
    theme-template
    └── login
        ├── resources
        │   └── js
        │       └── scripts.js
        ├── login.ftl
        └── theme.properties
    ```
   - login.ftl is a FreeMarker Template file. Body of the theme.
   - scripts.js contains functions that compliment the ftl file. Importantly, it contains `const IDPS_TO_SHOW = [];` line, which will be replaced with the corresponding array from theme-input.txt for each copied folder.
   - theme.properties defines basic property of the theme. In this case it defines parent of a theme: `moh-app-realm` That's where the base template and styles are inherited from.


2. Create or modify the theme-input.txt file located in src/main/resources. Each line in this file specifies a new folder name followed by a list of IDPs to be shown for a given theme.
    ***Theme name can contain letters, numbers, underscores(_) and dashes(-)***

    Example theme-input.txt content:

    ```
    IDIR : ["idir", "idir_aad"]
    ALL : ["idir", "idir_aad", "phsa", "phsa_aad", "fnha_aad", "bceid_business", "bcprovider_aad", "moh_idp"]
    IDIR-PHSA : ["idir", "idir_aad", "phsa", "phsa_aad"]
    IDIR_AAD-PHSA_AAD : ["idir_aad", "phsa_aad"]
    ```
   For the above input file, 4 new themes with names IDIR, ALL, IDIR-PHSA, IDIR_AAD-PHSA_AAD would be created. The IDIR theme would display Identity Providers with idir and idir_aad aliases. 


3. Build the application:

   `mvn clean package`
4. Run the application:

    `java -jar target/theme-generator.jar`
### Output:

The copied folders will be generated in src/main/resources/theme-output.

### Deploying to Keycloak:

