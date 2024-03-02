import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A TCP Multi-thread web server class.
 */
public class WebServer {
    private final ServerConfiguration serverConfig;
    private final ExecutorService executorService;

    /**
     * Constructs a WebServer object with the given server configuration.
     *
     * @param serverConfig - the server configuration
     */
    public WebServer(ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.executorService = Executors.newFixedThreadPool(serverConfig.getMaxThreads());
    }

    /**
     * Starts the web server and listens for incoming client connections.
     * Creates a new thread to handle each client request.
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(serverConfig.getPort())) {
            System.out.println("Web server started on port " + serverConfig.getPort());

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.execute(new ClientHandler(clientSocket, serverConfig));
                } catch (Exception | StackOverflowError e) {
                    notifyInternalServerError(null, e.getMessage());
                }
            }
        } finally {
            try {
                executorService.shutdownNow();
            } catch (Exception e) {
                notifyInternalServerError(null, e.getMessage());
            }
        }
    }

    /**
     * Notifies about an internal server error by sending a corresponding HTTP response or printing a message to the console.
     *
     * @param response - The HTTPResponse object to send the error response through. If null, the error message is printed to the console.
     * @param errorMessage - More information about the error.
     */
    public static void notifyInternalServerError(HTTPResponse response, String errorMessage) {
        if (response != null) {
            response.sendErrorResponse(500);
        } else {
            System.out.println("500 Internal Error\n" + errorMessage);
        }
    }

    /**
     * Entry point for starting the web server.
     */
    public static void main(String[] args) {
        try {
            ServerConfiguration serverConfig = ServerConfiguration.loadConfig("./config.ini");
            WebServer webServer = new WebServer(serverConfig);
            webServer.start();
        } catch (Exception e) {
            notifyInternalServerError(null, e.getMessage());
        }
    }
}
