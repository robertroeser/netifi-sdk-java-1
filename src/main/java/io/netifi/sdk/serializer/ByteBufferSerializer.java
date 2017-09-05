package io.netifi.sdk.serializer;

import java.nio.ByteBuffer;

/** */
public class ByteBufferSerializer implements Serializer<ByteBuffer> {
  public ByteBufferSerializer(Class<?> clazz) {}

  @Override
  public Class<ByteBuffer> getType() {
    return ByteBuffer.class;
  }

  @Override
  public ByteBuffer deserialize(ByteBuffer buffer) {
    return buffer;
  }

  @Override
  public ByteBuffer serialize(Object t) {
    return (ByteBuffer) t;
  }
}
