<template>
  <div id="user-info">
    <v-alert v-model="alertSuccess" type="success" dismissible>{{ successMessage }}</v-alert>
    <v-alert v-model="alertError" type="error" dismissible>{{ errorMessage }}</v-alert>

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
              v-for="role in effectiveClientRoles"
              v-model="selectedRoles"
              :value="role"
              :label="role.name"
              v-bind:key="role.name"
            ></v-checkbox>
            <v-checkbox
              hide-details="auto"
              v-for="role in availableClientRoles"
              v-model="selectedRoles"
              :value="role"
              :label="role.name"
              v-bind:key="role.name"
            ></v-checkbox>
          </div>
          <div class="my-6">
            <v-btn class="secondary" medium v-on:click="updateUserClientRoles()">Save User Role</v-btn>
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
  props: ["user"],
  data() {
    return {
      alertSuccess: false,
      alertError: false,
      successMessage: "",
      errorMessage: "",
      userId: "eea8d978-02e0-4883-828f-42316626ade9",
      clients: [],
      selectedClientId: null,
      effectiveClientRoles: [],
      availableClientRoles: [],
      selectedRoles: []
    };
  },
  methods: {
    getUserClientRoles: function() {
      this.effectiveClientRoles = [];
      this.availableClientRoles = [];
      this.selectedRoles = [];
      this.getUserEffectiveClientRoles();
      this.getUserAvailableClientRoles();
    },
    getUserAvailableClientRoles: function() {
      UsersRepository.getUserAvailableClientRoles(
        this.user.id,
        this.selectedClientId
      )
        .then(response => {
          this.availableClientRoles.push(...response.data);
        })
        .catch(e => {
          console.log(e);
        });
    },
    getUserEffectiveClientRoles: function() {
      UsersRepository.getUserEffectiveClientRoles(
        this.user.id,
        this.selectedClientId
      )
        .then(response => {
          this.effectiveClientRoles.push(...response.data);
          this.selectedRoles.push(...response.data);
        })
        .catch(e => {
          console.log(e);
        });
    },
    updateUserClientRoles: function() {
      //If in effective but not selected DELETE
      var rolesToDelete = this.effectiveClientRoles.filter(
        value => !this.selectedRoles.includes(value)
      );

      //If in available and selected ADD
      var rolesToAdd = this.availableClientRoles.filter(value =>
        this.selectedRoles.includes(value)
      );

      this.successMessage = "";
      this.errorMessage = "";

      if (rolesToDelete.length > 0) {
        this.deleteUserClientRoles(rolesToDelete);
      }
      if (rolesToAdd.length > 0) {
        this.addUserClientRoles(rolesToAdd);
      }    
    },
    deleteUserClientRoles: function(rolesToDelete) {
      UsersRepository.deleteUserClientRoles(
        this.user.id,
        this.selectedClientId,
        rolesToDelete
      )
        .then(response => {
          this.alertSuccess = true;
          this.successMessage =
            this.successMessage + "Roles Added Successfully ";
            console.log(response);
        })
        .catch(error => {
          this.alertError = true;
          this.errorMessage = this.errorMessage + "Error Adding Roles";
          console.log(error);
        });
    },
    addUserClientRoles: function(rolesToAdd) {
      UsersRepository.addUserClientRoles(
        this.user.id,
        this.selectedClientId,
        rolesToAdd
      )
        .then(response => {
          this.alertSuccess = true;
          this.successMessage =
            this.successMessage + "Roles Removed Successfully ";
          console.log(response);
        })
        .catch(error => {
          this.alertError = true;
          this.errorMessage = this.errorMessage + "Error Removing Roles";
          console.log(error);
        });
    }
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