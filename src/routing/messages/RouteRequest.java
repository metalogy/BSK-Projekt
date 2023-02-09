package routing.messages;

import core.DTNHost;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RouteRequest {
    private DTNHost sender;
    private DTNHost destination;
    private String requestId;
    private int sequence;
    private int hop;
}
