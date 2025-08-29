# DCSA Container Tracking API

This is the OpenAPI specification of the **DCSA Container Tracking** standard.

This API allows the transfer of structured container tracking events from a publisher to a subscriber.

The event publisher implements the `GET /events` endpoint, which can be called by authorized API consumers to retrieve relevant available events.

The event subscribers implement the `POST /events` endpoint, which can be called by event publishers call to send relevant events as they become available.

The registration of event subscribers with event publishers is out of scope.

The authentication and authorization in both directions between event publishers and subscribers is out of scope.

### Work in progress ⚠️

The DCSA Container Tracking standard, including this API, is currently being designed and is **not** ready for general adoption yet.
