package org.dcsa.conformance.core.check;

import java.util.List;

import static org.dcsa.conformance.core.check.JsonAttribute.rebaserFor;

public interface JsonContentCheckRebaser {


  default JsonRebasableContentCheck offset(JsonRebasableContentCheck jsonRebasableContentCheck) {
    JsonContentMatchedValidation m = offset(jsonRebasableContentCheck::validate);
    return JsonAttribute.JsonRebasableCheckImpl.of(
      jsonRebasableContentCheck.description(),
      m::validate
    );
  }

  JsonContentMatchedValidation offset(JsonContentMatchedValidation jsonContentMatchedValidation);

  static JsonContentCheckRebaser of(String path) {
    return rebaserFor(List.of(path));
  }
}
