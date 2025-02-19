Send a %s (POST) request. Allows the provider to CANCEL a Port Call Service, signaling that the Port Call Service is no
longer going to happen.

* In the path, you must use the `portCallServiceID` (UUIDv4), which identifies the Port Call Service to cancel.
* In the message body, optionally: You can provide a `reason` for the cancellation.
