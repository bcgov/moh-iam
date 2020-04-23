<template>
  <div id="user-info">
    <h1>Update - {{ username }}</h1>

    <div class="col2">
      <label>User Name</label>
      <input type="text" disabled="true" id="first-name" v-model="username"/>
    </div>
    <div class="col2">
      <label>First Name</label>
      <input type="text" id="first-name" />
    </div>
    <div class="col2">
      <label>Last Name</label>
      <input type="text" id="last-name" />
    </div>
  </div>
</template>

<script>
export default {
  name: "UserInfo",
  data() {
    return {
      username: "123-tschiavo"
    };
  }
};
</script>