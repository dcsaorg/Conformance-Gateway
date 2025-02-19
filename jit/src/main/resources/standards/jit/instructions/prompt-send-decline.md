Send a %s (POST) request. Allows the consumer to DECLINE a Port Call Service, signaling that the Port Call Service is no
longer going to happen.

* In the path, you must use a `portCallServiceID` (UUIDv4), which identifies the Port Call Service to decline.
* In the message body, optionally: You can provide a `reason` for the cancellation.
