package io.harness.eventpublisherclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;

import javax.ws.rs.Consumes;

@OwnedBy(HarnessTeam.CE)
public interface EventPublisherClient {
    @Consumes({"application/x-protobuf"})
    @GET("/event-publisher-server/publish")
    Call<PublishResponse> publish(@Body PublishRequest publishRequest);
}
