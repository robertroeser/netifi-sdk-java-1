package io.netifi.sdk.connection;

import io.netifi.sdk.serialization.RawPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** */
public interface Handler {
  /**
   * Fire and Forget interaction model of {@code RSocket}.
   *
   * @param payload Request payload.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   *     handled, otherwise errors.
   */
  Mono<Void> fireAndForget(RawPayload payload);

  /**
   * Request-Response interaction model of {@code RSocket}.
   *
   * @param payload Request payload.
   * @return {@code Publisher} containing at most a single {@code Payload} representing the
   *     response.
   */
  Mono<RawPayload> requestResponse(RawPayload payload);

  /**
   * Request-Stream interaction model of {@code RSocket}.
   *
   * @param payload Request payload.
   * @return {@code Publisher} containing the stream of {@code Payload}s representing the response.
   */
  Flux<RawPayload> requestStream(RawPayload payload);

  /**
   * Request-Channel interaction model of {@code RSocket}.
   *
   * @param payloads Stream of request payloads.
   * @return Stream of response payloads.
   */
  Flux<RawPayload> requestChannel(Publisher<RawPayload> payloads);

  /**
   * Metadata-Push interaction model of {@code RSocket}.
   *
   * @param payload Request payloads.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   *     handled, otherwise errors.
   */
  Mono<Void> metadataPush(RawPayload payload);
}
