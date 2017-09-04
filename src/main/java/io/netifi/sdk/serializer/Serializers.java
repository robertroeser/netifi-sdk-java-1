package io.netifi.sdk.serializer;

/** */
public class Serializers {
  public static final String CBOR = CBORSerializer.class.getCanonicalName();
  public static final String BINARY = ByteBufferSerializer.class.getCanonicalName();
  public static final String JSON = JSONSerializer.class.getCanonicalName();
}
