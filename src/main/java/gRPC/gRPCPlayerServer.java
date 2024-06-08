package gRPC;

import io.grpc.ServerBuilder;

import java.io.IOException;

public class gRPCPlayerServer {
    int port;
    
    public gRPCPlayerServer(int port) {
        this.port = port;
    }
    
    public void start() throws IOException {
        // Start the gRPC server
        io.grpc.Server server = ServerBuilder.forPort(port)
                .addService(new GreetingsServiceImp())
                .addService(new ElectionServiceImp())
                .addService(new AccessBaseServiceImp())
                .addService(new ExitGameServiceImp())
                .addService(new TagPlayerServiceImp())
                .build();
        server.start();
        System.out.println("Server started for gRPC at port: " + port);
    }
    
}