package gRPC;

import AdministratorServer.beans.Player;
import Utils.Coordinate;
import Player.NetworkTopologyModule;
import com.greetings.grpc.GreetingServiceGrpc;
import com.greetings.grpc.GreetingServiceOuterClass;
import io.grpc.stub.StreamObserver;

public class GreetingsServiceImp extends GreetingServiceGrpc.GreetingServiceImplBase {

    @Override
    public void sayHello(GreetingServiceOuterClass.HelloRequest request, StreamObserver<GreetingServiceOuterClass.HelloReply> responseObserver) {
        System.out.println(request);

        GreetingServiceOuterClass.Player receivedPlayer = request.getPlayer();
        Coordinate receivedCoordinate = new Coordinate(receivedPlayer.getCoordinates().getX(), receivedPlayer.getCoordinates().getY());

        // Create a Player object from the received data
        Player player = new Player(receivedPlayer.getId(), receivedPlayer.getAddress(), receivedPlayer.getPortNumber());

        // Update the HashMap with the received player and coordinates
        NetworkTopologyModule.getInstance().updateNetworkTopology(player, receivedCoordinate);

        GreetingServiceOuterClass.HelloReply response = GreetingServiceOuterClass.HelloReply.newBuilder()
                .setMessage("Hello " + request.getPlayer())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}