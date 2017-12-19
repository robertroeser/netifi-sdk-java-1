// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: admin.proto

package io.netifi.proteus.admin.om;

/**
 * Protobuf type {@code io.netifi.proteus.admin.om.Metrics}
 */
public  final class Metrics extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.netifi.proteus.admin.om.Metrics)
    MetricsOrBuilder {
private static final long serialVersionUID = 0L;
  // Use Metrics.newBuilder() to construct.
  private Metrics(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Metrics() {
    normal_ = 0D;
    danger_ = 0D;
    warning_ = 0D;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private Metrics(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownFieldProto3(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 9: {

            normal_ = input.readDouble();
            break;
          }
          case 17: {

            danger_ = input.readDouble();
            break;
          }
          case 25: {

            warning_ = input.readDouble();
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Metrics_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Metrics_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.netifi.proteus.admin.om.Metrics.class, io.netifi.proteus.admin.om.Metrics.Builder.class);
  }

  public static final int NORMAL_FIELD_NUMBER = 1;
  private double normal_;
  /**
   * <code>double normal = 1;</code>
   */
  public double getNormal() {
    return normal_;
  }

  public static final int DANGER_FIELD_NUMBER = 2;
  private double danger_;
  /**
   * <code>double danger = 2;</code>
   */
  public double getDanger() {
    return danger_;
  }

  public static final int WARNING_FIELD_NUMBER = 3;
  private double warning_;
  /**
   * <code>double warning = 3;</code>
   */
  public double getWarning() {
    return warning_;
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (normal_ != 0D) {
      output.writeDouble(1, normal_);
    }
    if (danger_ != 0D) {
      output.writeDouble(2, danger_);
    }
    if (warning_ != 0D) {
      output.writeDouble(3, warning_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (normal_ != 0D) {
      size += com.google.protobuf.CodedOutputStream
        .computeDoubleSize(1, normal_);
    }
    if (danger_ != 0D) {
      size += com.google.protobuf.CodedOutputStream
        .computeDoubleSize(2, danger_);
    }
    if (warning_ != 0D) {
      size += com.google.protobuf.CodedOutputStream
        .computeDoubleSize(3, warning_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof io.netifi.proteus.admin.om.Metrics)) {
      return super.equals(obj);
    }
    io.netifi.proteus.admin.om.Metrics other = (io.netifi.proteus.admin.om.Metrics) obj;

    boolean result = true;
    result = result && (
        java.lang.Double.doubleToLongBits(getNormal())
        == java.lang.Double.doubleToLongBits(
            other.getNormal()));
    result = result && (
        java.lang.Double.doubleToLongBits(getDanger())
        == java.lang.Double.doubleToLongBits(
            other.getDanger()));
    result = result && (
        java.lang.Double.doubleToLongBits(getWarning())
        == java.lang.Double.doubleToLongBits(
            other.getWarning()));
    result = result && unknownFields.equals(other.unknownFields);
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + NORMAL_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        java.lang.Double.doubleToLongBits(getNormal()));
    hash = (37 * hash) + DANGER_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        java.lang.Double.doubleToLongBits(getDanger()));
    hash = (37 * hash) + WARNING_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        java.lang.Double.doubleToLongBits(getWarning()));
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Metrics parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static io.netifi.proteus.admin.om.Metrics parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.netifi.proteus.admin.om.Metrics parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.netifi.proteus.admin.om.Metrics prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code io.netifi.proteus.admin.om.Metrics}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.netifi.proteus.admin.om.Metrics)
      io.netifi.proteus.admin.om.MetricsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Metrics_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Metrics_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.netifi.proteus.admin.om.Metrics.class, io.netifi.proteus.admin.om.Metrics.Builder.class);
    }

    // Construct using io.netifi.proteus.admin.om.Metrics.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      normal_ = 0D;

      danger_ = 0D;

      warning_ = 0D;

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Metrics_descriptor;
    }

    public io.netifi.proteus.admin.om.Metrics getDefaultInstanceForType() {
      return io.netifi.proteus.admin.om.Metrics.getDefaultInstance();
    }

    public io.netifi.proteus.admin.om.Metrics build() {
      io.netifi.proteus.admin.om.Metrics result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public io.netifi.proteus.admin.om.Metrics buildPartial() {
      io.netifi.proteus.admin.om.Metrics result = new io.netifi.proteus.admin.om.Metrics(this);
      result.normal_ = normal_;
      result.danger_ = danger_;
      result.warning_ = warning_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.netifi.proteus.admin.om.Metrics) {
        return mergeFrom((io.netifi.proteus.admin.om.Metrics)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.netifi.proteus.admin.om.Metrics other) {
      if (other == io.netifi.proteus.admin.om.Metrics.getDefaultInstance()) return this;
      if (other.getNormal() != 0D) {
        setNormal(other.getNormal());
      }
      if (other.getDanger() != 0D) {
        setDanger(other.getDanger());
      }
      if (other.getWarning() != 0D) {
        setWarning(other.getWarning());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      io.netifi.proteus.admin.om.Metrics parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.netifi.proteus.admin.om.Metrics) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private double normal_ ;
    /**
     * <code>double normal = 1;</code>
     */
    public double getNormal() {
      return normal_;
    }
    /**
     * <code>double normal = 1;</code>
     */
    public Builder setNormal(double value) {
      
      normal_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>double normal = 1;</code>
     */
    public Builder clearNormal() {
      
      normal_ = 0D;
      onChanged();
      return this;
    }

    private double danger_ ;
    /**
     * <code>double danger = 2;</code>
     */
    public double getDanger() {
      return danger_;
    }
    /**
     * <code>double danger = 2;</code>
     */
    public Builder setDanger(double value) {
      
      danger_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>double danger = 2;</code>
     */
    public Builder clearDanger() {
      
      danger_ = 0D;
      onChanged();
      return this;
    }

    private double warning_ ;
    /**
     * <code>double warning = 3;</code>
     */
    public double getWarning() {
      return warning_;
    }
    /**
     * <code>double warning = 3;</code>
     */
    public Builder setWarning(double value) {
      
      warning_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>double warning = 3;</code>
     */
    public Builder clearWarning() {
      
      warning_ = 0D;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:io.netifi.proteus.admin.om.Metrics)
  }

  // @@protoc_insertion_point(class_scope:io.netifi.proteus.admin.om.Metrics)
  private static final io.netifi.proteus.admin.om.Metrics DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.netifi.proteus.admin.om.Metrics();
  }

  public static io.netifi.proteus.admin.om.Metrics getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Metrics>
      PARSER = new com.google.protobuf.AbstractParser<Metrics>() {
    public Metrics parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new Metrics(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<Metrics> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Metrics> getParserForType() {
    return PARSER;
  }

  public io.netifi.proteus.admin.om.Metrics getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
