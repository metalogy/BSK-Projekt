package routing.messages;

import core.DTNHost;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RouteReply {
    private DTNHost source;
    private DTNHost destination;
    private int sequence;
    private int hop;
}
