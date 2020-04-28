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
                    :items-per-page="3"
                    :footer-props="footerProps"
                    v-on:click:row="selectUser"
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
                    {text: "Username", value: "username", class: "table-header"},
                    {text: "First name", value: "firstName", class: "table-header" },
                    {text: "Last name", value: "lastName", class: "table-header"},
                    {text: "Email", value: "email", class: "table-header"},
                    {text: "Enabled", value: "enabled", class: "table-header"},
                    {text: "ID", value: "id",  class: "table-header"}
                ],
                footerProps: { 'items-per-page-options': [20] },
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

<style>
#search-button {
    margin-top: 25px;
    margin-left: 20px;
}
</style>