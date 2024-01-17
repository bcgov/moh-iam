function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { AccountServiceContext } from "../../account-service/AccountServiceContext.js";
import TimeUtil from "../../util/TimeUtil.js";
import { Button, DataList, DataListItem, DataListItemRow, DataListContent, DescriptionList, DescriptionListTerm, DescriptionListDescription, DescriptionListGroup, Grid, GridItem, Label, PageSection, PageSectionVariants, Title, Tooltip, SplitItem, Split } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";
import { DesktopIcon, MobileAltIcon, SyncAltIcon } from "../../../../common/keycloak/web_modules/@patternfly/react-icons.js";
import { Msg } from "../../widgets/Msg.js";
import { ContinueCancelModal } from "../../widgets/ContinueCancelModal.js";
import { KeycloakContext } from "../../keycloak-service/KeycloakContext.js";
import { ContentPage } from "../ContentPage.js";
import { ContentAlert } from "../ContentAlert.js";
export class DeviceActivityPage extends React.Component {
  constructor(props, context) {
    super(props);

    _defineProperty(this, "context", void 0);

    _defineProperty(this, "signOutAll", keycloakService => {
      this.context.doDelete("/sessions").then(() => {
        keycloakService.logout();
      });
    });

    _defineProperty(this, "signOutSession", (device, session) => {
      this.context.doDelete("/sessions/" + encodeURIComponent(session.id)).then(() => {
        this.fetchDevices();
        ContentAlert.success("signedOutSession", [session.browser, device.os]);
      });
    });

    this.context = context;
    this.state = {
      devices: []
    };
    this.fetchDevices();
  }

  fetchDevices() {
    this.context.doGet("/sessions/devices").then(response => {
      console.log({
        response
      });
      let devices = this.moveCurrentToTop(response.data);
      this.setState({
        devices: devices
      });
    });
  } // current device and session should display at the top of their respective lists


  moveCurrentToTop(devices) {
    let currentDevice = devices[0];
    devices.forEach((device, index) => {
      if (device.current) {
        currentDevice = device;
        devices.splice(index, 1);
        devices.unshift(device);
      }
    });
    currentDevice.sessions.forEach((session, index) => {
      if (session.current) {
        const currentSession = currentDevice.sessions.splice(index, 1);
        currentDevice.sessions.unshift(currentSession[0]);
      }
    });
    return devices;
  }

  time(time) {
    return TimeUtil.format(time * 1000);
  }

  elementId(item, session, element = "session") {
    return `${element}-${session.id.substring(0, 7)}-${item}`;
  }

  findDeviceTypeIcon(session, device) {
    const deviceType = device.mobile;
    if (deviceType === true) return React.createElement(MobileAltIcon, {
      id: this.elementId("icon-mobile", session, "device")
    });
    return React.createElement(DesktopIcon, {
      id: this.elementId("icon-desktop", session, "device")
    });
  }

  findOS(device) {
    if (device.os.toLowerCase().includes("unknown")) return Msg.localize("unknownOperatingSystem");
    return device.os;
  }

  findOSVersion(device) {
    if (device.osVersion.toLowerCase().includes("unknown")) return "";
    return device.osVersion;
  }

  makeClientsString(clients) {
    let clientsString = "";
    clients.forEach((client, index) => {
      let clientName;

      if (client.hasOwnProperty("clientName") && client.clientName !== undefined && client.clientName !== "") {
        clientName = Msg.localize(client.clientName);
      } else {
        clientName = client.clientId;
      }

      clientsString += clientName;
      if (clients.length > index + 1) clientsString += ", ";
    });
    return clientsString;
  }

  isShowSignOutAll(devices) {
    if (devices.length === 0) return false;
    if (devices.length > 1) return true;
    if (devices[0].sessions.length > 1) return true;
    return false;
  }

  render() {
    return React.createElement(ContentPage, {
      title: "Sessions"
    }, React.createElement(PageSection, {
      isFilled: true,
      variant: PageSectionVariants.light
    }, React.createElement(Split, {
      hasGutter: true,
      className: "pf-u-mb-lg"
    }, React.createElement(SplitItem, {
      isFilled: true
    }, React.createElement("div", {
      id: "signedInDevicesTitle",
      className: "pf-c-content"
    }, React.createElement(Title, {
      headingLevel: "h2",
      size: "xl"
    }, React.createElement(Msg, {
      msgKey: "signedInDevices"
    })))), React.createElement(SplitItem, null, React.createElement(Tooltip, {
      content: React.createElement(Msg, {
        msgKey: "refreshPage"
      })
    }, React.createElement(Button, {
      "aria-describedby": "refresh page",
      id: "refresh-page",
      variant: "link",
      onClick: this.fetchDevices.bind(this),
      icon: React.createElement(SyncAltIcon, null)
    }, React.createElement(Msg, {
      msgKey: "refresh"
    })))), React.createElement(SplitItem, null, React.createElement(KeycloakContext.Consumer, null, keycloak => this.isShowSignOutAll(this.state.devices) && React.createElement(ContinueCancelModal, {
      buttonTitle: "signOutAllDevices",
      buttonId: "sign-out-all",
      modalTitle: "signOutAllDevices",
      modalMessage: "signOutAllDevicesWarning",
      onContinue: () => this.signOutAll(keycloak)
    })))), React.createElement(DataList, {
      className: "signed-in-device-list",
      "aria-label": Msg.localize("signedInDevices")
    }, React.createElement(DataListItem, {
      "aria-labelledby": "sessions",
      id: "device-activity-sessions"
    }, this.state.devices.map((device, deviceIndex) => {
      return React.createElement(React.Fragment, null, device.sessions.map((session, sessionIndex) => {
        return React.createElement(React.Fragment, {
          key: "device-" + deviceIndex + "-session-" + sessionIndex
        }, React.createElement(DataListItemRow, null, React.createElement(DataListContent, {
          "aria-label": "device-sessions-content",
          isHidden: false,
          className: "pf-u-flex-grow-1"
        }, React.createElement(Grid, {
          id: this.elementId("item", session),
          className: "signed-in-device-grid",
          hasGutter: true
        }, React.createElement(GridItem, {
          className: "device-icon",
          span: 1,
          rowSpan: 2
        }, React.createElement("span", null, this.findDeviceTypeIcon(session, device))), React.createElement(GridItem, {
          sm: 8,
          md: 9,
          span: 10
        }, React.createElement("span", {
          id: this.elementId("browser", session),
          className: "pf-u-mr-md session-title"
        }, this.findOS(device), " ", this.findOSVersion(device), " /", " ", session.browser), session.current && React.createElement(Label, {
          color: "green",
          id: this.elementId("current-badge", session)
        }, React.createElement(Msg, {
          msgKey: "currentSession"
        }))), React.createElement(GridItem, {
          className: "pf-u-text-align-right",
          sm: 3,
          md: 2,
          span: 1
        }, !session.current && React.createElement(ContinueCancelModal, {
          buttonTitle: "doSignOut",
          buttonId: this.elementId("sign-out", session),
          modalTitle: "doSignOut",
          buttonVariant: "secondary",
          modalMessage: "signOutWarning",
          onContinue: () => this.signOutSession(device, session)
        })), React.createElement(GridItem, {
          span: 11
        }, React.createElement(DescriptionList, {
          columnModifier: {
            sm: "2Col",
            lg: "3Col"
          }
        }, React.createElement(DescriptionListGroup, null, React.createElement(DescriptionListTerm, null, Msg.localize("ipAddress")), React.createElement(DescriptionListDescription, {
          id: this.elementId("ip", session)
        }, session.ipAddress)), React.createElement(DescriptionListGroup, null, React.createElement(DescriptionListTerm, null, Msg.localize("lastAccessedOn")), React.createElement(DescriptionListDescription, {
          id: this.elementId("last-access", session)
        }, this.time(session.lastAccess))), React.createElement(DescriptionListGroup, null, React.createElement(DescriptionListTerm, null, Msg.localize("clients")), React.createElement(DescriptionListDescription, {
          id: this.elementId("clients", session)
        }, this.makeClientsString(session.clients))), React.createElement(DescriptionListGroup, null, React.createElement(DescriptionListTerm, null, Msg.localize("started")), React.createElement(DescriptionListDescription, {
          id: this.elementId("started", session)
        }, this.time(session.started))), React.createElement(DescriptionListGroup, null, React.createElement(DescriptionListTerm, null, Msg.localize("expires")), React.createElement(DescriptionListDescription, {
          id: this.elementId("expires", session)
        }, this.time(session.expires)))))))));
      }));
    })))));
  }

}

_defineProperty(DeviceActivityPage, "contextType", AccountServiceContext);
//# sourceMappingURL=DeviceActivityPage.js.map