package io.netifi.sdk.serialization;

import java.nio.ByteBuffer;

/**
 * Raw Payload that a connection sends over the wire
 */
public interface RawPayload {
    ByteBuffer getMetadata();
    
    ByteBuffer getData();
}
