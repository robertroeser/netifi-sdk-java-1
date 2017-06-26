package io.netifi.sdk.connection;

public interface Availability {
    
    /**
     * @return a positive numbers representing the availability of the entity. Higher is better, 0.0
     *     means not available
     */
    double availability();
}
