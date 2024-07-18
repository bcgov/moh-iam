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


3. Run the application from your IDE.

### Output:

The copied folders will be generated in target/classes/theme-output.

### Deploying to Keycloak:

In order to add the themes to a Keycloak installation:

1. Copy the generated folders to the "themes" directory in your Keycloak installation.
2. Restart Keycloak.
3. Apply the themes. The theme will be available for selection in the Keycloak Admin Console. Those themes are meant to be applied at the client level: Client -> Client Name -> Settings -> Login Theme.

#### Clearing Theme Cache
During a deploy, some theme resource files such as scripts may remain cached after restarting Keycloak. This cache can be cleared with the following:

As the gfadmin user, remove the cache folder for a specific theme found under /data/gfadmin/software/keycloak/data/tmp/kc-gzip-cache/?????/login/moh-app-realm/ (where ????? can be found from inspecting a theme resource file from your browser's Developer Tools -> Network tab).

Note: Dev ?????=ng78f, Test ?????=472zi, and Prod ?????=on1k1 but this could change in the future.

e.g. To clear the cache for the moh-app-realm login theme in Keycloak Dev, run the following command as gfadmin:

rm -rf /data/gfadmin/software/keycloak/data/tmp/kc-gzip-cache/ng78f/login/moh-app-realm/

