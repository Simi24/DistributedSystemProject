package gRPC;

import AdministratorServer.beans.PlayerBean;
import Utils.Coordinate;
import Player.NetworkTopologyModule;
import com.greetings.grpc.GreetingServiceGrpc;
import com.greetings.grpc.GreetingServiceOuterClass;
import io.grpc.stub.StreamObserver;

public class GreetingsServiceImp extends GreetingServiceGrpc.GreetingServiceImplBase {

    @Override
    public void sayHello(GreetingServiceOuterClass.HelloRequest request, StreamObserver<GreetingServiceOuterClass.HelloReply> responseObserver) {
        System.out.println("Received a new player " + request.getPlayer().getId());

        GreetingServiceOuterClass.Player receivedPlayer = request.getPlayer();
        Coordinate receivedCoordinate = new Coordinate(receivedPlayer.getCoordinates().getX(), receivedPlayer.getCoordinates().getY());

        // Create a Player object from the received data
        PlayerBean player = new PlayerBean(receivedPlayer.getId(), receivedPlayer.getAddress(), receivedPlayer.getPortNumber());

        // Update the HashMap with the received player and coordinates
        NetworkTopologyModule.getInstance().addNewPlayerToNetworkTopology(player, receivedCoordinate);

        boolean isSeeker = NetworkTopologyModule.getInstance().getCurrentPlayer().getIsSeeker();

        GreetingServiceOuterClass.HelloReply response = GreetingServiceOuterClass.HelloReply.newBuilder()
                .setGameStatus(GreetingServiceOuterClass.GameStatus.forNumber(NetworkTopologyModule.getInstance().getGameStatus()))
                .setRole(GreetingServiceOuterClass.Role.forNumber(isSeeker ? 0 : 1))
                .setId(NetworkTopologyModule.getInstance().getCurrentPlayer().getId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
