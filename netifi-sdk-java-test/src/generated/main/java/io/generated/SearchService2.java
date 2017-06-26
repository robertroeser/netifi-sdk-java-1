package io.generated;

import reactor.core.publisher.Mono;

/** */
public interface SearchService2 {
    Mono<Addressbook> search(Person person);
    Mono<Person> search2(Addressbook person);
}
