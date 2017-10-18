package io.netifi.sdk;

import reactor.core.publisher.Flux;

import java.util.Collection;

public interface PresenceNotificationHandler {

  Flux<Collection<String>> presence(long accountId, String group);

  Flux<Collection<String>> presence(long accountId, String group, String destination);
}
