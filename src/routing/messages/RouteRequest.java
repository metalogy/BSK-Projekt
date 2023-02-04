package routing.messages;

import core.DTNHost;

public class RouteRequest {
    private DTNHost id;
    private DTNHost destination;
    private String broadcastId;
    private int hop = 0;
    private int sequence;

    public DTNHost getId() {
        return id;
    }

    public void setId(DTNHost id) {
        this.id = id;
    }

    public DTNHost getDestination() {
        return destination;
    }

    public void setDestination(DTNHost destination) {
        this.destination = destination;
    }

    public String getBroadcastId() {
        return broadcastId;
    }

    public void setBroadcastId(String broadcastId) {
        this.broadcastId = broadcastId;
    }

    public int getHop() {
        return hop;
    }

    public void setHop(int hop) {
        this.hop = hop;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
}
