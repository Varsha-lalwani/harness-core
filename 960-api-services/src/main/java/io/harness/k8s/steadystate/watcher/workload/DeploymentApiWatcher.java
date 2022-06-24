/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.steadystate.watcher.workload;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.steadystate.model.K8ApiResponseDTO;
import io.harness.k8s.steadystate.model.K8sStatusWatchDTO;
import io.harness.k8s.steadystate.statusviewer.DeploymentStatusViewer;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DeploymentApiWatcher implements WorkloadWatcher {
  @Inject private DeploymentStatusViewer statusViewer;

  @Override
  public boolean watchRolloutStatus(K8sStatusWatchDTO k8SStatusWatchDTO, KubernetesResourceId workload,
      LogCallback executionLogCallback) throws Exception {
    return watchDeployment(
        k8SStatusWatchDTO.getApiClient(), workload, executionLogCallback, k8SStatusWatchDTO.isErrorFrameworkEnabled());
  }

  private boolean watchDeployment(ApiClient apiClient, KubernetesResourceId deploymentResource,
      LogCallback executionLogCallback, boolean errorFrameworkEnabled) throws Exception {
    Preconditions.checkNotNull(apiClient, "K8s API Client cannot be null.");
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    while (true) {
      try (Watch<V1Deployment> watch = Watch.createWatch(apiClient,
               appsV1Api.listNamespacedDeploymentCall(
                   deploymentResource.getNamespace(), null, true, null, null, null, null, null, null, 300, true, null),
               new TypeToken<Watch.Response<V1Deployment>>() {}.getType())) {
        for (Watch.Response<V1Deployment> event : watch) {
          V1Deployment deployment = event.object;
          V1ObjectMeta meta = deployment.getMetadata();
          if (meta != null && !deploymentResource.getName().equals(meta.getName())) {
            continue;
          }
          switch (event.type) {
            case "ADDED":
            case "MODIFIED":
              K8ApiResponseDTO rolloutStatus = statusViewer.extractRolloutStatus(deployment);
              executionLogCallback.saveExecutionLog(rolloutStatus.getMessage());
              if (rolloutStatus.isFailed()) {
                if (errorFrameworkEnabled) {
                  throw new KubernetesTaskException(rolloutStatus.getMessage());
                }
                return false;
              }
              if (rolloutStatus.isDone()) {
                return true;
              }
              break;
            case "DELETED":
              throw new InvalidRequestException("object has been deleted");
            default:
              throw new InvalidRequestException(String.format("unexpected k8s event %s", event.type));
          }
        }
      } catch (IOException e) {
        throw new InvalidRequestException("Failed to close Kubernetes watch.", e);
      } catch (ApiException e) {
        ApiException ex = ExceptionMessageSanitizer.sanitizeException(e);
        log.error("Failed to watch deployment rollout status.", ex);
        executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(ex), LogLevel.ERROR);
        if (errorFrameworkEnabled) {
          throw ex;
        }
        return false;
      }
    }
  }
}
