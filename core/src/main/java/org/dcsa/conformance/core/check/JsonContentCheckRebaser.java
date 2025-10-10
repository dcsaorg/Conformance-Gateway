package org.dcsa.conformance.core.check;

import java.util.List;

import static org.dcsa.conformance.core.check.JsonAttribute.rebaserFor;

public interface JsonContentCheckRebaser {


  default JsonRebaseableContentCheck offset(JsonRebaseableContentCheck jsonRebaseableContentCheck) {
    JsonContentMatchedValidation m = offset(jsonRebaseableContentCheck::validate);
    return new JsonAttribute.JsonRebaseableCheckImpl(
      jsonRebaseableContentCheck.description(),
            true,
      m::validate
    );
  }

  JsonContentMatchedValidation offset(JsonContentMatchedValidation jsonContentMatchedValidation);

  static JsonContentCheckRebaser of(String path) {
    return rebaserFor(List.of(path));
  }
}
