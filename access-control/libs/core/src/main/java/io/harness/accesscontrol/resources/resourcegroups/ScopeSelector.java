package io.harness.accesscontrol.resources.resourcegroups;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ScopeSelectorKeys")
@OwnedBy(HarnessTeam.PL)
public class ScopeSelector {
  @NotNull Scope scope;
  boolean includingChildScopes;
}
