import { kcRequest } from "./Repository";

const resource = "/clients";

export default {
    get() {
        return kcRequest().get(`${resource}`);
    }
}