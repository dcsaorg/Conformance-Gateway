package org.dcsa.conformance.specifications.an.v100.dataoverview;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.dcsa.conformance.specifications.an.v100.OpenApiToolkit;

public class AttributesData {
  private final ArrayList<List<String>> normalizedRows = new ArrayList<>();

  public AttributesData(OpenAPI openAPI) {
    Map<String, Schema<?>> schemas =
      OpenApiToolkit.parameterizeStringRawSchemaMap(openAPI.getComponents().getSchemas());
    new TreeSet<>(schemas.keySet())
      .forEach(
        typeName -> {
          Schema<?> typeSchema = schemas.get(typeName);
          Set<String> requiredAttributes =
            new HashSet<>(
              Objects.requireNonNullElse(typeSchema.getRequired(), Collections.emptySet()));
          Map<String, Schema<?>> typeAttributeProperties =
            OpenApiToolkit.parameterizeStringRawSchemaMap(typeSchema.getProperties());
          new TreeSet<>(typeAttributeProperties.keySet())
            .forEach(
              attributeName -> {
                Schema<?> attributeSchema = typeAttributeProperties.get(attributeName);
                String attributeSchemaType = attributeSchema.getType();
                String csvType = "UNKNOWN";
                String csvRequired = requiredAttributes.remove(attributeName) ? "yes" : "";
                Integer maxLength = attributeSchema.getMaxLength();
                String csvSize =
                  maxLength == null || maxLength == Integer.MAX_VALUE
                    ? ""
                    : maxLength.toString(); // FIXME range, array items
                switch (attributeSchemaType) {
                  case "array":
                  {
                    Schema<?> itemSchema = attributeSchema.getItems();
                    String itemType = itemSchema.getType();
                    if (itemType == null) {
                      String $ref = itemSchema.get$ref();
                      if ($ref != null) {
                        itemType = $ref.substring("#/components/schemas/".length());
                      } else {
                        itemType = "UNKNOWN";
                      }
                    }
                    csvType = "%s list".formatted(itemType);
                    break;
                  }
                  case "object":
                  {
                    List<Schema<?>> allOf =
                      OpenApiToolkit.parameterizeRawSchemaList(
                        attributeSchema.getAllOf());
                    if (allOf.size() == 1) {
                      String $ref = allOf.getFirst().get$ref();
                      if ($ref != null) {
                        csvType = $ref.substring("#/components/schemas/".length());
                      }
                    }
                    break;
                  }
                  case null:
                  {
                    String $ref = attributeSchema.get$ref();
                    if ($ref != null) {
                      csvType = $ref.substring("#/components/schemas/".length());
                    } else {
                      csvType = "string";
                    }
                    break;
                  }
                  default:
                  {
                    csvType = attributeSchemaType;
                    break;
                  }
                }
                String csvExample =
                  Objects.requireNonNullElse(attributeSchema.getExample(), "").toString();
                String csvDescription =
                  Objects.requireNonNullElse(attributeSchema.getDescription(), "").trim();
                String csvPattern = "";
                if (csvType.equals("string")) {
                  String schemaPattern = attributeSchema.getPattern();
                  if (schemaPattern != null) {
                    csvPattern = schemaPattern;
                  }
                }
                normalizedRows.add(
                  List.of(
                    typeName,
                    attributeName,
                    csvType,
                    csvRequired,
                    csvSize,
                    csvPattern,
                    csvExample,
                    csvDescription));
              });
        });

  }

  public List<List<String>> getNormalizedRows() {
    return Collections.unmodifiableList(normalizedRows);
  }
}
