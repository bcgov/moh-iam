import axios from 'axios';

export default {
    getClients(keycloak, callback) {

        keycloak.updateToken().success(function () {
            var url =
                keycloak.authServerUrl +
                "admin/realms/" +
                keycloak.realm +
                "/clients";

            axios.get(url, { headers: { Authorization: 'Bearer ' + keycloak.token } })
                .then(response => {
                    callback(response.data);
                })
                .catch(e => {
                    callback(e);
                });
        });
    }
}