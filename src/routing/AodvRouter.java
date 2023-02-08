package routing;

import core.*;
import routing.messages.RouteReply;
import routing.messages.RouteRequest;
import routing.util.RoutingEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class AodvRouter extends ActiveRouter {
    private HashMap<String, RoutingEntry> routingTable = new HashMap<>();
    private ArrayList<RouteRequest> routeRequests;
    private ArrayList<RouteReply> routeReplies;

    public AodvRouter(Settings s) {
        super(s);
    }

    protected AodvRouter(ActiveRouter r) {
        super(r);
    }

    @Override
    public MessageRouter replicate() {
        return new AodvRouter(this);
    }

//    @Override
//    public void update() {
//        super.update();
//
//        if (exchangeDeliverableMessages() == null && (canStartTransfer() || !isTransferring())) {
////            ArrayList<Message> messages = new ArrayList<>(getMessageCollection());
//            routeRequests.forEach(routeRequest -> {
//                String recipient = message.getTo().toString();
//                if (routingTable.containsKey(recipient)) { //TODO sprawdz
//                    unicastMessage(recipient, message);
//                } else {
//                    broadcastMessage(message);
//                }
//            });
//        }
//    }

    @Override
    public void update() {
        super.update();

        routeRequests.forEach(routeRequest -> {
            //roześlij do wszystkich sąsiadów
            this.getConnections().forEach(connection -> );
        });
    }

    public void unicastMessage(String recipent, Message message) {
        Optional<Connection> connection = getConnectionForDestinationNextNode(recipent);
        connection.ifPresent(value -> startTransfer(message, value));
    }

//    public void broadcastMessage(Message message) {
//        getConnections().forEach(connection -> startTransfer(message, connection));
//    }

    public void broadcastRreqToNeighbour(RouteRequest routeRequest) {
        getConnections().forEach(connection -> receiveRreq(routeRequest, connection));
    }

    public void receiveRreq(RouteRequest routeRequest, Connection connection){
        //sprawdz czy do ciebie
        //sprawdź z tablicą historyczną routingu i porównaj z id requestu
        //sprawdź numer sekwencji i ew. odeślij do nadawcy
        //zaaktualizuj swoją tablice routingu
        //jak nie ponownie rozeslij rreq
    }

    public void sendRrep(RouteReply routeReply) {
        //todo
    }

    public void receiveRrep(RouteReply routeReply, Connection connection){
       //todo
    }

    public Optional<Connection> getConnectionForDestinationNextNode(String destination) { //todo 2 typy mozna zwrocic
        DTNHost destinationNextNode = routingTable.get(destination).getNextNode();

        return getConnections()
                .stream()
                .filter(connection -> destinationNextNode.equals(connection.getOtherNode(getHost())))
                .findFirst();
    }

//    @Override
//    public int receiveMessage(Message message, DTNHost sender) {
//        int receiveCheckResult = checkReceiving(message, sender);
//
//        return receiveCheckResult == RCV_OK
//                ? saveToRoutingTableAndReceiveMessage(message, sender)
//                : receiveCheckResult;
//    }

    public int saveToRoutingTableAndReceiveMessage(Message message, DTNHost sender) {
        saveToRoutingTable(message, sender);
        return super.receiveMessage(message, sender);
    }

    public void saveToRoutingTable(Message message, DTNHost dtnHost) {
        if (routingTable.containsKey(dtnHost.toString())) { //todo sprawdz
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
    public void changedConnection(Connection connection) {
        //zaaktualizauj tablice routingu w oparciu o połaczenie
        DTNHost connectionNode = connection.getOtherNode(getHost());
        int connectionTime = SimClock.getIntTime(); //czas -> sequenceID

        if (connection.isUp()) { //jeżeli sąsiad wpisany kiedyś zaaktualizuj sequence
            if (routingTable.containsKey(connectionNode) && routingTable.get(connectionNode).getSequence() < connectionTime) { //toodo sprawdz
                RoutingEntry oldEntry = routingTable.get(connectionNode);
                RoutingEntry newEntry = RoutingEntry.builder()
                        .destinationNode(oldEntry.getDestinationNode())
                        .nextNode(oldEntry.getNextNode())
                        .hop(oldEntry.getHop())
                        .sequence(connectionTime)
                        .build();
                routingTable.replace(connectionNode.toString(),
                        newEntry);
            } else // jeżeli nie masz sąsiada w tablicy routingu dodaj go
            {
                RoutingEntry newEntry = RoutingEntry.builder()
                        .destinationNode(connectionNode)
                        .nextNode(connectionNode)
                        .hop(1)
                        .sequence(connectionTime)
                        .build();
                routingTable.put(connectionNode.toString(), newEntry);
            }
        } else { //brak połączenie z sąsiadem usuń z tablicy routingu
            routingTable.remove(connectionNode.toString());

        }

        //sprawdzić jakie wiadomości do przekazania
        ArrayList<Message> messages = new ArrayList<>(getMessageCollection());
        messages.forEach(message -> {
            String recipient = message.getTo().toString();

            if (routingTable.containsKey(recipient)) {
                //posiadamy wpis w tablicy routingu -> wyślij wiadomość
                unicastMessage(recipient, message);
                //funkcja start transfer sama usunie wiadomość!!!
            } else {
                //nie mamy adresata, musimy rozesłać RREQ

                //todo sprawdzenie czy już nie mamy gotowego RREQ!!!
                RouteRequest routeRequest = RouteRequest.builder()
                        .sender(this.getHost())
                        .destination(message.getTo())
                        .broadcastId(UUID.randomUUID().toString())
                        .sequence(SimClock.getIntTime())
                        .hop(0)
                        .build();
                broadcastMessage(message);
                this.routeRequests.add(routeRequest);
            }
        });
    }

    public void updateNeighbourInRoutingTable() {
        //todo rozdziel i uporządkuj funkcje
    }
}