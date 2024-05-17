function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { Form, FormGroup, TextInput, PageSection, PageSectionVariants } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentPage } from "../ContentPage.js";
export class AccountPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "isRegistrationEmailAsUsername", features.isRegistrationEmailAsUsername);

    _defineProperty(this, "DEFAULT_STATE", {
      errors: {
        username: "",
        firstName: "",
        lastName: "",
        email: ""
      },
      formFields: {
        username: "",
        firstName: "",
        lastName: "",
        email: "",
        attributes: {}
      }
    });

    _defineProperty(this, "state", this.DEFAULT_STATE);

    this.context = context;
    this.fetchPersonalInfo();
  }

  fetchPersonalInfo() {
    this.context.doGet("/").then(response => {
      this.setState(this.DEFAULT_STATE);
      const formFields = response.data;

      if (!formFields.attributes) {
        formFields.attributes = {
          locale: [locale]
        };
      } else if (!formFields.attributes.locale) {
        formFields.attributes.locale = [locale];
      }

      this.setState({ ...{
          formFields: formFields
        }
      });
    });
  }

  render() {
    const fields = this.state.formFields;
    return React.createElement(ContentPage, {
      title: "Account Information"
    }, React.createElement(PageSection, {
      isFilled: true,
      variant: PageSectionVariants.light
    }, React.createElement(Form, {
      className: "personal-info-form"
    }, !this.isRegistrationEmailAsUsername && fields.username != undefined && React.createElement(FormGroup, {
      label: Msg.localize("username"),
      fieldId: "user-name"
    }, React.createElement(TextInput, {
      isDisabled: true,
      type: "text",
      id: "user-name",
      name: "username",
      value: this.state.formFields.username
    })), React.createElement(FormGroup, {
      label: Msg.localize("email"),
      fieldId: "email-address"
    }, React.createElement(TextInput, {
      isDisabled: true,
      type: "email",
      id: "email-address",
      name: "email",
      maxLength: 254,
      value: fields.email
    })), React.createElement(FormGroup, {
      label: Msg.localize("firstName"),
      fieldId: "first-name"
    }, React.createElement(TextInput, {
      isDisabled: true,
      type: "text",
      id: "first-name",
      name: "firstName",
      maxLength: 254,
      value: fields.firstName
    })), React.createElement(FormGroup, {
      label: Msg.localize("lastName"),
      fieldId: "last-name"
    }, React.createElement(TextInput, {
      isDisabled: true,
      type: "text",
      id: "last-name",
      name: "lastName",
      maxLength: 254,
      value: fields.lastName
    })))));
  }

}

_defineProperty(AccountPage, "contextType", AccountServiceContext);
//# sourceMappingURL=AccountPage.js.map