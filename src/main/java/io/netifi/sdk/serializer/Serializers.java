package io.netifi.sdk.serializer;

/** */
public enum Serializers {
  CBOR(CBORSerializer.class),
  BINARY(ByteBufferSerializer.class),
  JSON(JSONSerializer.class);

  private Class<? extends Serializer> serializer;

  Serializers(Class<? extends Serializer> serializer) {
    this.serializer = serializer;
  }

  public Class<? extends Serializer> getSerializer() {
    return serializer;
  }
}
