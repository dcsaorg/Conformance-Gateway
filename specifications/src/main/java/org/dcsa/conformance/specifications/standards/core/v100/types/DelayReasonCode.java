package org.dcsa.conformance.specifications.standards.core.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    maxLength = 3,
    example = "STR",
    description =
"""
Code identifying the reason for a delay, as defined by SMDG here:
https://smdg.org/documents/smdg-code-lists/delay-reason-and-port-call-activity/
""")
public class DelayReasonCode {}
