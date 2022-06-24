/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.steadystate.watcher.workload;

import io.harness.k8s.model.Kind;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class K8sWorkloadWatcherConfig {
  @Inject private static DeploymentApiWatcher deploymentApiWatcher;
  @Inject private static StatefulSetApiWatcher statefulSetApiWatcher;
  @Inject private static DaemonSetApiWatcher daemonSetApiWatcher;
  @Inject private static JobApiWatcher jobApiWatcher;
  @Inject private static DeploymentConfigCliWatcher deploymentConfigCliWatcher;
  @Inject private static JobCliWatcher jobCliWatcher;

  private static final EnumMap<Kind, WorkloadWatcher> apiWorkloadWatcherMap = new EnumMap<>(
      Map.of(Kind.Deployment, deploymentApiWatcher, Kind.StatefulSet, statefulSetApiWatcher, Kind.DaemonSet,
          daemonSetApiWatcher, Kind.Job, jobApiWatcher, Kind.DeploymentConfig, deploymentConfigCliWatcher));

  private static final EnumMap<Kind, WorkloadWatcher> cliWorkloadWatcherMap =
      new EnumMap<>(Map.of(Kind.Job, jobCliWatcher, Kind.DeploymentConfig, deploymentConfigCliWatcher));

  public EnumMap<Kind, WorkloadWatcher> getApiWorkloadWatcherMap() {
    return apiWorkloadWatcherMap;
  }

  public EnumMap<Kind, WorkloadWatcher> getCliWorkloadWatcherMap() {
    return cliWorkloadWatcherMap;
  }
}
