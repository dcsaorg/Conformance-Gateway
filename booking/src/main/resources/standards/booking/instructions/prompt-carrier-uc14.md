Perform Use Case 14: Confirm/Decline the cancellation of the booking WITH_CBR_OR_CBRR_PLACEHOLDER.

* Set the booking cancellation status to `CANCELLATION_CONFIRMED` or `CANCELLATION_DECLINED`.
* Set the original booking status to `CANCELLED`, `CONFIRMED`, or `PENDING_AMENDMENT`.
* If an amendment exists and the cancellation is confirmed, set the amended booking status to `AMENDMENT_CANCELLED`.
* Use a compatible status combination:
  * `CANCELLATION_CONFIRMED` requires booking status `CANCELLED` and permits an absent or `AMENDMENT_CANCELLED` amended booking status.
  * `CANCELLATION_DECLINED` requires booking status `CONFIRMED` or `PENDING_AMENDMENT` and permits an existing amendment status to remain unchanged.

