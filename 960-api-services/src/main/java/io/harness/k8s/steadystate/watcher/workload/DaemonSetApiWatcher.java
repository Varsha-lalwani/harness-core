/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.steadystate.watcher.workload;

import io.harness.configuration.KubernetesCliCommandType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.KubernetesCliTaskRuntimeException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.steadystate.model.K8ApiResponseDTO;
import io.harness.k8s.steadystate.model.K8sStatusWatchDTO;
import io.harness.k8s.steadystate.statusviewer.DaemonSetStatusViewer;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DaemonSetApiWatcher implements WorkloadWatcher {
  @Inject private DaemonSetStatusViewer statusViewer;

  @Override
  public boolean watchRolloutStatus(K8sStatusWatchDTO k8SStatusWatchDTO, KubernetesResourceId workload,
      LogCallback executionLogCallback) throws Exception {
    return watchDaemonSet(
        k8SStatusWatchDTO.getApiClient(), workload, executionLogCallback, k8SStatusWatchDTO.isErrorFrameworkEnabled());
  }

  private boolean watchDaemonSet(ApiClient apiClient, KubernetesResourceId workload, LogCallback executionLogCallback,
      boolean errorFrameworkEnabled) {
    Preconditions.checkNotNull(apiClient, "K8s API Client cannot be null.");
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    while (true) {
      try (Watch<V1DaemonSet> watch = Watch.createWatch(apiClient,
               appsV1Api.listNamespacedDaemonSetCall(
                   workload.getNamespace(), null, true, null, null, null, null, null, null, 300, true, null),
               new TypeToken<Watch.Response<V1DaemonSet>>() {}.getType())) {
        for (Watch.Response<V1DaemonSet> event : watch) {
          V1DaemonSet daemonSet = event.object;
          V1ObjectMeta meta = daemonSet.getMetadata();
          if (meta != null && !workload.getName().equals(meta.getName())) {
            continue;
          }
          switch (event.type) {
            case "ADDED":
            case "MODIFIED":
              K8ApiResponseDTO rolloutStatus = statusViewer.extractRolloutStatus(daemonSet);
              executionLogCallback.saveExecutionLog(rolloutStatus.getMessage());
              if (rolloutStatus.isDone()) {
                return true;
              }
              break;
            case "DELETED":
              throw new KubernetesCliTaskRuntimeException(
                  "object has been deleted", KubernetesCliCommandType.STEADY_STATE_CHECK);
            default:
              throw new KubernetesCliTaskRuntimeException(
                  String.format("unexpected k8s event %s", event.type), KubernetesCliCommandType.STEADY_STATE_CHECK);
          }
        }
      } catch (IOException e) {
        IOException ex = ExceptionMessageSanitizer.sanitizeException(e);
        String errorMessage = "Failed to close Kubernetes watch." + ExceptionUtils.getMessage(ex);
        log.error(errorMessage, ex);
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        if (errorFrameworkEnabled) {
          throw new KubernetesCliTaskRuntimeException(errorMessage, KubernetesCliCommandType.STEADY_STATE_CHECK);
        }
        return false;
      } catch (ApiException e) {
        ApiException ex = ExceptionMessageSanitizer.sanitizeException(e);
        String errorMessage = "Failed to watch daemonset rollout status. " + ExceptionUtils.getMessage(ex);
        log.error(errorMessage, ex);
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        if (errorFrameworkEnabled) {
          throw new KubernetesCliTaskRuntimeException(errorMessage, KubernetesCliCommandType.STEADY_STATE_CHECK);
        }
        return false;
      }
    }
  }
}
