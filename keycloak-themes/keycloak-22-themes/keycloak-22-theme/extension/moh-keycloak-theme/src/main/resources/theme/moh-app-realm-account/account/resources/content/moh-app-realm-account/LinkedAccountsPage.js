function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { withRouter } from "../../../../common/keycloak/web_modules/react-router-dom.js";
import { DataList, DataListItemCells, DataListCell, DataListItemRow, Label, PageSection, PageSectionVariants, Split, SplitItem, Stack, StackItem, Title, DataListItem } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { BitbucketIcon, CubeIcon, GitlabIcon, OpenshiftIcon, PaypalIcon, FacebookIcon, GoogleIcon, InstagramIcon, MicrosoftIcon, TwitterIcon, StackOverflowIcon, LinkedinIcon, GithubIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import { Msg } from "../../widgets/Msg.js";
import { ContentPage } from "../ContentPage.js";

class LinkedAccountsPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    this.context = context;
    this.state = {
      linkedAccounts: [],
      unLinkedAccounts: []
    };
    this.getLinkedAccounts();
  }

  getLinkedAccounts() {
    this.context.doGet("/linked-accounts").then(response => {
      console.log({
        response
      });
      const linkedAccounts = response.data.filter(account => account.connected);
      const unLinkedAccounts = response.data.filter(account => !account.connected);
      this.setState({
        linkedAccounts: linkedAccounts,
        unLinkedAccounts: unLinkedAccounts
      });
    });
  }

  render() {
    return React.createElement(ContentPage, {
      title: "Federated Identities"
    }, React.createElement(PageSection, {
      isFilled: true,
      variant: PageSectionVariants.light
    }, React.createElement(Stack, {
      hasGutter: true
    }, React.createElement(StackItem, null, React.createElement(Title, {
      headingLevel: "h2",
      className: "pf-u-mb-lg",
      size: "xl"
    }, "Linked Identity Providers"), React.createElement(DataList, {
      id: "linked-idps",
      "aria-label": Msg.localize("linkedLoginProviders")
    }, this.makeRows(this.state.linkedAccounts, true))), React.createElement(StackItem, null, React.createElement(Title, {
      headingLevel: "h2",
      className: "pf-u-mt-xl pf-u-mb-lg",
      size: "xl"
    }, "Unlinked Identity Providers"), React.createElement(DataList, {
      id: "unlinked-idps",
      "aria-label": Msg.localize("unlinkedLoginProviders")
    }, this.makeRows(this.state.unLinkedAccounts, false))))));
  }

  emptyRow(isLinked) {
    let isEmptyMessage = "";

    if (isLinked) {
      isEmptyMessage = Msg.localize("linkedEmpty");
    } else {
      isEmptyMessage = Msg.localize("unlinkedEmpty");
    }

    return React.createElement(DataListItem, {
      key: "emptyItem",
      "aria-labelledby": Msg.localize("isEmptyMessage")
    }, React.createElement(DataListItemRow, {
      key: "emptyRow"
    }, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: "empty"
      }, isEmptyMessage)]
    })));
  }

  makeRows(accounts, isLinked) {
    if (accounts.length === 0) {
      return this.emptyRow(isLinked);
    }

    return React.createElement(React.Fragment, null, " ", accounts.map(account => React.createElement(DataListItem, {
      id: `${account.providerAlias}-idp`,
      key: account.providerName,
      "aria-labelledby": Msg.localize("linkedAccountsTitle")
    }, React.createElement(DataListItemRow, {
      key: account.providerName
    }, React.createElement(DataListItemCells, {
      dataListCells: [React.createElement(DataListCell, {
        key: "idp"
      }, React.createElement(Split, null, React.createElement(SplitItem, {
        className: "pf-u-mr-sm"
      }, this.findIcon(account)), React.createElement(SplitItem, {
        className: "pf-u-my-xs",
        isFilled: true
      }, React.createElement("span", {
        id: `${account.providerAlias}-idp-name`
      }, account.displayName)))), React.createElement(DataListCell, {
        key: "label"
      }, React.createElement(Split, null, React.createElement(SplitItem, {
        className: "pf-u-my-xs",
        isFilled: true
      }, React.createElement("span", {
        id: `${account.providerAlias}-idp-label`
      }, this.label(account))))), React.createElement(DataListCell, {
        key: "username",
        width: 5
      }, React.createElement(Split, null, React.createElement(SplitItem, {
        className: "pf-u-my-xs",
        isFilled: true
      }, React.createElement("span", {
        id: `${account.providerAlias}-idp-username`
      }, account.linkedUsername))))]
    })))), " ");
  }

  label(account) {
    if (account.social) {
      return React.createElement(Label, {
        color: "blue"
      }, React.createElement(Msg, {
        msgKey: "socialLogin"
      }));
    }

    return React.createElement(Label, {
      color: "green"
    }, React.createElement(Msg, {
      msgKey: "systemDefined"
    }));
  }

  findIcon(account) {
    const socialIconId = `${account.providerAlias}-idp-icon-social`;
    console.log(account);

    switch (true) {
      case account.providerName.toLowerCase().includes("linkedin"):
        return React.createElement(LinkedinIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("facebook"):
        return React.createElement(FacebookIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("google"):
        return React.createElement(GoogleIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("instagram"):
        return React.createElement(InstagramIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("microsoft"):
        return React.createElement(MicrosoftIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("bitbucket"):
        return React.createElement(BitbucketIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("twitter"):
        return React.createElement(TwitterIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("openshift"):
        // return <div className="idp-icon-social" id="openshift-idp-icon-social" />;
        return React.createElement(OpenshiftIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("gitlab"):
        return React.createElement(GitlabIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("github"):
        return React.createElement(GithubIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("paypal"):
        return React.createElement(PaypalIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName.toLowerCase().includes("stackoverflow"):
        return React.createElement(StackOverflowIcon, {
          id: socialIconId,
          size: "lg"
        });

      case account.providerName !== "" && account.social:
        return React.createElement("div", {
          className: "idp-icon-social",
          id: socialIconId
        });

      default:
        return React.createElement(CubeIcon, {
          id: `${account.providerAlias}-idp-icon-default`,
          size: "lg"
        });
    }
  }

}

_defineProperty(LinkedAccountsPage, "contextType", AccountServiceContext);

const LinkedAccountsPagewithRouter = withRouter(LinkedAccountsPage);
export { LinkedAccountsPagewithRouter as LinkedAccountsPage };
//# sourceMappingURL=LinkedAccountsPage.js.map