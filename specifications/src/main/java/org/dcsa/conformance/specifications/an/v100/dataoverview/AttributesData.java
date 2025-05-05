package org.dcsa.conformance.specifications.an.v100.dataoverview;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.dcsa.conformance.specifications.an.v100.AnSchemaCreator;
import org.dcsa.conformance.specifications.an.v100.OpenApiToolkit;
import org.dcsa.conformance.specifications.an.v100.constraints.SchemaConstraint;

public class AttributesData {
  private final ArrayList<AttributeInfo> attributeInfoList = new ArrayList<>();
  private final List<String> rootTypeNames;

  public AttributesData(OpenAPI openAPI, List<String> rootTypeNames) {
    this.rootTypeNames = rootTypeNames;
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
                        AttributeInfo attributeInfo = new AttributeInfo();
                        attributeInfo.setObjectType(typeName);
                        attributeInfo.setAttributeName(attributeName);

                        Schema<?> attributeSchema = typeAttributeProperties.get(attributeName);
                        String attributeSchemaType = attributeSchema.getType();
                        attributeInfo.setAttributeType("UNKNOWN");
                        attributeInfo.setAttributeBaseType(attributeInfo.getAttributeType());
                        attributeInfo.setRequired(requiredAttributes.remove(attributeName));
                        Integer maxLength = attributeSchema.getMaxLength();
                        attributeInfo.setSize(
                            maxLength == null || maxLength == Integer.MAX_VALUE
                                ? ""
                                : maxLength.toString()); // FIXME range, array items
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
                              attributeInfo.setAttributeType("%s list".formatted(itemType));
                              attributeInfo.setAttributeBaseType(itemType);
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
                                  attributeInfo.setAttributeType(
                                      $ref.substring("#/components/schemas/".length()));
                                  attributeInfo.setAttributeBaseType(
                                      attributeInfo.getAttributeType());
                                }
                              }
                              break;
                            }
                          case null:
                            {
                              String $ref = attributeSchema.get$ref();
                              if ($ref != null) {
                                attributeInfo.setAttributeType(
                                    $ref.substring("#/components/schemas/".length()));
                                attributeInfo.setAttributeBaseType(
                                    attributeInfo.getAttributeType());
                              } else {
                                attributeInfo.setAttributeType("string");
                                attributeInfo.setAttributeBaseType(
                                    attributeInfo.getAttributeType());
                              }
                              break;
                            }
                          default:
                            {
                              attributeInfo.setAttributeType(attributeSchemaType);
                              attributeInfo.setAttributeBaseType(attributeInfo.getAttributeType());
                              break;
                            }
                        }
                        attributeInfo.setExample(
                            Objects.requireNonNullElse(attributeSchema.getExample(), "")
                                .toString());
                        attributeInfo.setDescription(
                            Objects.requireNonNullElse(attributeSchema.getDescription(), "")
                                .trim());
                        attributeInfo.setConstraints(
                            AnSchemaCreator.constraintsByClassAndField
                                .getOrDefault(attributeInfo.getObjectType(), Map.of())
                                .getOrDefault(attributeInfo.getAttributeName(), List.of())
                                .stream()
                                .map(SchemaConstraint::getDescription)
                                .collect(Collectors.joining("\n\n")));
                        if (!attributeInfo.getConstraints().isEmpty()) {
                          attributeInfo.setDescription(
                              attributeInfo
                                  .getDescription()
                                  .substring(
                                      0,
                                      attributeInfo.getDescription().length()
                                          - attributeInfo.getConstraints().length())
                                  .trim());
                        }
                        attributeInfo.setPattern("");
                        if (attributeInfo.getAttributeType().equals("string")) {
                          String schemaPattern = attributeSchema.getPattern();
                          if (schemaPattern != null) {
                            attributeInfo.setPattern(schemaPattern);
                          }
                        }
                        attributeInfoList.add(attributeInfo);
                      });
            });
  }

  public List<List<String>> getNormalizedRows() {
    return attributeInfoList.stream()
        .map(
            attributeInfo ->
                List.of(
                    Objects.requireNonNullElse(attributeInfo.getObjectType(), ""),
                    Objects.requireNonNullElse(attributeInfo.getAttributeName(), ""),
                    Objects.requireNonNullElse(attributeInfo.getAttributeType(), ""),
                    attributeInfo.isRequired() ? "yes" : "",
                    Objects.requireNonNullElse(attributeInfo.getSize(), ""),
                    Objects.requireNonNullElse(attributeInfo.getPattern(), ""),
                    Objects.requireNonNullElse(attributeInfo.getExample(), ""),
                    Objects.requireNonNullElse(attributeInfo.getDescription(), ""),
                    Objects.requireNonNullElse(attributeInfo.getConstraints(), "")))
        .toList();
  }

  public List<List<String>> getHierarchicalRows() {
    TreeMap<String, AttributeInfo> attributesByPath = new TreeMap<>();
    rootTypeNames.forEach(
        rootTypeName ->
            attributesByPath.put(
                "%s:%s".formatted(rootTypeName, rootTypeName), new AttributeInfo()));

    Map<String, List<AttributeInfo>> attributeInfoByObjectType = new HashMap<>();
    attributeInfoList.forEach(
        attributeInfo ->
            attributeInfoByObjectType
                .computeIfAbsent(
                    attributeInfo.getObjectType(), (ignoredObjectType) -> new ArrayList<>())
                .add(attributeInfo));

    ArrayList<String> newPaths = new ArrayList<>(attributesByPath.keySet());
    while (!newPaths.isEmpty()) {
      ArrayList<String> pendingPaths = new ArrayList<>(newPaths);
      newPaths.clear();
      pendingPaths.forEach(
          existingPath -> {
            String lastObjectType = existingPath.substring(1 + existingPath.lastIndexOf(":"));
            attributeInfoByObjectType
                .getOrDefault(lastObjectType, List.of())
                .forEach(
                    attributeInfo -> {
                      String newPath =
                          "%s / %s:%s"
                              .formatted(
                                  existingPath,
                                  attributeInfo.getAttributeName(),
                                  attributeInfo.getAttributeBaseType());
                      attributesByPath.put(newPath, attributeInfo);
                      newPaths.add(newPath);
                    });
          });
    }
    return attributesByPath.entrySet().stream()
        .map(
            pathAndAttributeInfo -> {
              String path =
                  Arrays.stream(pathAndAttributeInfo.getKey().split(" / "))
                      .map(nameAndType -> nameAndType.substring(0, nameAndType.indexOf(":")))
                      .collect(Collectors.joining(" / "));
              AttributeInfo attributeInfo = pathAndAttributeInfo.getValue();
              return List.of(
                  path,
                  Objects.requireNonNullElse(attributeInfo.getAttributeName(), ""),
                  Objects.requireNonNullElse(attributeInfo.getAttributeType(), ""),
                  attributeInfo.isRequired() ? "yes" : "",
                  Objects.requireNonNullElse(attributeInfo.getSize(), ""),
                  Objects.requireNonNullElse(attributeInfo.getPattern(), ""),
                  Objects.requireNonNullElse(attributeInfo.getExample(), ""),
                  Objects.requireNonNullElse(attributeInfo.getDescription(), ""),
                  Objects.requireNonNullElse(attributeInfo.getConstraints(), ""));
            })
        .toList();
  }
}
