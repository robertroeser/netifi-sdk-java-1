package io.netifi.nrqp.frames;

/**
 *
 */
public enum RouteType {
    UNDEFINED(0x00, false),
    STREAM_ID_ROUTE(0x01, true),
    STREAM_GROUP_ROUTE(0x02, false),
    CHANNEL_ID_ROUTE(0x03, true),
    CHANNEL_ACKED_ID_ROUTE(0x04, true),
    CHANNEL_BEST_EFFORT_ID_ROUTE(0x05, true),
    // UNICAST
    CHANNEL_GROUP_ROUTE(0x06, false),
    CHANNEL_ACKED_GROUP_ROUTE(0x07, false),
    CHANNEL_BEST_EFFORT_GROUP_ROUTE(0x08, false),
    // BROADCAST
    CHANNEL_BROADCAST_GROUP_ROUTE(0x09, false),
    CHANNEL_ACKED_BROADCAST_GROUP_ROUTE(0x0A, false),
    CHANNEL_BEST_EFFORT_BROADCAST_GROUP_ROUTE(0x0B, false);
    
    private static RouteType[] typesById;
    
    private final int id;
    private final boolean hasDestination;
    
    /** Index types by id for indexed lookup. */
    static {
        int max = 0;
        
        for (RouteType t : values()) {
            max = Math.max(t.id, max);
        }
        
        typesById = new RouteType[max + 1];
        
        for (RouteType t : values()) {
            typesById[t.id] = t;
        }
    }
    
    RouteType(int id, boolean hasDestination) {
        this.id = id;
        this.hasDestination = hasDestination;
    }
    
    public int getEncodedType() {
        return id;
    }
    
    public boolean hasDestination() {
        return hasDestination;
    }
    
    public static RouteType from(int id) {
        return typesById[id];
    }
}
