<?php

namespace Drupal\mohkeycloak\Routing;

use Drupal\Core\Routing\RouteSubscriberBase;
use Drupal\Core\Routing\RoutingEvents;
use Symfony\Component\Routing\RouteCollection;

/**
 * Listens to dynamic route events.
 */
class MohKeycloakRouteSubscriber extends RouteSubscriberBase {

  public function __construct() {
  }

  /**
   * {@inheritdoc}
   */
  protected function alterRoutes(RouteCollection $collection) {
    $logger = \Drupal::logger('mohkeycloak');
    $logger->debug('alterRoutes');

    if ($route = $collection->get('user.logout')) {
      $route
        ->setDefaults([
          '_controller' => '\Drupal\mohkeycloak\Controller\MohKeycloakController::logout',
        ])
        ->setRequirements([
          '_access' => 'TRUE',
        ])
        ->setOptions([
//          'no_cache' => TRUE,
        ]);
    }
  }

  /**
   * {@inheritdoc}
   */
  public static function getSubscribedEvents() {
    // Come after field_ui.
    $events[RoutingEvents::ALTER] = ['onAlterRoutes', -200];

    return $events;
  }

}
