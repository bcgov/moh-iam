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

    // Get the URL of the Keycloak logout endpoint from config.
    $sign_out_endpoint = \Drupal::config("openid_connect.settings.mohkeycloak")->get('settings.sign_out_endpoint_kc');

    // Create a link to the Drupal front page.
    // Keycloak takes a redirect_uri query parameter to know where to redirect the browser to after logging out of Keycloak.
    $logout_redirect = Url::fromRoute('<front>', [], ['absolute' => TRUE])->toString(true)->getGeneratedUrl();

    // Logout from Keycloak and redirect back to the Drupal front page.
    return new TrustedRedirectResponse($sign_out_endpoint . '?redirect_uri=' . $logout_redirect, 302);
  }

}
