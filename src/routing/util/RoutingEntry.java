package routing.util;

import core.DTNHost;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Builder
@Getter
@Setter
public class RoutingEntry {
    private DTNHost destinationNode;
    private DTNHost nextNode;
    private int sequence;
    private int hop;
}
