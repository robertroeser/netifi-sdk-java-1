package io.netifi.sdk.serializer;

/** */
public class Serializers {
  public static final Class<CBORSerializer> CBOR = CBORSerializer.class;
  public static final Class<ByteBufferSerializer> BINARY = ByteBufferSerializer.class;
  public static final Class<JSONSerializer> JSON = JSONSerializer.class;
}
