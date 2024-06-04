package gRPC;

import Player.NetworkTopologyModule;
import Utils.Coordinate;
import election.ElectionServiceGrpc;
import election.ElectionServiceOuterClass;
import io.grpc.stub.StreamObserver;

public class ElectionServiceImp extends ElectionServiceGrpc.ElectionServiceImplBase {

    @Override
    public void startElection(ElectionServiceOuterClass.ElectionRequest request, StreamObserver<ElectionServiceOuterClass.ElectionResponse> responseObserver) {
        String playerId = request.getPlayerId();

        NetworkTopologyModule networkTopologyModule = NetworkTopologyModule.getInstance();

        Coordinate receivedCoordinate = new Coordinate(request.getCoordinates().getX(), request.getCoordinates().getY());

        networkTopologyModule.updatePlayerCoordinate(playerId, receivedCoordinate);

        ElectionServiceOuterClass.ElectionResponse response = ElectionServiceOuterClass.ElectionResponse.newBuilder()
                .setMessage(networkTopologyModule.playerIsCloserThenCurrentPlayer(playerId).toString())
                .build();

        responseObserver.onNext(response);
    }

    @Override
    public void acknowledgeElection(ElectionServiceOuterClass.ElectionRequest request, StreamObserver<ElectionServiceOuterClass.ElectionResponse> responseObserver) {
        super.acknowledgeElection(request, responseObserver);
    }

    @Override
    public void declareVictory(ElectionServiceOuterClass.ElectionRequest request, StreamObserver<ElectionServiceOuterClass.ElectionResponse> responseObserver) {
        super.declareVictory(request, responseObserver);
    }
}
