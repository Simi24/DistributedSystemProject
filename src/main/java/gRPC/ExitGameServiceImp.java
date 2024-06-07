package gRPC;

import Player.NetworkTopologyModule;
import exitGame.ExitGameServiceGrpc;
import exitGame.ExitGameServiceOuterClass;
import io.grpc.stub.StreamObserver;

public class ExitGameServiceImp extends  ExitGameServiceGrpc.ExitGameServiceImplBase {

    @Override
    public void exitGame(ExitGameServiceOuterClass.ExitGameRequest request, StreamObserver<ExitGameServiceOuterClass.ExitGameResponse> responseObserver) {
        System.out.println("Received a request to exit game from " + request.getId() + " because it's: " + request.getReason());

        NetworkTopologyModule.getInstance().removePlayerFromNetworkTopology(request.getId());

        ExitGameServiceOuterClass.ExitGameResponse response = ExitGameServiceOuterClass.ExitGameResponse
                .newBuilder()
                .setMessage("Bye bye!!!")
                .build();

        responseObserver.onNext(response);
    }
}
