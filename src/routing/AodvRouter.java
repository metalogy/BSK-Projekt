package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import routing.messages.RouteReply;
import routing.messages.RouteRequest;
import routing.util.RoutingEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class AodvRouter extends ActiveRouter {
    private HashMap<String, RoutingEntry> routingTable;
//    private ArrayList<RouteRequest> routeRequests;
//    private ArrayList<RouteReply> routeReplies;

    protected AodvRouter(ActiveRouter r) {
        super(r);
    }

    @Override
    public MessageRouter replicate() {
        return new AodvRouter(this);
    }

    @Override
    public void update() {
        super.update();

        if (exchangeDeliverableMessages() == null && (canStartTransfer() || !isTransferring())) {
            ArrayList<Message> messages = new ArrayList<>(getMessageCollection());
            messages.forEach(message -> {
                String recipient = message.getTo().toString();
                if (routingTable.containsKey(recipient)) {
                    unicastMessage(recipient, message);
                } else {
                    broadcastMessage(message);
                }
            });
        }
    }

    public void unicastMessage(String recipent, Message message) {
        Optional<Connection> connection = getConnectionForDestinationNextNode(recipent);
        connection.ifPresent(value -> startTransfer(message, value));
    }

    public void broadcastMessage(Message message) {
        getConnections().forEach(connection -> startTransfer(message, connection));
    }

    public Optional<Connection> getConnectionForDestinationNextNode(String destination) { //todo 2 typy mozna zwrocic
        DTNHost destinationNextNode = routingTable.get(destination).getNextNode();

        return getConnections()
                .stream()
                .filter(connection -> destinationNextNode.equals(connection.getOtherNode(getHost())))
                .findFirst();
    }

    @Override
    public int receiveMessage(Message message, DTNHost sender) {
        int receiveCheckResult = checkReceiving(message, sender);

        return receiveCheckResult == RCV_OK
                ? saveToRoutingTableAndReceiveMessage(message, sender)
                : receiveCheckResult;
    }

    public int saveToRoutingTableAndReceiveMessage(Message message, DTNHost sender) {
        saveToRoutingTable(message, sender);
        return super.receiveMessage(message, sender);
    }

    public void saveToRoutingTable(Message message, DTNHost dtnHost) {
        if (routingTable.containsKey(dtnHost.toString())) {
            if (routingTable.get(dtnHost.toString()).getHop() > message.getHopCount()) {
                RoutingEntry routingEntry = RoutingEntry.builder()
                        .destinationNode(message.getFrom())
                        .nextNode(dtnHost)
                        .hop(message.getHopCount())
                        .build();
                routingTable.replace(dtnHost.toString(), routingEntry);
            }
        } else {
            RoutingEntry routingEntry = RoutingEntry.builder()
                    .destinationNode(message.getFrom())
                    .nextNode(dtnHost)
                    .hop(message.getHopCount())
                    .build();

            routingTable.put(message.getFrom().toString(), routingEntry);
        }
    }

    @Override
    public void changedConnection(Connection connection) { //kiedy to wykonywane
        if (!connection.isUp()) {
            DTNHost nodeToRemove = connection.getOtherNode(getHost());
            routingTable.remove(nodeToRemove.toString());
        }
    }
}