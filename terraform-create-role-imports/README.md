# Writing terraform resource and imports

### How to use this:
1. Store the client secret values as environment variables named `TF_VAR_dev_client_secret`, `TF_VAR_test_client_secret`
and `TF_VAR_prod_client_secret`.
2. Specify the clients you want to import in the input.json file
   1. `clientID` should be the client's ID, and _not_ the client name or UUID. 
   2. `clientType` can be: `basic`, `data source` or `payara`.
3. Write the information in the `configuration.properties` file. The `phase` value should be set to 1.
4. run the script. A folder with the name of the realm should appear on the path specified on `outputPath`
5. Move this folder to the Terraform repo.
6. Run `imports.bash` on the Terraform repo. 
7. Change the `phase` value to 2. 
8. Move this folder to the Terraform repo.
9. Run `imports.bash` on the Terraform repo. 



### Extra Information
#### Things to note: 
We made a few assumptions on how the terraform repository is arranged. 
Client resources in the project is stored in this particular module structure:
`module.ENVIRONENT.module.REALM.module.CLIENT.*`,
where `*` can be keycloak resources or other modules that contain the client configurations.

#### Why do we need to copy the folder and run the import twice?
Some resources depend on other resources. For example, 
the service account of a client might need role information from another client. 
Because of the way we're storing client roles in terraform, an error occurs when we try to 
import multiple roles that other resources refer to. 

The first import/copy pasting essentially imports the client roles (and other resource that don't 
"depend" on other resources from other clients). The second import/copy pasting imports resources
that "depend" on the resources we imported on the first stage. 

