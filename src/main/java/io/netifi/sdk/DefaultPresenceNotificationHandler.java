package io.netifi.sdk;

import reactor.core.publisher.Flux;

import java.util.Collection;

/** Implementation of {@link PresenceNotificationHandler} */
public class DefaultPresenceNotificationHandler implements PresenceNotificationHandler {
  /*private static final Logger logger = LoggerFactory.getLogger(Netifi.class);

    private final BooleanSupplier running;

    private final TimebasedIdGenerator idGenerator;

    private final long accountId;

    private final String fromDestination;

    public DefaultPresenceNotificationHandler(
        BooleanSupplier running,
        TimebasedIdGenerator idGenerator,
        long accountId,
        String fromDestination) {
      this.running = running;
      this.idGenerator = idGenerator;
      this.accountId = accountId;
      this.fromDestination = fromDestination;
    }
  */
  @Override
  public Flux<Collection<String>> presence(long accountId, String group) {
    return null;
  }

  @Override
  public Flux<Collection<String>> presence(long accountId, String group, String destination) {
    return null;
  }
}
