package io.generated;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netifi.edge.router.Route;
import io.netifi.sdk.connection.Handler;
import io.netifi.sdk.serialization.RawPayload;
import io.netifi.sdk.service.Namespace;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.nio.ByteBuffer;

/** */
@Namespace(id = 1613422765472679988L)
public final class namespace implements Handler {
  private volatile SearchService searchService;
  private volatile SearchService2 searchService2;

  private namespace() {}

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  public void setSearchService2(SearchService2 searchService2) {
    this.searchService2 = searchService2;
  }

  @Override
  public Mono<Void> fireAndForget(RawPayload payload) {
    return Mono.defer(
        () -> {
          ByteBuffer data = payload.getData();
          ByteBuffer metadata = payload.getMetadata();
          Route route = Route.getRootAsRoute(metadata);
          long classId = route.destClass();
          long methodId = route.destMethod();
          if (-609141169116730076L == classId && null != searchService && null != searchService) {
            if (-8173743921145919388L == methodId) {
              try {
                Person person = Person.parseFrom(data);
                return searchService.ff(person).then();
              } catch (InvalidProtocolBufferException e) {
                return Mono.error(e);
              }
            } else {
              return Mono.error(
                  new IllegalStateException("no service found for method with id " + classId));
            }
          } else {
            return Mono.error(
                new IllegalStateException("no service found for class with id " + classId));
          }
        });
  }

  @Override
  public Mono<RawPayload> requestResponse(RawPayload payload) {
    return Mono.defer(
        () -> {
          ByteBuffer data = payload.getData();
          ByteBuffer metadata = payload.getMetadata();
          Route route = Route.getRootAsRoute(metadata);
          long classId = route.destClass();
          long methodId = route.destMethod();

          if (-609141169116730076L == classId && null != searchService) {
            if (5302125728398668880L == methodId) {
              try {
                Person person = Person.parseFrom(data);
                return searchService.rr(person).map(result -> resultToRawPayload(result, metadata));
              } catch (InvalidProtocolBufferException e) {
                return Mono.error(e);
              }
            } else {
              return Mono.error(
                  new IllegalStateException("no service found for method with id " + classId));
            }
          } else if (-8007573123134514388L == classId && null != searchService2) {
            if (1613671883585443942L == methodId) {
              try {
                Person person = Person.parseFrom(data);
                return searchService2
                    .search(person)
                    .map(result -> resultToRawPayload(result, metadata));
              } catch (InvalidProtocolBufferException e) {
                return Mono.error(e);
              }
            } else if (-1799178911675546103L == methodId) {
              try {
                Person person = Person.parseFrom(data);
                return searchService.rr(person).map(result -> resultToRawPayload(result, metadata));
              } catch (InvalidProtocolBufferException e) {
                return Mono.error(e);
              }
            } else {
              return Mono.error(
                  new IllegalStateException("no service found for method with id " + classId));
            }
          } else {
            return Mono.error(
                new IllegalStateException("no service found for class with id " + classId));
          }
        });
  }

  @Override
  public Flux<RawPayload> requestStream(RawPayload payload) {
    return Flux.defer(
        () -> {
          ByteBuffer data = payload.getData();
          ByteBuffer metadata = payload.getMetadata();
          Route route = Route.getRootAsRoute(metadata);
          long classId = route.destClass();
          long methodId = route.destMethod();

          if (-609141169116730076L == classId && null != searchService) {
            if (4604048618267360067L == methodId) {
              try {
                Person person = Person.parseFrom(data);
                return searchService
                    .stream(person)
                    .map(result -> resultToRawPayload(result, metadata));
              } catch (InvalidProtocolBufferException e) {
                return Mono.error(e);
              }
            } else {
              return Mono.error(
                  new IllegalStateException("no service found for method with id " + classId));
            }
          } else {
            return Mono.error(
                new IllegalStateException("no service found for class with id " + classId));
          }
        });
  }

  @Override
  public Flux<RawPayload> requestChannel(Publisher<RawPayload> payloads) {
    UnicastProcessor<RawPayload> response = UnicastProcessor.create();

    payloads.subscribe(
        new Subscriber<RawPayload>() {
          volatile Subscription s;
          UnicastProcessor<RawPayload> payloads = UnicastProcessor.create();
          boolean first = true;

          @Override
          public void onSubscribe(Subscription s) {
            this.s = s;
          }

          @Override
          public void onNext(RawPayload payload) {
            try {
              boolean f;
              synchronized (this) {
                f = first;
                if (first) {
                  first = false;
                }
              }

              if (f) {
                ByteBuffer metadata = payload.getMetadata();
                Route route = Route.getRootAsRoute(metadata);
                long classId = route.destClass();
                long methodId = route.destMethod();

                if (-609141169116730076L == classId && null != searchService) {
                  if (1015069108730327219L == methodId) {
                    Flux<Person> map =
                        payloads.map(
                            p -> {
                              try {
                                Person person = Person.parseFrom(p.getData());
                                return person;
                              } catch (Exception e) {
                                throw new RuntimeException(e);
                              }
                            });

                    searchService
                        .channel(map)
                        .map(person -> resultToRawPayload(person, metadata))
                        .doOnRequest(s::request)
                        .doOnCancel(s::cancel)
                        .subscribe(response);
                  } else if (7666376402739013864L == methodId) {
                    Flux<Addressbook> map =
                        payloads.map(
                            p -> {
                              try {
                                Addressbook addressbook = Addressbook.parseFrom(p.getData());
                                return addressbook;
                              } catch (Exception e) {
                                throw new RuntimeException(e);
                              }
                            });

                    searchService
                        .channel2(map)
                        .map(person -> resultToRawPayload(person, metadata))
                        .doOnRequest(s::request)
                        .doOnCancel(s::cancel)
                        .subscribe(response);
                  } else {
                    onError(
                        new IllegalStateException(
                            "no service found for method with id " + classId));
                  }
                } else {
                  onError(
                      new IllegalStateException("no service found for class with id " + classId));
                }
              }

              payloads.onNext(payload);

            } catch (Throwable t) {
              onError(t);
            }
          }

          @Override
          public void onError(Throwable t) {
            response.onError(t);
          }

          @Override
          public void onComplete() {
            response.onComplete();
          }
        });

    return response;
  }

  @Override
  public Mono<Void> metadataPush(RawPayload payload) {
    return Mono.error(new UnsupportedOperationException("metadataPush now supported"));
  }

  private RawPayload resultToRawPayload(GeneratedMessageV3 message, ByteBuffer metadata) {
    return new RawPayload() {
      @Override
      public ByteBuffer getMetadata() {
        return metadata;
      }

      @Override
      public ByteBuffer getData() {
        return message.toByteString().asReadOnlyByteBuffer();
      }
    };
  }
}
