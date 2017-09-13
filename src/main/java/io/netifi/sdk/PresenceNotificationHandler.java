package io.netifi.sdk;

import io.reactivex.Flowable;

import java.util.Collection;

public interface PresenceNotificationHandler {

  Flowable<Collection<String>> presence(long accountId, String group);

  Flowable<Collection<String>> presence(long accountId, String group, String destination);
}
