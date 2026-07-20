Perform Use Case 8: Confirm/Decline the amendment to the booking WITH_CBR_OR_CBRR_PLACEHOLDER.

* If confirmed, replace the original booking with a copy of the amendment booking.
* If declined, add one or more reasons for declining the amendment to the `feedbacks` array.
* Set the original booking status to `CONFIRMED` or `PENDING_AMENDMENT`.
* Set the amended booking status to `AMENDMENT_CONFIRMED` or `AMENDMENT_DECLINED`.
* Use a compatible status combination:
  * `AMENDMENT_CONFIRMED` requires booking status `CONFIRMED`.
  * `AMENDMENT_DECLINED` permits booking status `CONFIRMED` or `PENDING_AMENDMENT`.

