package org.dcsa.conformance.core.check;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
import static org.dcsa.conformance.core.check.JsonAttribute.renderJsonPointer;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AtLeastOneMatchValidatorImpl implements MultiAttributeValidator {

  private final String contextPath;
  private final JsonNode body;
  private final JsonContentMatchedValidation validation;
  private final boolean withRelevance;

  private final List<Match> allMatches = new ArrayList<>();

  @Override
  public AttributePathBuilder at(JsonPointer pointer) {
    return new AttributePathBuilderImpl(
        List.of(new Match(null, body.at(pointer), renderJsonPointer(pointer), false)));
  }

  @Override
  public AttributePathBuilder path(String path) {
    if (path.contains("*")) {
      throw new IllegalArgumentException(
          "Segments cannot contain wildcards (a.foo*.c is not supported)");
    }
    return new AttributePathBuilderImpl(List.of(new Match(null, body.path(path), path, false)));
  }

  public ConformanceCheckResult getValidationResult() {
    if (allMatches.isEmpty()) {
      if (withRelevance) {
        return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
      }
      return ConformanceCheckResult.simple(Set.of("No elements matched the specified path"));
    }

    // Check if at least one match is valid (passes validation)
    for (var match : allMatches) {
      var result = validation.validate(match.node, concatContextPath(contextPath, match.render()));
      if (result.isConformant()) {
        return ConformanceCheckResult.simple(Set.of());
      }
    }

    // None of the matches were valid
    return ConformanceCheckResult.simple(
        Set.of(
            "At least one element must satisfy the validation, but all %d element(s) failed"
                .formatted(allMatches.size())));
  }

  @RequiredArgsConstructor
  private class AttributePathBuilderImpl implements AttributePathBuilder {

    private final List<Match> matchingNodes;

    @Override
    public AttributePathBuilder all() {
      return new AttributePathBuilderImpl(
          matchingNodes.stream().filter(Match::isArray).flatMap(Match::allInArray).toList());
    }

    @Override
    public AttributePathBuilder at(JsonPointer pointer) {
      return new AttributePathBuilderImpl(matchingNodes.stream().map(m -> m.at(pointer)).toList());
    }

    @Override
    public AttributePathBuilder path(String path) {
      return new AttributePathBuilderImpl(matchingNodes.stream().map(m -> m.path(path)).toList());
    }

    @Override
    public MultiAttributeValidator submitPath() {
      allMatches.addAll(matchingNodes);
      return AtLeastOneMatchValidatorImpl.this;
    }
  }

  @RequiredArgsConstructor
  static class Match {
    private final Match parent;
    final JsonNode node;
    private final String pathSegment;
    private final boolean isIndexNode;

    private String cached;

    String render() {
      var p = parent;
      if (p == null) {
        return pathSegment;
      }
      if (cached != null) {
        return cached;
      }
      var parentPath = p.render();
      var result = parentPath + (isIndexNode ? "" : ".") + pathSegment;
      cached = result;
      return result;
    }

    public Match at(JsonPointer jsonPointer) {
      return new Match(this, node.at(jsonPointer), renderJsonPointer(jsonPointer), false);
    }

    public Match path(String path) {
      return new Match(this, node.path(path), path, false);
    }

    public Match path(int index) {
      return new Match(this, node.path(index), "[" + index + "]", true);
    }

    public boolean isArray() {
      return node.isArray();
    }

    public Stream<Match> allInArray() {
      return IntStream.range(0, node.size()).mapToObj(this::path);
    }
  }
}

