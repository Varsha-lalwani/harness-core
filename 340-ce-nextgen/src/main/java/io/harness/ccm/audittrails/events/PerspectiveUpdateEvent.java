/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.audittrails.events;

import io.harness.ccm.views.entities.CEView;
import io.harness.event.Event;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PerspectiveUpdateEvent implements Event {
  public static final String PERSPECTIVE_UPDATED = "PerspectiveUpdated";
  private CEView oldPerspectiveDTO;
  private CEView newPerspectiveDTO;
  private String accountIdentifier;

  public PerspectiveUpdateEvent(String accountIdentifier, CEView newPerspectiveDTO, CEView oldPerspectiveDTO) {
    this.accountIdentifier = accountIdentifier;
    this.newPerspectiveDTO = newPerspectiveDTO;
    this.oldPerspectiveDTO = oldPerspectiveDTO;
  }

  @Override
  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, newPerspectiveDTO.getUuid());
  }

  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newPerspectiveDTO.getName());
    return Resource.builder().identifier(newPerspectiveDTO.getUuid()).type("PERSPECTIVE").labels(labels).build();
  }

  @Override
  public String getEventType() {
    return PERSPECTIVE_UPDATED;
  }
}
