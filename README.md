# Netifi Java SDK

<a href='https://travis-ci.org/netifi/netifi-sdk-java'><img src='https://travis-ci.org/netifi/netifi-sdk-java.svg?branch=master'></a>

The Netifi SDK for JDK connects to Netifi's router using the RSocket protocol. It serves as a drop-in replacement to the RSocketFactory, and provides a transparent routing layer.

Each connection to the Netifi Router is a unique destination. Each connection belongs to a group. The destination is string up to 255 characters, and a group is string up to 255. You can either
route calls to a single destination, or you can route calls to a group. If you route calls to a group the Netifi Router will load balance automatically.

## Trivial Proteus Example
This example uses [Proteus](https://github.com/netifi/proteus-java) to send requests, but anything that uses an RSocket can be used.

1. Create a Protobuf IDL:
```
syntax = "proto3";

package io.netifi.testing;

option java_package = "io.netifi.testing.protobuf";
option java_outer_classname = "SimpleServiceProto";
option java_multiple_files = true;

// A simple service for test.
service SimpleService {
  // Simple unary RPC.
  rpc UnaryRpc (SimpleRequest) returns (SimpleResponse) {}

  // Simple client-to-server streaming RPC.
  rpc ClientStreamingRpc (stream SimpleRequest) returns (SimpleResponse) {}

  // Simple server-to-client streaming RPC.
  rpc ServerStreamingRpc (SimpleRequest) returns (stream SimpleResponse) {}

  // Simple bidirectional streaming RPC.
  rpc BidiStreamingRpc (stream SimpleRequest) returns (stream SimpleResponse) {}
}

// A simple request message type for test.
message SimpleRequest {
  // An optional string message for test.
  string requestMessage = 1;
}

// A simple response message type for test.
message SimpleResponse {
  // An optional string message for test.
  string responseMessage = 1;
}
```

2. Generate code with the [Profobuf Gradle Plugin](https://github.com/google/protobuf-gradle-plugin):
```
protobuf {
  // Configure the codegen plugins
  plugins {
    // Define the proteus plugin
    proteus {
      artifact = 'io.proteus:proteus-java'
    }
  }
}
```

3. Implement the SimpleService interface:
```
public class DefaultSimpleService implements SimpleService {
    @Override
    public Mono<SimpleResponse> unaryRpc(SimpleRequest message) {
      return Mono.fromCallable(
          () ->
              SimpleResponse.newBuilder()
                  .setResponseMessage("we got the message -> " + message.getRequestMessage())
                  .build());
    }

    @Override
    public Mono<SimpleResponse> clientStreamingRpc(Publisher<SimpleRequest> messages) {
      return Flux.from(messages)
          .take(10)
          .doOnNext(s -> System.out.println("got -> " + s.getRequestMessage()))
          .last()
          .map(
              simpleRequest ->
                  SimpleResponse.newBuilder()
                      .setResponseMessage("last one -> " + simpleRequest.getRequestMessage())
                      .build());

    }

    @Override
    public Flux<SimpleResponse> serverStreamingRpc(SimpleRequest message) {
      String requestMessage = message.getRequestMessage();
      return Flux.interval(Duration.ofMillis(200))
          .onBackpressureDrop()
          .map(i -> i + " - got message - " + requestMessage)
          .map(s -> SimpleResponse.newBuilder().setResponseMessage(s).build());
    }

    @Override
    public Flux<SimpleResponse> bidiStreamingRpc(Publisher<SimpleRequest> messages) {
      return Flux.from(messages).flatMap(this::unaryRpc);
    }

    @Override
    public double availability() {
      return 1.0;
    }

    @Override
    public Mono<Void> close() {
      return Mono.empty();
    }

    @Override
    public Mono<Void> onClose() {
      return Mono.empty();
    }
  }
```

4. Create an Netifi instance that acts a server, and give it the generate SimpleServiceServer with the implemented interface:
```
long accessKey = ...
long accountId = ...
String accessToken = ...

Netifi server =
        Netifi.builder()
            .group("test.server")
            .destination("server")
            .accountId(accountId)
            .accessKey(accessKey)
            .accessToken(accessToken)
            .addHandler(new SimpleServiceServer(new DefaultSimpleService()))
            .build();
```

5. Create the Netifi instance to act as  client. Use the client to generate a socket, and hand the NetifiSocket to the
SimpleServiceClient:
```
long accessKey = ...
long accountId = ...
String accessToken = ...

NetifiSocket netifiSocket = client.connect("test.server").block()

Netifi client =
        Netifi.builder()
            .group("test.client")
            .destination("client")
            .accountId(accountId)
            .accessKey(accessKey)
            .accessToken(accessToken)
            .build();

SimpleServiceClient client = new SimpleServiceClient(rSocket);
```

6. Call the Client:
```
SimpleResponse response =
    client
        .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("sending a message").build())
        .block();

String responseMessage = response.getResponseMessage();

System.out.println(responseMessage);

SimpleServiceClient client = new SimpleServiceClient(rSocket);
client
    .serverStreamingRpc(
        SimpleRequest.newBuilder().setRequestMessage("sending a message").build())
    .take(5)
    .toStream()
.forEach(simpleResponse -> System.out.println(simpleResponse.getResponseMessage()));

```

## Bugs and Feedback

For bugs, questions, and discussions please use the [Github Issues](https://github.com/netifi/netifi-sdk-java/issues).

## License
Copyright 2017 Netifi Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
