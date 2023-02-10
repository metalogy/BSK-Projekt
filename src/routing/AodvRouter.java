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
    private ArrayList<RouteRequest> historyRouteRequests = new ArrayList<>();

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

    public void unicastMessage(String recipent, Message message) {
        Optional<Connection> connection = getConnectionForDestinationNextNode(recipent);
        connection.ifPresent(value -> startTransfer(message, value));
    }

    public void broadcastRreqToAllNeighbours(RouteRequest routeRequest, DTNHost dtnHost) {
        getConnections()
                .stream().filter(connection -> connection.getOtherNode(this.getHost()) != dtnHost)
                .forEach(connection -> this.getAodvRouterFromDtnHost(connection.getOtherNode(this.getHost()))
                        .receiveRreq(routeRequest, connection.getOtherNode(this.getHost())));
    }

    public AodvRouter getAodvRouterFromDtnHost(DTNHost dtnHost) {
        MessageRouter router = dtnHost.getRouter();
        assert router instanceof AodvRouter : "Aodv only works with other routers of same type";
        return (AodvRouter) router;
    }

    public void receiveRreq(RouteRequest routeRequest, DTNHost rreqSender) {

        //jeżeli request ten nie został już przetworzony
        if (!isRouteRequestInHistoryTable(routeRequest) && rreqSender != this.getHost()) {

            //dodaj do tablicy przetworzonych
            this.historyRouteRequests.add(routeRequest);

            //zaaktualizuj tablicę routingu
            if (isNodeInRoutingTable(routeRequest.getSender())) {
                RoutingEntry routingTableEntry = routingTable.get(routeRequest.getSender().toString());

                if (!isRoutingTableEntryUpToDate(routeRequest)
                        || routingTableEntry.getHop() > routeRequest.getHop() + 1) { //route request nowszy lub ma krótszą ścieżkę

                    RoutingEntry routingEntry = RoutingEntry.builder()
                            .destinationNode(routeRequest.getSender())
                            .nextNode(rreqSender)
                            .hop(routeRequest.getHop() + 1)
                            .sequence(routeRequest.getSequence())
                            .build();

                    this.routingTable.replace(routeRequest.getSender().toString(), routingEntry);
                }

            } else {

                //dodaj to tablicy routingu nowy wpis
                RoutingEntry routingEntry = RoutingEntry.builder()
                        .destinationNode(routeRequest.getSender())
                        .nextNode(rreqSender)
                        .hop(routeRequest.getHop() + 1)
                        .sequence(routeRequest.getSequence())
                        .build();

                this.routingTable.put(routeRequest.getSender().toString(), routingEntry);
            }

            //sprawdz czy do ciebie
            if (routeRequest.getDestination().equals(this.getHost())) {

                //adresowane do nas wyślij rrep do noda od którego otrzymalismy pakiet RREQ
                RouteReply routeReply = RouteReply.builder()
                        .source(this.getHost())
                        .destination(routeRequest.getSender())
                        .sequence(SimClock.getIntTime())
                        .hop(0)
                        .build();

                getAodvRouterFromDtnHost(rreqSender).receiveRrep(routeReply, this.getHost()); //wyślij RREP
            } else if (isNodeInRoutingTable(routeRequest.getDestination()) //nie do nas lecz adresata w tablicy routingu
                    && isRoutingTableEntryUpToDate(routeRequest)) { //wpis w tablicy routingu jest ważny

                //zczytuje z tablicy routingu
                RoutingEntry routingEntry = this.routingTable.get(routeRequest.getDestination().toString());

                //wyślij rrep do tego od kogo otrzymaliśmy pakiet
                RouteReply routeReply = RouteReply.builder()
                        .source(routingEntry.getDestinationNode())
                        .destination(routeRequest.getSender())
                        .sequence(routingEntry.getSequence())
                        .hop(routingEntry.getHop())
                        .build();

                this.getAodvRouterFromDtnHost(rreqSender).receiveRrep(routeReply, this.getHost()); //wyślij RREP
            } else {
                //zwieksz hop counta
                routeRequest.setHop(routeRequest.getHop() + 1);
                //rebroadcastuj rreq pakiet do sąsiadów
                this.broadcastRreqToAllNeighbours(routeRequest, rreqSender);
            }
        }
    }

    private boolean isRouteRequestInHistoryTable(RouteRequest routeRequest) {
        return this.historyRouteRequests.stream().anyMatch(historyRouteRequest ->
                historyRouteRequest.getSender().equals(routeRequest.getSender())
                        && historyRouteRequest.getRequestId().equals(routeRequest.getRequestId()));
    }

    private boolean isNodeInRoutingTable(DTNHost dtnHost) {
        return this.routingTable.containsKey(dtnHost.toString());
    }

    private boolean isRoutingTableEntryUpToDate(RouteRequest routeRequest) {
        return this.routingTable.get(routeRequest.getDestination().toString()).getSequence() >= routeRequest.getSequence();
    }

    public void receiveRrep(RouteReply routeReply, DTNHost rrepSender) {
        //zapisz do routing table
        RoutingEntry routingEntry = RoutingEntry.builder()
                .destinationNode(routeReply.getSource())
                .nextNode(rrepSender)
                .hop(routeReply.getHop() + 1)
                .sequence(routeReply.getSequence())
                .build();

        routingTable.put(routeReply.getSource().toString(), routingEntry);

        //jezli nie do ciebie to przekaż dalej
        if (routeReply.getDestination() != this.getHost()) {

            //przesyłamy RREP
            this.getAodvRouterFromDtnHost(this.routingTable.get(routeReply.getDestination().toString()).getNextNode())
                    .receiveRrep(routeReply, this.getHost());
        }
    }

    public Optional<Connection> getConnectionForDestinationNextNode(String destination) {
        DTNHost destinationNextNode = routingTable.get(destination).getNextNode();

        return getConnections()
                .stream()
                .filter(connection -> destinationNextNode.equals(connection.getOtherNode(getHost())))
                .findFirst();
    }

    @Override
    public void changedConnection(Connection connection) {
        //zaaktualizauj tablice routingu w oparciu o połaczenie
        DTNHost connectionNode = connection.getOtherNode(getHost());
        int connectionTime = SimClock.getIntTime(); //czas -> sequenceID

        if (connection.isUp()) { //jeżeli sąsiad wpisany kiedyś zaaktualizuj sequence
            RoutingEntry neighbourEntry = RoutingEntry.builder()
                    .destinationNode(connectionNode)
                    .nextNode(connectionNode)
                    .hop(1)
                    .sequence(connectionTime)
                    .build();


            routingTable.put(connectionNode.toString(), neighbourEntry);
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

                RouteRequest routeRequest = RouteRequest.builder()
                        .sender(this.getHost())
                        .destination(message.getTo())
                        .requestId(this.generateUuid())
                        .sequence(SimClock.getIntTime())
                        .hop(0)
                        .build();

                this.broadcastRreqToAllNeighbours(routeRequest, this.getHost());
            }
        });
    }

    private String generateUuid() {
        return UUID.randomUUID().toString();
    }

}