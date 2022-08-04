package io.harness.util;

import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static io.harness.annotations.dev.HarnessTeam.CE;

@OwnedBy(CE)
@Slf4j
public class GrpcRestUtils {
  public static <T> T executeRestCall(Call<T> call) throws IOException {
    Response<T> response = null;
    try {
      response = call.execute();
      if (response.isSuccessful()) {
        return response.body();
      }
    } catch (Exception e) {
      log.error("error executing rest call", e);
      throw e;
    } finally {
      if (response != null && !response.isSuccessful()) {
        String errorResponse = response.errorBody().string();
        final int errorCode = response.code();
        log.warn("Call received {} Error Response: {}", errorCode, errorResponse);
        response.errorBody().close();
      }
    }
    return null;
  }
}
