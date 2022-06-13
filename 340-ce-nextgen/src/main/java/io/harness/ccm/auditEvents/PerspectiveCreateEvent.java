/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.auditEvents;

import io.harness.ccm.views.entities.CEView;
import io.harness.event.Event;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;

import java.util.HashMap;
import java.util.Map;

public class PerspectiveCreateEvent implements Event {
  private CEView perspective;
  private String accountIdentifier;

  public PerspectiveCreateEvent(String accountIdentifier, CEView perspective) {
    this.perspective = perspective;
    this.accountIdentifier = accountIdentifier;
  }

  @Override
  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, perspective.getUuid());
  }

  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, perspective.getName());
    return Resource.builder().identifier(perspective.getUuid()).type("PERSPECTIVE").labels(labels).build();
  }

  @Override
  public String getEventType() {
    return "PerspectiveCreated";
  }
}
