package gRPC;

import Player.NetworkTopologyModule;
import io.grpc.stub.StreamObserver;
import reachBase.AccessBaseService;
import reachBase.AccessServiceGrpc;

public class AccessBaseServiceImp extends AccessServiceGrpc.AccessServiceImplBase {

    @Override
    public void requestAccess(AccessBaseService.AccessRequest request, StreamObserver<AccessBaseService.AccessResponse> responseObserver) {
        System.out.println("Received a new access request from " + request.getId());

        long timeStampRequested = request.getTimestamp();

        boolean granted = !NetworkTopologyModule.getInstance().isMyTimeStampMinor(timeStampRequested);
        System.out.println("Access granted: " + granted);

        //If the request is not granted, add the player to the list of players to give access when player leaves the base
        if(!granted && NetworkTopologyModule.getInstance().getRequestTimeStamp() != 0) {
            NetworkTopologyModule.getInstance().playersListToGiveAccess.add(request.getId());
            System.out.println(NetworkTopologyModule.getInstance().playersListToGiveAccess);
        }

        AccessBaseService.AccessResponse response = AccessBaseService.AccessResponse.newBuilder()
                .setGranted(granted)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void grantAccess(AccessBaseService.AccessResponse request, StreamObserver<AccessBaseService.Thanks> responseObserver) {
        System.out.println("Received a new access grant ");

        NetworkTopologyModule.getInstance().incrementGrantCounter();

        AccessBaseService.Thanks response = AccessBaseService.Thanks.newBuilder()
                .setMessage("Thanks mate!")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
