<?php

namespace Drupal\mohkeycloak\Controller;

use Drupal\Core\Controller\ControllerBase;
use Drupal\Core\Routing\TrustedRedirectResponse;
use Drupal\Core\Url;
use Drupal\openid_connect\OpenIDConnectClaims;

/**
 * Implements Keycloak and Drupal logout.
 *
 * @package Drupal\mohkeycloak\Controller
 */
class MohKeycloakController extends ControllerBase {

  /**
   * Logs the user out of Drupal and Keycloak.
   *
   * Note that this method attempts to log the user out of Keycloak even if the user was not logged in using Keycloak,
   * for example, the admin user.
   *
   * @return TrustedRedirectResponse
   */
  public function logout() {
    // Logout from Drupal.
    user_logout();
    // Logout from Keycloak and redirect back to the Drupal front page.
    $logout_redirect = Url::fromRoute('<front>', [], ['absolute' => TRUE])->toString(true)->getGeneratedUrl();
    return new TrustedRedirectResponse('https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications/protocol/openid-connect/logout?redirect_uri=' . $logout_redirect, 302);
  }

}
