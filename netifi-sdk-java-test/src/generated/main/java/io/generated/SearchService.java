package io.generated;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** */
public interface SearchService {
  Mono<Person> rr(Person person);

  Mono<Void> ff(Person person);

  Flux<Person> stream(Person person);

  Flux<Person> channel(Publisher<Person> person);
  
  Flux<Person> channel2(Publisher<Addressbook> addressbook);
}
