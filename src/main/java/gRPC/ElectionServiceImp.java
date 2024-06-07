package gRPC;

import Player.NetworkTopologyModule;
import Player.Player;
import Utils.Coordinate;
import election.ElectionServiceGrpc;
import election.ElectionServiceOuterClass;
import io.grpc.stub.StreamObserver;

public class ElectionServiceImp extends ElectionServiceGrpc.ElectionServiceImplBase {

    @Override
    public void startElection(ElectionServiceOuterClass.ElectionRequest request, StreamObserver<ElectionServiceOuterClass.ElectionResponse> responseObserver) {
        System.out.println("Received a new election request " + request.getCoordinates().getX() + " " + request.getCoordinates().getY() + " ");

        String playerId = request.getPlayerId();

        NetworkTopologyModule networkTopologyModule = NetworkTopologyModule.getInstance();

        Coordinate receivedCoordinate = new Coordinate(request.getCoordinates().getX(), request.getCoordinates().getY());

        networkTopologyModule.updatePlayerCoordinate(playerId, receivedCoordinate);

        boolean isCloser = networkTopologyModule.playerIsCloserThenCurrentPlayer(playerId);

        System.out.println("Sending election response " + isCloser);

        ElectionServiceOuterClass.ElectionResponse response = ElectionServiceOuterClass.ElectionResponse.newBuilder()
                .setMessage(String.valueOf(isCloser))
                .build();

        responseObserver.onNext(response);
    }

    @Override
    public void acknowledgeElection(ElectionServiceOuterClass.ElectionRequest request, StreamObserver<ElectionServiceOuterClass.ElectionResponse> responseObserver) {
        super.acknowledgeElection(request, responseObserver);
    }

    @Override
    public void declareVictory(ElectionServiceOuterClass.ElectionRequest request, StreamObserver<ElectionServiceOuterClass.ElectionResponse> responseObserver) {
        System.out.println("Received a new election victory from " + request.getPlayerId() + " " + request.getCoordinates().getX() + " " + request.getCoordinates().getY() + " ");
        NetworkTopologyModule.getInstance().setSeeker(request.getPlayerId());
        NetworkTopologyModule.getInstance().removeSeekerFromNetworkTopology();

        ElectionServiceOuterClass.ElectionResponse response = ElectionServiceOuterClass.ElectionResponse.newBuilder()
                .setMessage("Got it! You are the new seeker!")
                .build();

        synchronized (NetworkTopologyModule.getInstance().getCurrentPlayer().lock) {
            NetworkTopologyModule.getInstance().getCurrentPlayer().lock.notify();
        }

        responseObserver.onNext(response);
    }
}
