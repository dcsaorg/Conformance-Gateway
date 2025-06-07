package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;

@Schema(
    description =
        org.dcsa.conformance.specifications.standards.dt.v100.model.ConsignmentItem
            .CLASS_SCHEMA_DESCRIPTION)
@ClearSchemaConstraints
public class ConsignmentItem
    extends org.dcsa.conformance.specifications.standards.dt.v100.model.ConsignmentItem {}
