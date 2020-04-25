<template>
  <div id="user-info">
    <h1>Update - {{ userName }}</h1>
    <v-row no-gutters>
      <v-col class="col-7">
        <label for="user-name" class="disabled">User Name</label>
        <v-text-field dense outlined disabled id="user-name" v-model="user.username" />

        <label for="first-name">First Name</label>
        <v-text-field dense outlined id="first-name" v-model="user.firstName" />

        <label for="last-name">Last Name</label>
        <v-text-field dense outlined id="last-name" v-model="user.lastName" />
      </v-col>
    </v-row>

    <v-card outlined class="subgroup">
      <h2>Permissions</h2>

      <v-row no-gutters>
        <v-col class="col-7">
          <label for="select-client">Application</label>
          <v-autocomplete
            id="select-client"
            outlined
            dense
            :items="clients"
            item-text="clientId"
            item-value="id"
            placeholder="Select an Application"
            v-model="selectedClientId"
            v-on:change="getUserClientRoles();"
          ></v-autocomplete>
        </v-col>
        <v-col class="col-7">
          <label v-show="selectedClientId">Roles</label>
          <div class="checkbox-group" v-if="selectedClientId">
            <v-checkbox
              hide-details="auto"
              v-for="role in rolesOfSelectedClient" 
              v-model="selectedRoles"
              :value="role"
              :label="role.name"
              v-bind:key="role.name"
            ></v-checkbox>
          </div>
          <div class="my-6">
            <v-btn class="secondary" medium v-on:click="postUserClientRoles">Save User Role</v-btn>
            {{ selectedRoles }}
          </div>
        </v-col>
      </v-row>
    </v-card>
  </div>
</template>

<script>
import { RepositoryFactory } from "./../api/RepositoryFactory";
const ClientsRepository = RepositoryFactory.get("clients");
const UsersRepository = RepositoryFactory.get("users");

export default {
  name: "UserInfo",
  props: ['user'],
  data() {
    return {
      userId: "eea8d978-02e0-4883-828f-42316626ade9",
      clients: [],
      selectedClientId: null,
      rolesOfSelectedClient: [],
      selectedRoles: [],
      userName: "123-tschiavo",
      firstName: "trevor",
      lastName: "Schiavone"
    };
  },
  methods: {
    getUserClientRoles: function() {
      this.rolesOfSelectedClient = [],
      this.selectedRoles = [],
      this.getUserEffectiveClientRoles(this.getUserAvailableClientRoles);
    },
    getUserAvailableClientRoles: function() {
      UsersRepository.getUserAvailableClientRoles(
        this.userId,
        this.selectedClientId
      )
        .then(response => {
          this.rolesOfSelectedClient.push(...response.data);
        })
        .catch(e => {
          console.log(e);
        });
    },
    getUserEffectiveClientRoles: function(callback) {
      UsersRepository.getUserEffectiveClientRoles(
        this.userId,
        this.selectedClientId
      )
        .then(response => {
          this.rolesOfSelectedClient.push(...response.data);
          this.selectedRoles.push(...response.data);
          callback()
        })
        .catch(e => {
          console.log(e);
        });
    },
    postUserClientRoles: function() {}
  },
  mounted() {
    ClientsRepository.get()
      .then(response => {
        this.clients = response.data;
      })
      .catch(e => {
        console.log(e);
      });
  }
};
</script>