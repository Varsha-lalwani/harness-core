package io.harness.util;

import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
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
      Request request = call.request();
      log.info("Request body: {}", request.body());
      log.info("Request headers: {}", request.headers());
      log.info("Request method: {}", request.method());
      log.info("Request url: {}", request.url());
      log.info("Request toString: {}", request.toString());
      response = call.execute();
      log.info("Response isSuccessful: {}", response.isSuccessful());
      log.info("Response code: {}", response.code());
      log.info("Response raw: {}", response.raw());
      log.info("Response errorBody: {}", response.errorBody());
      log.info("Response headers: {}", response.headers());
      log.info("Response message: {}", response.message());
      log.info("Response body: {}", response.body());
      log.info("Response toString: {}", response.toString());
      log.info("Response raw.body: {}", response.raw().body());
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
