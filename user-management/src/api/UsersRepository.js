import { kcRequest } from "./Repository";

const resource = "/users";
const clientRoleMappings = "role-mappings/clients";

export default {
    get() {
        return kcRequest().get(`${resource}`);
    },
    
    getUser(userId){
        return kcRequest().get(`${resource}/${userId}`)
    },

    getUserAvailableClientRoles(userId, clientId) {
        return kcRequest().get(`${resource}/${userId}/${clientRoleMappings}/${clientId}/available`)
    },

    getUserEffectiveClientRoles(userId, clientId) {
        return kcRequest().get(`${resource}/${userId}/${clientRoleMappings}/${clientId}/composite`)
    }
}