import * as React from "react";
import {
  Form,
  FormGroup,
  TextInput,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";

import { HttpResponse } from "../../account-service/account.service";
import { AccountServiceContext } from "../../account-service/AccountServiceContext";
import { Features } from "../../widgets/features";
import { Msg } from "../../widgets/Msg";
import { ContentPage } from "../ContentPage";

declare const features: Features;
declare const locale: string;

interface AccountPageProps {}

interface FormFields {
  readonly username?: string;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly email?: string;
  attributes?: { locale?: [string] };
}

interface AccountPageState {
  readonly errors: FormFields;
  readonly formFields: FormFields;
}

export class AccountPage extends React.Component<
  AccountPageProps,
  AccountPageState
> {
  static contextType = AccountServiceContext;
  context: React.ContextType<typeof AccountServiceContext>;
  private isRegistrationEmailAsUsername: boolean =
    features.isRegistrationEmailAsUsername;
  private readonly DEFAULT_STATE: AccountPageState = {
    errors: {
      username: "",
      firstName: "",
      lastName: "",
      email: "",
    },
    formFields: {
      username: "",
      firstName: "",
      lastName: "",
      email: "",
      attributes: {},
    },
  };

  public state: AccountPageState = this.DEFAULT_STATE;

  public constructor(
    props: AccountPageProps,
    context: React.ContextType<typeof AccountServiceContext>
  ) {
    super(props);
    this.context = context;

    this.fetchPersonalInfo();
  }

  private fetchPersonalInfo(): void {
    this.context!.doGet<FormFields>("/").then(
      (response: HttpResponse<FormFields>) => {
        this.setState(this.DEFAULT_STATE);
        const formFields = response.data;
        if (!formFields!.attributes) {
          formFields!.attributes = { locale: [locale] };
        } else if (!formFields!.attributes.locale) {
          formFields!.attributes.locale = [locale];
        }

        this.setState({ ...{ formFields: formFields as FormFields } });
      }
    );
  }

  public render(): React.ReactNode {
    const fields: FormFields = this.state.formFields;
    return (
      <ContentPage title="Account Information">
        <PageSection isFilled variant={PageSectionVariants.light}>
          <Form className="personal-info-form">
            {!this.isRegistrationEmailAsUsername &&
              fields.username != undefined && (
                <FormGroup label={Msg.localize("username")} fieldId="user-name">
                  <TextInput
                    isDisabled
                    type="text"
                    id="user-name"
                    name="username"
                    value={this.state.formFields.username}
                  ></TextInput>
                </FormGroup>
              )}

            <FormGroup label={Msg.localize("email")} fieldId="email-address">
              <TextInput
                isDisabled
                type="email"
                id="email-address"
                name="email"
                maxLength={254}
                value={fields.email}
              ></TextInput>
            </FormGroup>
            <FormGroup label={Msg.localize("firstName")} fieldId="first-name">
              <TextInput
                isDisabled
                type="text"
                id="first-name"
                name="firstName"
                maxLength={254}
                value={fields.firstName}
              ></TextInput>
            </FormGroup>
            <FormGroup label={Msg.localize("lastName")} fieldId="last-name">
              <TextInput
                isDisabled
                type="text"
                id="last-name"
                name="lastName"
                maxLength={254}
                value={fields.lastName}
              ></TextInput>
            </FormGroup>
          </Form>
        </PageSection>
      </ContentPage>
    );
  }
}
