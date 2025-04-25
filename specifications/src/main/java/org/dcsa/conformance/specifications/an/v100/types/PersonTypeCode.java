package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    type = "string",
    example = "LEGAL_PERSON",
    description =
"""
One of the person types defined by the Union Customs Code art. 5(4)
""")
@AllArgsConstructor
public enum PersonTypeCode implements EnumBase {
  NATURAL_PERSON(
"""
A person that is an individual living human being
"""),
  LEGAL_PERSON(
"""
Person (including a human being and public or private organizations) that can perform legal actions,
such as own a property, sue and be sued
"""),
  ASSOCIATION_OF_PERSONS(
"""
Not a legal person, but recognised under Union or National law as having the capacity to perform legal acts
""");

  @Getter private final String valueDescription;
}
