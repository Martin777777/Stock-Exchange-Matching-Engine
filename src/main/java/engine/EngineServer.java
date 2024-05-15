package engine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EngineServer {
    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;

    public void start() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Exchange Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");

                executorService.submit(new TaskHandler(clientSocket));
            }
        } catch (IOException ex) {
            System.err.println("Server exception: " + ex.getMessage());
        } finally {
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }
}
