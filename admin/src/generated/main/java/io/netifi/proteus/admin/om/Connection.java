// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: admin.proto

package io.netifi.proteus.admin.om;

/**
 * Protobuf type {@code io.netifi.proteus.admin.om.Connection}
 */
public  final class Connection extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.netifi.proteus.admin.om.Connection)
    ConnectionOrBuilder {
private static final long serialVersionUID = 0L;
  // Use Connection.newBuilder() to construct.
  private Connection(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Connection() {
    source_ = "";
    target_ = "";
    notices_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private Connection(
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
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            source_ = s;
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();

            target_ = s;
            break;
          }
          case 26: {
            io.netifi.proteus.admin.om.Metrics.Builder subBuilder = null;
            if (metrics_ != null) {
              subBuilder = metrics_.toBuilder();
            }
            metrics_ = input.readMessage(io.netifi.proteus.admin.om.Metrics.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(metrics_);
              metrics_ = subBuilder.buildPartial();
            }

            break;
          }
          case 34: {
            if (!((mutable_bitField0_ & 0x00000008) == 0x00000008)) {
              notices_ = new java.util.ArrayList<io.netifi.proteus.admin.om.Notice>();
              mutable_bitField0_ |= 0x00000008;
            }
            notices_.add(
                input.readMessage(io.netifi.proteus.admin.om.Notice.parser(), extensionRegistry));
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
      if (((mutable_bitField0_ & 0x00000008) == 0x00000008)) {
        notices_ = java.util.Collections.unmodifiableList(notices_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Connection_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Connection_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.netifi.proteus.admin.om.Connection.class, io.netifi.proteus.admin.om.Connection.Builder.class);
  }

  private int bitField0_;
  public static final int SOURCE_FIELD_NUMBER = 1;
  private volatile java.lang.Object source_;
  /**
   * <code>string source = 1;</code>
   */
  public java.lang.String getSource() {
    java.lang.Object ref = source_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      source_ = s;
      return s;
    }
  }
  /**
   * <code>string source = 1;</code>
   */
  public com.google.protobuf.ByteString
      getSourceBytes() {
    java.lang.Object ref = source_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      source_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int TARGET_FIELD_NUMBER = 2;
  private volatile java.lang.Object target_;
  /**
   * <code>string target = 2;</code>
   */
  public java.lang.String getTarget() {
    java.lang.Object ref = target_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      target_ = s;
      return s;
    }
  }
  /**
   * <code>string target = 2;</code>
   */
  public com.google.protobuf.ByteString
      getTargetBytes() {
    java.lang.Object ref = target_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      target_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int METRICS_FIELD_NUMBER = 3;
  private io.netifi.proteus.admin.om.Metrics metrics_;
  /**
   * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
   */
  public boolean hasMetrics() {
    return metrics_ != null;
  }
  /**
   * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
   */
  public io.netifi.proteus.admin.om.Metrics getMetrics() {
    return metrics_ == null ? io.netifi.proteus.admin.om.Metrics.getDefaultInstance() : metrics_;
  }
  /**
   * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
   */
  public io.netifi.proteus.admin.om.MetricsOrBuilder getMetricsOrBuilder() {
    return getMetrics();
  }

  public static final int NOTICES_FIELD_NUMBER = 4;
  private java.util.List<io.netifi.proteus.admin.om.Notice> notices_;
  /**
   * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
   */
  public java.util.List<io.netifi.proteus.admin.om.Notice> getNoticesList() {
    return notices_;
  }
  /**
   * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
   */
  public java.util.List<? extends io.netifi.proteus.admin.om.NoticeOrBuilder> 
      getNoticesOrBuilderList() {
    return notices_;
  }
  /**
   * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
   */
  public int getNoticesCount() {
    return notices_.size();
  }
  /**
   * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
   */
  public io.netifi.proteus.admin.om.Notice getNotices(int index) {
    return notices_.get(index);
  }
  /**
   * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
   */
  public io.netifi.proteus.admin.om.NoticeOrBuilder getNoticesOrBuilder(
      int index) {
    return notices_.get(index);
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
    if (!getSourceBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, source_);
    }
    if (!getTargetBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, target_);
    }
    if (metrics_ != null) {
      output.writeMessage(3, getMetrics());
    }
    for (int i = 0; i < notices_.size(); i++) {
      output.writeMessage(4, notices_.get(i));
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getSourceBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, source_);
    }
    if (!getTargetBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, target_);
    }
    if (metrics_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(3, getMetrics());
    }
    for (int i = 0; i < notices_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(4, notices_.get(i));
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
    if (!(obj instanceof io.netifi.proteus.admin.om.Connection)) {
      return super.equals(obj);
    }
    io.netifi.proteus.admin.om.Connection other = (io.netifi.proteus.admin.om.Connection) obj;

    boolean result = true;
    result = result && getSource()
        .equals(other.getSource());
    result = result && getTarget()
        .equals(other.getTarget());
    result = result && (hasMetrics() == other.hasMetrics());
    if (hasMetrics()) {
      result = result && getMetrics()
          .equals(other.getMetrics());
    }
    result = result && getNoticesList()
        .equals(other.getNoticesList());
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
    hash = (37 * hash) + SOURCE_FIELD_NUMBER;
    hash = (53 * hash) + getSource().hashCode();
    hash = (37 * hash) + TARGET_FIELD_NUMBER;
    hash = (53 * hash) + getTarget().hashCode();
    if (hasMetrics()) {
      hash = (37 * hash) + METRICS_FIELD_NUMBER;
      hash = (53 * hash) + getMetrics().hashCode();
    }
    if (getNoticesCount() > 0) {
      hash = (37 * hash) + NOTICES_FIELD_NUMBER;
      hash = (53 * hash) + getNoticesList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.netifi.proteus.admin.om.Connection parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Connection parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static io.netifi.proteus.admin.om.Connection parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.netifi.proteus.admin.om.Connection parseFrom(
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
  public static Builder newBuilder(io.netifi.proteus.admin.om.Connection prototype) {
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
   * Protobuf type {@code io.netifi.proteus.admin.om.Connection}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.netifi.proteus.admin.om.Connection)
      io.netifi.proteus.admin.om.ConnectionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Connection_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Connection_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.netifi.proteus.admin.om.Connection.class, io.netifi.proteus.admin.om.Connection.Builder.class);
    }

    // Construct using io.netifi.proteus.admin.om.Connection.newBuilder()
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
        getNoticesFieldBuilder();
      }
    }
    public Builder clear() {
      super.clear();
      source_ = "";

      target_ = "";

      if (metricsBuilder_ == null) {
        metrics_ = null;
      } else {
        metrics_ = null;
        metricsBuilder_ = null;
      }
      if (noticesBuilder_ == null) {
        notices_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000008);
      } else {
        noticesBuilder_.clear();
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return io.netifi.proteus.admin.om.ProteusAdmin.internal_static_io_netifi_proteus_admin_om_Connection_descriptor;
    }

    public io.netifi.proteus.admin.om.Connection getDefaultInstanceForType() {
      return io.netifi.proteus.admin.om.Connection.getDefaultInstance();
    }

    public io.netifi.proteus.admin.om.Connection build() {
      io.netifi.proteus.admin.om.Connection result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public io.netifi.proteus.admin.om.Connection buildPartial() {
      io.netifi.proteus.admin.om.Connection result = new io.netifi.proteus.admin.om.Connection(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      result.source_ = source_;
      result.target_ = target_;
      if (metricsBuilder_ == null) {
        result.metrics_ = metrics_;
      } else {
        result.metrics_ = metricsBuilder_.build();
      }
      if (noticesBuilder_ == null) {
        if (((bitField0_ & 0x00000008) == 0x00000008)) {
          notices_ = java.util.Collections.unmodifiableList(notices_);
          bitField0_ = (bitField0_ & ~0x00000008);
        }
        result.notices_ = notices_;
      } else {
        result.notices_ = noticesBuilder_.build();
      }
      result.bitField0_ = to_bitField0_;
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
      if (other instanceof io.netifi.proteus.admin.om.Connection) {
        return mergeFrom((io.netifi.proteus.admin.om.Connection)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.netifi.proteus.admin.om.Connection other) {
      if (other == io.netifi.proteus.admin.om.Connection.getDefaultInstance()) return this;
      if (!other.getSource().isEmpty()) {
        source_ = other.source_;
        onChanged();
      }
      if (!other.getTarget().isEmpty()) {
        target_ = other.target_;
        onChanged();
      }
      if (other.hasMetrics()) {
        mergeMetrics(other.getMetrics());
      }
      if (noticesBuilder_ == null) {
        if (!other.notices_.isEmpty()) {
          if (notices_.isEmpty()) {
            notices_ = other.notices_;
            bitField0_ = (bitField0_ & ~0x00000008);
          } else {
            ensureNoticesIsMutable();
            notices_.addAll(other.notices_);
          }
          onChanged();
        }
      } else {
        if (!other.notices_.isEmpty()) {
          if (noticesBuilder_.isEmpty()) {
            noticesBuilder_.dispose();
            noticesBuilder_ = null;
            notices_ = other.notices_;
            bitField0_ = (bitField0_ & ~0x00000008);
            noticesBuilder_ = 
              com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ?
                 getNoticesFieldBuilder() : null;
          } else {
            noticesBuilder_.addAllMessages(other.notices_);
          }
        }
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
      io.netifi.proteus.admin.om.Connection parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.netifi.proteus.admin.om.Connection) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.lang.Object source_ = "";
    /**
     * <code>string source = 1;</code>
     */
    public java.lang.String getSource() {
      java.lang.Object ref = source_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        source_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string source = 1;</code>
     */
    public com.google.protobuf.ByteString
        getSourceBytes() {
      java.lang.Object ref = source_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        source_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string source = 1;</code>
     */
    public Builder setSource(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      source_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string source = 1;</code>
     */
    public Builder clearSource() {
      
      source_ = getDefaultInstance().getSource();
      onChanged();
      return this;
    }
    /**
     * <code>string source = 1;</code>
     */
    public Builder setSourceBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      source_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object target_ = "";
    /**
     * <code>string target = 2;</code>
     */
    public java.lang.String getTarget() {
      java.lang.Object ref = target_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        target_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string target = 2;</code>
     */
    public com.google.protobuf.ByteString
        getTargetBytes() {
      java.lang.Object ref = target_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        target_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string target = 2;</code>
     */
    public Builder setTarget(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      target_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string target = 2;</code>
     */
    public Builder clearTarget() {
      
      target_ = getDefaultInstance().getTarget();
      onChanged();
      return this;
    }
    /**
     * <code>string target = 2;</code>
     */
    public Builder setTargetBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      target_ = value;
      onChanged();
      return this;
    }

    private io.netifi.proteus.admin.om.Metrics metrics_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<
        io.netifi.proteus.admin.om.Metrics, io.netifi.proteus.admin.om.Metrics.Builder, io.netifi.proteus.admin.om.MetricsOrBuilder> metricsBuilder_;
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public boolean hasMetrics() {
      return metricsBuilder_ != null || metrics_ != null;
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public io.netifi.proteus.admin.om.Metrics getMetrics() {
      if (metricsBuilder_ == null) {
        return metrics_ == null ? io.netifi.proteus.admin.om.Metrics.getDefaultInstance() : metrics_;
      } else {
        return metricsBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public Builder setMetrics(io.netifi.proteus.admin.om.Metrics value) {
      if (metricsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        metrics_ = value;
        onChanged();
      } else {
        metricsBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public Builder setMetrics(
        io.netifi.proteus.admin.om.Metrics.Builder builderForValue) {
      if (metricsBuilder_ == null) {
        metrics_ = builderForValue.build();
        onChanged();
      } else {
        metricsBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public Builder mergeMetrics(io.netifi.proteus.admin.om.Metrics value) {
      if (metricsBuilder_ == null) {
        if (metrics_ != null) {
          metrics_ =
            io.netifi.proteus.admin.om.Metrics.newBuilder(metrics_).mergeFrom(value).buildPartial();
        } else {
          metrics_ = value;
        }
        onChanged();
      } else {
        metricsBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public Builder clearMetrics() {
      if (metricsBuilder_ == null) {
        metrics_ = null;
        onChanged();
      } else {
        metrics_ = null;
        metricsBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public io.netifi.proteus.admin.om.Metrics.Builder getMetricsBuilder() {
      
      onChanged();
      return getMetricsFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    public io.netifi.proteus.admin.om.MetricsOrBuilder getMetricsOrBuilder() {
      if (metricsBuilder_ != null) {
        return metricsBuilder_.getMessageOrBuilder();
      } else {
        return metrics_ == null ?
            io.netifi.proteus.admin.om.Metrics.getDefaultInstance() : metrics_;
      }
    }
    /**
     * <code>.io.netifi.proteus.admin.om.Metrics metrics = 3;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        io.netifi.proteus.admin.om.Metrics, io.netifi.proteus.admin.om.Metrics.Builder, io.netifi.proteus.admin.om.MetricsOrBuilder> 
        getMetricsFieldBuilder() {
      if (metricsBuilder_ == null) {
        metricsBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            io.netifi.proteus.admin.om.Metrics, io.netifi.proteus.admin.om.Metrics.Builder, io.netifi.proteus.admin.om.MetricsOrBuilder>(
                getMetrics(),
                getParentForChildren(),
                isClean());
        metrics_ = null;
      }
      return metricsBuilder_;
    }

    private java.util.List<io.netifi.proteus.admin.om.Notice> notices_ =
      java.util.Collections.emptyList();
    private void ensureNoticesIsMutable() {
      if (!((bitField0_ & 0x00000008) == 0x00000008)) {
        notices_ = new java.util.ArrayList<io.netifi.proteus.admin.om.Notice>(notices_);
        bitField0_ |= 0x00000008;
       }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
        io.netifi.proteus.admin.om.Notice, io.netifi.proteus.admin.om.Notice.Builder, io.netifi.proteus.admin.om.NoticeOrBuilder> noticesBuilder_;

    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public java.util.List<io.netifi.proteus.admin.om.Notice> getNoticesList() {
      if (noticesBuilder_ == null) {
        return java.util.Collections.unmodifiableList(notices_);
      } else {
        return noticesBuilder_.getMessageList();
      }
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public int getNoticesCount() {
      if (noticesBuilder_ == null) {
        return notices_.size();
      } else {
        return noticesBuilder_.getCount();
      }
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public io.netifi.proteus.admin.om.Notice getNotices(int index) {
      if (noticesBuilder_ == null) {
        return notices_.get(index);
      } else {
        return noticesBuilder_.getMessage(index);
      }
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder setNotices(
        int index, io.netifi.proteus.admin.om.Notice value) {
      if (noticesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNoticesIsMutable();
        notices_.set(index, value);
        onChanged();
      } else {
        noticesBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder setNotices(
        int index, io.netifi.proteus.admin.om.Notice.Builder builderForValue) {
      if (noticesBuilder_ == null) {
        ensureNoticesIsMutable();
        notices_.set(index, builderForValue.build());
        onChanged();
      } else {
        noticesBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder addNotices(io.netifi.proteus.admin.om.Notice value) {
      if (noticesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNoticesIsMutable();
        notices_.add(value);
        onChanged();
      } else {
        noticesBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder addNotices(
        int index, io.netifi.proteus.admin.om.Notice value) {
      if (noticesBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureNoticesIsMutable();
        notices_.add(index, value);
        onChanged();
      } else {
        noticesBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder addNotices(
        io.netifi.proteus.admin.om.Notice.Builder builderForValue) {
      if (noticesBuilder_ == null) {
        ensureNoticesIsMutable();
        notices_.add(builderForValue.build());
        onChanged();
      } else {
        noticesBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder addNotices(
        int index, io.netifi.proteus.admin.om.Notice.Builder builderForValue) {
      if (noticesBuilder_ == null) {
        ensureNoticesIsMutable();
        notices_.add(index, builderForValue.build());
        onChanged();
      } else {
        noticesBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder addAllNotices(
        java.lang.Iterable<? extends io.netifi.proteus.admin.om.Notice> values) {
      if (noticesBuilder_ == null) {
        ensureNoticesIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, notices_);
        onChanged();
      } else {
        noticesBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder clearNotices() {
      if (noticesBuilder_ == null) {
        notices_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000008);
        onChanged();
      } else {
        noticesBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public Builder removeNotices(int index) {
      if (noticesBuilder_ == null) {
        ensureNoticesIsMutable();
        notices_.remove(index);
        onChanged();
      } else {
        noticesBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public io.netifi.proteus.admin.om.Notice.Builder getNoticesBuilder(
        int index) {
      return getNoticesFieldBuilder().getBuilder(index);
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public io.netifi.proteus.admin.om.NoticeOrBuilder getNoticesOrBuilder(
        int index) {
      if (noticesBuilder_ == null) {
        return notices_.get(index);  } else {
        return noticesBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public java.util.List<? extends io.netifi.proteus.admin.om.NoticeOrBuilder> 
         getNoticesOrBuilderList() {
      if (noticesBuilder_ != null) {
        return noticesBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(notices_);
      }
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public io.netifi.proteus.admin.om.Notice.Builder addNoticesBuilder() {
      return getNoticesFieldBuilder().addBuilder(
          io.netifi.proteus.admin.om.Notice.getDefaultInstance());
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public io.netifi.proteus.admin.om.Notice.Builder addNoticesBuilder(
        int index) {
      return getNoticesFieldBuilder().addBuilder(
          index, io.netifi.proteus.admin.om.Notice.getDefaultInstance());
    }
    /**
     * <code>repeated .io.netifi.proteus.admin.om.Notice notices = 4;</code>
     */
    public java.util.List<io.netifi.proteus.admin.om.Notice.Builder> 
         getNoticesBuilderList() {
      return getNoticesFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilderV3<
        io.netifi.proteus.admin.om.Notice, io.netifi.proteus.admin.om.Notice.Builder, io.netifi.proteus.admin.om.NoticeOrBuilder> 
        getNoticesFieldBuilder() {
      if (noticesBuilder_ == null) {
        noticesBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<
            io.netifi.proteus.admin.om.Notice, io.netifi.proteus.admin.om.Notice.Builder, io.netifi.proteus.admin.om.NoticeOrBuilder>(
                notices_,
                ((bitField0_ & 0x00000008) == 0x00000008),
                getParentForChildren(),
                isClean());
        notices_ = null;
      }
      return noticesBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:io.netifi.proteus.admin.om.Connection)
  }

  // @@protoc_insertion_point(class_scope:io.netifi.proteus.admin.om.Connection)
  private static final io.netifi.proteus.admin.om.Connection DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.netifi.proteus.admin.om.Connection();
  }

  public static io.netifi.proteus.admin.om.Connection getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Connection>
      PARSER = new com.google.protobuf.AbstractParser<Connection>() {
    public Connection parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new Connection(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<Connection> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Connection> getParserForType() {
    return PARSER;
  }

  public io.netifi.proteus.admin.om.Connection getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

