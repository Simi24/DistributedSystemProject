package gRPC;

import com.greetings.grpc.GreetingServiceGrpc;
import com.greetings.grpc.GreetingServiceOuterClass;
import io.grpc.stub.StreamObserver;

public class GreetingsServiceImp extends GreetingServiceGrpc.GreetingServiceImplBase {

    @Override
    public void sayHello(GreetingServiceOuterClass.HelloRequest request, StreamObserver<GreetingServiceOuterClass.HelloReply> responseObserver) {
        System.out.println(request);

        GreetingServiceOuterClass.HelloReply response = GreetingServiceOuterClass.HelloReply.newBuilder()
                .setMessage("Hello " + request.getPlayer())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
