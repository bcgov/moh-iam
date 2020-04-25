<template>
    <div>
        <div class="col1">
            <v-text-field outlined dense label="Search" v-model="userSearchInput"
                          placeholder="Username, email, name, or ID"
                          @keyup.native.enter="searchUser"/>
            <v-btn class="secondary" medium v-on:click="searchUser">Search Users</v-btn>
        </div>
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
                console.log("select user");
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
</style>