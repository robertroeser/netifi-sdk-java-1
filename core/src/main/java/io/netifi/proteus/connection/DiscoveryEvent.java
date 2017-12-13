package io.netifi.proteus.connection;

import java.net.SocketAddress;

/** DiscoveryEvent encapsulating when a address is added or removed */
public enum DiscoveryEvent {
  Add,
  Remove;

  private String id;
  private SocketAddress socketAddress;

  public static DiscoveryEvent add(String id, SocketAddress socketAddress) {
    DiscoveryEvent add = DiscoveryEvent.Add;
    add.setAddress(socketAddress);
    add.setId(id);
    return add;
  }

  public static DiscoveryEvent add(SocketAddress socketAddress) {
    DiscoveryEvent add = DiscoveryEvent.Add;
    add.setAddress(socketAddress);
    add.setId(socketAddress.toString());
    return add;
  }

  public static DiscoveryEvent remove(String id, SocketAddress socketAddress) {
    DiscoveryEvent remove = DiscoveryEvent.Remove;
    remove.setAddress(socketAddress);
    remove.setId(id);
    return remove;
  }

  public static DiscoveryEvent remove(SocketAddress socketAddress) {
    DiscoveryEvent remove = DiscoveryEvent.Remove;
    remove.setAddress(socketAddress);
    remove.setId(socketAddress.toString());
    return remove;
  }

  public SocketAddress getAddress() {
    return socketAddress;
  }

  void setAddress(SocketAddress socketAddress) {
    this.socketAddress = socketAddress;
  }

  public String getId() {
    return id;
  }

  void setId(String id) {
    this.id = id;
  }
}
