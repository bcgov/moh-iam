<template>
    <div>
        <v-row no-gutters>
            <v-col class="col-6">
                <label for="user-search">Search
                    <v-tooltip right>
                        <template v-slot:activator="{ on }">
                            <v-icon v-on="on" small>mdi-help-circle</v-icon>
                        </template>
                        <span>Search by Username, email, name, or ID</span>
                    </v-tooltip>
                </label>
                
                
                <v-text-field id="user-search" outlined dense v-model="userSearchInput"
                            placeholder="Username, email, name, or ID"
                            @keyup.native.enter="searchUser"/>
            </v-col>
            <v-col class="col-1">
                <v-btn id="search-button" class="secondary" medium v-on:click="searchUser">Search Users</v-btn>
            </v-col>
        </v-row>
        <div class="col4">
            <v-data-table
                    :headers="headers"
                    :items="searchResults"
                    :items-per-page="5"
                    v-on:click:row="selectUser"
                    class="elevation-1"
            ></v-data-table>
        </div>
    </div>
</template>

<script>
    import axios from "axios";

    export default {
        name: "UserSearch",

        data() {
            return {
                headers: [
                    {text: "Username", value: "username"},
                    {text: "First name", value: "firstName"},
                    {text: "Last name", value: "lastName"},
                    {text: "Email", value: "email"},
                    {text: "Enabled", value: "enabled"},
                    {text: "ID", value: "id"}
                ],
                result: "",
                userSearchInput: "",
                searchResults: [],
                clients: [],
            };
        },
        methods: {
            selectUser(user) {
                this.$emit('userSelected', user);
            },
            searchUser: function () {
                var vm = this;
                this.$keycloak.updateToken().success(function () {
                    var url =
                    vm.$keycloak.authServerUrl +
                    "admin/realms/" +
                    vm.$keycloak.realm +
                    "/users?briefRepresentation=true&first=0&max=20&search=" +
                    vm.userSearchInput;

                    axios
                    .get(url, {
                        headers: {Authorization: "Bearer " + vm.$keycloak.token}
                    })
                    .then(response => {
                        vm.result = response.data;
                        vm.searchResults = response.data;
                    })
                    .catch(e => {
                        vm.result = e;
                    });
                });
            }
        }
    };
</script>

<style scoped>
#search-button {
    margin-top: 25px;
    margin-left: 20px;
}
</style>