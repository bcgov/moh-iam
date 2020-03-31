<?php

namespace Drupal\mohkeycloak\Routing;

use Drupal\Core\Routing\RouteSubscriberBase;
use Drupal\Core\Routing\RoutingEvents;
use Symfony\Component\Routing\RouteCollection;

/**
 * Alters the Drupal user logout route to delegate logout to the MohKeycloakController.
 */
class MohKeycloakRouteSubscriber extends RouteSubscriberBase {

  protected function alterRoutes(RouteCollection $collection) {
    if ($route = $collection->get('user.logout')) {
      $route
        ->setDefaults([
          '_controller' => '\Drupal\mohkeycloak\Controller\MohKeycloakController::logout',
        ])
        ->setRequirements([
          '_access' => 'TRUE',
        ])
        ->setOptions([
          'no_cache' => TRUE,
        ]);
    }
  }

  public static function getSubscribedEvents() {
    // Come after field_ui.
    $events[RoutingEvents::ALTER] = ['onAlterRoutes', -200];
    return $events;
  }

}
