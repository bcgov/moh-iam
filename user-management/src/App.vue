<template>
    <v-app>
        <the-header></the-header>
        <the-nav-bar></the-nav-bar>
        <main>
            <section class="content">
                <the-sub-nav></the-sub-nav>

                <div class="col1">
                    <v-text-field v-model="userSearchInput" placeholder="Search for user"/>
                    <v-btn small v-on:click="searchUser">Search Users</v-btn>
                </div>
                <v-data-table
                        :headers="headers"
                        :items="searchResults"
                        :items-per-page="5"
                        class="elevation-1"
                ></v-data-table>

                <v-btn small v-on:click="loadClients">Load Clients</v-btn>
                <ul id="client-list">
                    <li v-for="client in clients" v-bind:key="client.clientId">{{ client.clientId }}</li>
                </ul>
            </section>
        </main>

        <v-checkbox v-model="checkbox1" :label="`Keycloak Dev Tools`"></v-checkbox>
        <KeycloakDevTools v-show="checkbox1"/>
        <the-footer></the-footer>
    </v-app>
</template>

<script>
    import TheHeader from "./components/TheHeader.vue";
    import TheFooter from "./components/TheFooter.vue";
    import TheNavBar from "./components/TheNavBar.vue";
    import TheSubNav from "./components/TheSubNav.vue";

    import {RepositoryFactory} from "./api/RepositoryFactory";
    import axios from "axios";
    import KeycloakDevTools from "./KeycloakDevTools";

    const ClientsRepository = RepositoryFactory.get("clients");

    export default {
        name: "App",
        components: {
            KeycloakDevTools,
            TheHeader,
            TheNavBar,
            TheFooter,
            TheSubNav
        },
        data() {
            return {
                headers: [
                    {
                        text: 'ID',
                        align: 'start',
                        sortable: false,
                        value: 'id',
                    },
                    {text: 'Username', value: 'username'},
                ],
                result: "",
                userSearchInput: "",
                searchResults: [],
                clients: [],
                checkbox1: false
            };
        },
        methods: {
            loadClients: function () {
                var vm = this;
                ClientsRepository.get()
                    .then(response => {
                        vm.clients = response.data;
                    })
                    .catch(e => {
                        vm.clients = e;
                    });
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

<style src=./assets/css/fonts.css></style>
<style src=./assets/css/grid.css></style>
<style src=./assets/css/main.css></style>
<style src=./assets/css/reset.css></style>
<style src=./assets/css/typography.css></style>
