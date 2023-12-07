## System Requirements

You need to have <span>Keycloak</span> running. It is recommended to use Keycloak 22 or later.

All you need to build this project is Java 17 (Java SDK 17) or later and Maven 3.6.3 or later.

If you want to do the JSX example, you will need to install npm on your system.

It is also recommended that you read about Keycloak themes in the Server Developer guide.

## Configuration in <span>Keycloak</span>

To build the provider, run the following maven command:

```
mvn clean install -Pextension -DskipTests
```

To install the provider, copy the `target/moh-account-theme.jar` JAR file to the `providers` directory of the server distribution.

Finally, start the server as follows:

    ```
    kc.[sh|bat] start-dev
    ```

1. Open Keycloak Admin Console.
2. Go to the `Realm Settings-->Themes` tab.
3. Set Account Theme to `moh-app-realm-account`
4. Go to the account console.

## Additional information

As of Keycloak 22, account theme is a React application. The idea behind extending themes is similar as with FreeMarker templates - the theme inherits everything from the parent unless explicitly overwritten.
The `moh-account-theme` overwrites 3 pages: `AccountPage, DeviceActivityPage, LinkedAccountsPage`, changes the layout and functionality of the pages.

Important files & folders:

1. `keycloak-themes.json` inside META-INF folder contains the high-level definition of the theme: name and supported theme aspect (login, account etc.). Keycloak server uses this config file to discover and load the theme.
2. `theme.properties` specifies the basic theme information - parent that it's extending, source of the styles file.
3. `resources` folder is where the actual theme content resides. It contains fonts, styles, images and minified transpiled source code.
4. `content.json` in `resources` folder contains metadata about custom components, that makes the discoverable for the Keycloak server.

## Extending the theme further

If you wish to make further changes to the customized theme, you can extend the React application. To do so, in the `src` folder:

1. run `npm install` which will install all the required dependencies
2. Make necessary changes in the source code
3. run `npm run build` which will transpile the source code and put it to the `resources` folder
4. Build and deploy JAR file
