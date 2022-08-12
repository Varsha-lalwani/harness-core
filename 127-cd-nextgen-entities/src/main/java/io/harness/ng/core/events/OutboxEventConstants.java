/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class OutboxEventConstants {
  public static final String SERVICE_CREATED = "ServiceCreated";
  public static final String SERVICE_UPDATED = "ServiceUpdated";
  public static final String SERVICE_DELETED = "ServiceDeleted";
  public static final String SERVICE_UPSERTED = "ServiceUpserted";
  public static final String SERVICE_OVERRIDE_CREATED = "ServiceOverrideCreated";
  public static final String SERVICE_OVERRIDE_UPDATED = "ServiceOverrideUpdated";
  public static final String SERVICE_OVERRIDE_UPSERTED = "ServiceOverrideUpserted";
  public static final String SERVICE_OVERRIDE_DELETED = "ServiceOverrideDeleted";
  public static final String INFRASTRUCTURE_DEF_CREATED = "InfrastructureDefCreated";
  public static final String INFRASTRUCTURE_DEF_UPDATED = "InfrastructureDefUpdated";
  public static final String INFRASTRUCTURE_DEF_UPSERTED = "InfrastructureDefUpserted";
  public static final String INFRASTRUCTURE_DEF_DELETED = "InfrastructureDefDelete";
  public static final String ENVIRONMENT_CREATED = "EnvironmentCreated";
  public static final String ENVIRONMENT_UPDATED = "EnvironmentUpdated";
  public static final String ENVIRONMENT_DELETED = "EnvironmentDeleted";
  public static final String ENVIRONMENT_UPSERTED = "EnvironmentUpserted";

  // EnvironmentGroup
  public static final String ENVIRONMENT_GROUP_CREATED = "EnvironmentGroupCreated";
  public static final String ENVIRONMENT_GROUP_DELETED = "EnvironmentGroupDeleted";
  public static final String ENVIRONMENT_GROUP_UPDATED = "EnvironmentGroupUpdated";
}
