package io.netifi.sdk.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import java.io.IOException;
import java.nio.ByteBuffer;

/** */
public class JSONSerializer<T> implements Serializer<T> {
  private static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.registerModule(new AfterburnerModule());
  }

  private final Class<T> clazz;

  public JSONSerializer(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public Class<T> getType() {
    return clazz;
  }

  @Override
  public T deserialize(ByteBuffer buffer) {
    ByteBufferBackedInputStream bis = new ByteBufferBackedInputStream(buffer);
    try {
      return MAPPER.readValue(bis, getType());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ByteBuffer serialize(Object t) {
    try {
      byte[] bytes = MAPPER.writeValueAsBytes(t);
      return ByteBuffer.wrap(bytes);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
