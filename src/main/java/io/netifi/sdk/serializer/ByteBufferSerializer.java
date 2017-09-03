package io.netifi.sdk.serializer;

import io.netifi.sdk.annotations.FIRE_FORGET;

import java.nio.ByteBuffer;

/** */
public class ByteBufferSerializer implements Serializer<ByteBuffer> {
  private static final ByteBufferSerializer SERIALIZER = new ByteBufferSerializer();

  private ByteBufferSerializer() {}

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
