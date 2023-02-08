package routing.messages;

import core.DTNHost;
import core.Message;

public class RouteReply extends Message {
    private DTNHost source;
    private DTNHost destination;
    private int sequence;
    private int hop;
    private int lifetime;

    /**
     * Creates a new Message.
     *
     * @param from Who the message is (originally) from
     * @param to   Who the message is (originally) to
     * @param id   Message identifier (must be unique for message but
     *             will be the same for all replicates of the message)
     * @param size Size of the message (in bytes)
     */
    public RouteReply(DTNHost from, DTNHost to, String id, int size) {
        super(from, to, id, size);
    }

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

    public int getLifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }
}
