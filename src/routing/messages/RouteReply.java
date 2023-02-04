package routing.messages;

import core.DTNHost;

public class RouteReply {
    private DTNHost source;
    private DTNHost destination;
    private int sequence;
    private int hop;

    public DTNHost getSource() {
        return source;
    }

    public void setSource(DTNHost source) {
        this.source = source;
    }

    public DTNHost getDestination() {
        return destination;
    }

    public void setDestination(DTNHost destination) {
        this.destination = destination;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getHop() {
        return hop;
    }

    public void setHop(int hop) {
        this.hop = hop;
    }
}
