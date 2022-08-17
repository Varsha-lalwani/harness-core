package io.harness.logstreaming;

import io.harness.delegate.beans.taskprogress.ITaskProgressClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogStreamingTaskClientHelper {
  // we are storing in a hashmap instead of cache, since we don't know how long the task will take to complete

  private Map<String, List<LogLine>> logCache = new HashMap<>();

  private Map<String, LogStreamingSanitizer> logStreamingSanitizerMap = new HashMap<>();

  private Map<String, String> baseLogKeyMap = new HashMap<>();

  private Map<String, String> appIdMap = new HashMap<>();

  private Map<String, String> activityIdMap = new HashMap<>();

  private Map<String, ITaskProgressClient> taskProgressClientMap = new HashMap<>();

  public void setLogStreamingSanitizerInMap(String taskId, LogStreamingSanitizer logStreamingSanitizer) {
    logStreamingSanitizerMap.put(taskId, logStreamingSanitizer);
  }

  public void setBaseLogKeyInMap(String taskId, String baseLogKey) {
    baseLogKeyMap.put(taskId, baseLogKey);
  }

  public String getBaseLogKeyFromMap(String taskId) {
    return baseLogKeyMap.get(taskId);
  }

  public void setAppIdInMap(String taskId, String appId) {
    appIdMap.put(taskId, appId);
  }

  public void setActivityIdInMap(String taskId, String activityId) {
    activityIdMap.put(taskId, activityId);
  }

  public void setTaskProgressClientInMap(String taskId, ITaskProgressClient taskProgressClient) {
    taskProgressClientMap.put(taskId, taskProgressClient);
  }
}
