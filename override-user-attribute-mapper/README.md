# OIDC Override User Attribute Mapper

The OIDC Override User Attribute Mapper is a customized version of Keycloak's built-in OIDC User Attribute Mapper with
modified priority settings. Several minor internal adjustments were made, including changes to the internal ID and class
name, to prevent conflicts with the built-in mapper.

The Override Mapper runs at a _lower_ priority than the built-in mappers, meaning it executes _after_ them. This enables
the Override Mapper to override existing values. It was created to allow overriding the `preferred_username` set by the
default `profile` scope.

## Prerequisites

Before building and deploying the Override User Attribute Mapper, ensure you have the following:

- JDK 17
- Apache Maven (version 3.6.3 or compatible)
- Keycloak (version 22.0.4 or compatible)

## Build Instructions

To build the Override User Attribute Mapper, follow these steps:

1. Clone the repository:
    ```bash
    git clone https://github.com/your-username/override-user-attribute-mapper.git
    cd override-user-attribute-mapper
    ```

2. Compile and package the project using Maven:
    ```bash
    mvn clean package
    ```

   This command generates a JAR file located in the `target` directory.

## Installation

To install the Override User Attribute Mapper, follow Keycloak's documentation on Service Provider Interfaces (SPIs). At
the time of writing, the process involves generating the JAR, copying it to the `providers` folder of the Keycloak
installation, and restarting Keycloak.

Example:

```bash
cp target/OverrideUserAttributeMapper-1.0-SNAPSHOT.jar /data/gfadmin/software/keycloak/providers/
sudo systemctl restart keycloak
```

According to the Keycloak documentation, "After registering new providers or dependencies, Keycloak needs to be re-built
with the `kc.[sh|bat] build` command". However, in our deployment environment, simply restarting the service was
sufficient, possibly because the `build` step is included in the systemctl service.

## Usage

Once the Override User Attribute Mapper is installed, you can use it the same way you would any mapper on Keycloak. It
will appear as "Override User Attribute" in the interface.

## Resources

- [Keycloak documentation: Registering provider implementations](https://www.keycloak.org/docs/22.0.5/server_development/index.html#registering-provider-implementations)
- [Blog: Custom Protocol Mapper with Keycloak](https://www.baeldung.com/keycloak-custom-protocol-mapper)
- [Blog: Java Service Provider Interface](https://www.baeldung.com/java-spi)