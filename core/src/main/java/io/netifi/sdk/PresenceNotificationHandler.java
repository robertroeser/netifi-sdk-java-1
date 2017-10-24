package io.netifi.sdk;

import java.util.Collection;
import reactor.core.publisher.Flux;

public interface PresenceNotificationHandler {

  Flux<Collection<String>> presence(long accountId, String group);

  Flux<Collection<String>> presence(long accountId, String group, String destination);
}
