package io.netifi.sdk.serializer;

import java.nio.ByteBuffer;

public interface Serializer<T> {
  Class<T> getType();
  
  ByteBuffer serialize(Object t);
  
  T deserialize(ByteBuffer buffer);
}

