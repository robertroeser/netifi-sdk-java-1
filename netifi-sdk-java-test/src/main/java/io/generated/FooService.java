package io.generated;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** */
public interface FooService {
  Mono<Person> someRequestResponse(Addressbook o);
  
  Mono<Person> someRequestResponse2(Person o);

  Mono<Void> firing(Addressbook o);

  Flux<Object> streaming(Object o);
}
