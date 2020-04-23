<template>
  <div id="user-info">
    <h1>Update - {{ username }}</h1>

    <div class="col2">
      <label class="disabled" for="user-name">User Name</label>
      <input type="text" disabled="true" id="user-name" v-model="username"/>
    </div>
    <div class="col2">
      <label for="first-name">First Name</label>
      <input type="text" id="first-name" />
    </div>
    <div class="col2">
      <label for="last-name">Last Name</label>
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