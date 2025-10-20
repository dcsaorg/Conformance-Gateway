package org.dcsa.conformance.core.check;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
import static org.dcsa.conformance.core.check.JsonAttribute.renderJsonPointer;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class MultiAttributeValidatorImpl implements MultiAttributeValidator {

  private final String contextPath;
  private final JsonNode body;
  private final JsonContentMatchedValidation validation;

  @Getter
  private final Set<ConformanceCheckResult> validationIssues = new HashSet<>();

  @Override
  public AttributePathBuilder at(JsonPointer pointer) {
    return new AttributePathBuilderImpl(List.of(
      new Match(
        null,
        body.at(pointer),
        renderJsonPointer(pointer),
        false
      )));
  }

  @Override
  public AttributePathBuilder path(String path) {
    if (path.contains("*")) {
      throw new IllegalArgumentException("Segments cannot contain wildcards (a.foo*.c is not supported)");
    }
    return new AttributePathBuilderImpl(List.of(new Match(
      null,
      body.path(path),
      path,
      false
    )));
  }

  @RequiredArgsConstructor
  private class AttributePathBuilderImpl implements AttributePathBuilder {

    private final List<Match> matchingNodes;

    @Override
    public AttributePathBuilder all() {
      return new AttributePathBuilderImpl(
        matchingNodes.stream()
          .filter(Match::isArray)
          .flatMap(Match::allInArray)
          .toList()
      );
    }

    @Override
    public AttributePathBuilder at(JsonPointer pointer) {
      return new AttributePathBuilderImpl(matchingNodes.stream()
        .map(m -> m.at(pointer))
        .toList());
    }

    @Override
    public AttributePathBuilder path(String path) {
      return new AttributePathBuilderImpl(matchingNodes.stream()
        .map(m -> m.path(path))
        .toList());
    }

    @Override
    public MultiAttributeValidator submitPath() {
      validateAll(matchingNodes);
      return MultiAttributeValidatorImpl.this;
    }

    private void validateAll(List<Match> matches) {
      if (matches.isEmpty()){
        validationIssues.add(ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant())));
        return;
      }
      matches.stream().map(m -> validation.validate(m.node, concatContextPath(contextPath, m.render())))
        .filter(s -> !s.getErrorMessages().isEmpty())
        .forEach(validationIssues::add);
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
      var result =  parentPath + (isIndexNode ? "" : ".") + pathSegment;
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
      return IntStream.range(0, node.size())
        .mapToObj(this::path);
    }
  }

}


