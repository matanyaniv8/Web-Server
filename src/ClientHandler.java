import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java class that handles a TCP connection between a multithreading server and processes
 * GET, POST, HEAD, and TRACE HTTP methods.
 */
public class ClientHandler implements Runnable {
    private final String CRLF = "\r\n";
    private final Socket clientSocket;
    private final ServerConfiguration serverConfig;

    private HTTPRequest request;
    private HTTPResponse response;
    private BufferedReader socketDataReader = null;

    /**
     * Constructs a ClientHandler object with the given client socket and server configuration.
     *
     * @param clientSocket - the client socket
     * @param serverConfiguration - the server configuration
     * @throws IllegalArgumentException if the client socket is null
     */
    public ClientHandler(Socket clientSocket, ServerConfiguration serverConfiguration) {
        if (clientSocket == null) {
            throw new IllegalArgumentException("Client socket cannot be null!");
        }

        this.clientSocket = clientSocket;
        this.serverConfig = serverConfiguration;
    }

    @Override
    public void run() throws StackOverflowError{
        handleClientRequest();
    }

    /**
     * Handles the client request by reading the incoming data from the socket input buffer and processing the HTTP request.
     */
    private void handleClientRequest(){
        try {
            String requestString = readIncomingDataFromSocket();
            this.request = new HTTPRequest(requestString, serverConfig);
            this.response = new HTTPResponse(request);
            this.response.initialize(clientSocket.getOutputStream());

            if (request.isRequestValid()) {
                processHttpRequest();
            } else {
                response.sendErrorResponse(400);
            }
        } catch (IOException e) {
            WebServer.notifyInternalServerError(response, e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Reads the incoming data from the socket's input buffer.
     *
     * @return the incoming data from the socket
     */
    private String readIncomingDataFromSocket() {
        String requestLine;
        StringBuilder requestPartsContainer = new StringBuilder();

        try {
            socketDataReader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream(), StandardCharsets.UTF_8));

            while ((requestLine = socketDataReader.readLine()) != null && !requestLine.isEmpty()) {
                requestPartsContainer.append(requestLine);

                if (!requestLine.endsWith(CRLF)) {
                    requestPartsContainer.append(CRLF);
                }
            }

            if (!requestPartsContainer.toString().endsWith(CRLF + CRLF)) {
                requestPartsContainer.append(CRLF);
            }
        } catch (Exception e) {
            WebServer.notifyInternalServerError(response, e.getMessage());
        }

        return requestPartsContainer.toString();
    }

    /**
     * Safe closing of the socket and its buffer reader.
     */
    public void close() {
        try {
            if (socketDataReader != null) {
                socketDataReader.close();
            }

            if (response != null) {
                response.closeOutputStream();
            }

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing resources: " + e.getMessage());
            WebServer.notifyInternalServerError(response, e.getMessage());
        }
    }

    /**
     * Processes the HTTP request based on its method type.
     */
    public void processHttpRequest() {
        System.out.println("HTTP Request:\n" + request.getRequest());
        try {
            switch (request.getMethodType()) {
                case "GET":
                    handleGETRequest();
                    break;
                case "POST":
                    handlePOSTRequest();
                    break;
                case "HEAD":
                    handleHEADRequest();
                    break;
                case "TRACE":
                    handleTRACERequest();
                    break;
                default:
                    response.sendErrorResponse(501);
            }
        } catch (Exception e) {
            WebServer.notifyInternalServerError(response, e.getMessage());
        }
    }

    /**
     * Handles a GET request.
     *
     * @throws IOException if unable to reach the given path or connection with the socket is lost
     */
    private void handleGETRequest() throws IOException {
        sendRequestedFileToClient();
    }

    /**
     * Reads the files written to the request's path and sends them back to the client if they exist and are within
     * the root directory (listed in the config.ini file).
     * The method is mainly for the response of the GET and POST methods.
     *
     * @throws IOException if unable to read files or the socket connection is closed
     */
    private void sendRequestedFileToClient() throws IOException {
        Path requestedPagePath = request.getRequestedPage();

        if (Files.exists(requestedPagePath) && requestedPagePath.startsWith(serverConfig.getRoot())) {
            byte[] content = Files.readAllBytes(requestedPagePath);
            request.setRequestContent(content);
            response.sendHttpResponse(200);
        } else {
            response.sendErrorResponse(404);
        }
    }

    /**
     * Handles a POST request.
     * It's extract the params from the entity body of the request and updates the request's params field.
     * It also responds, according to the path given in the request.
     *
     * @throws IOException if unable to read or write from/to the socket
     */
    private void handlePOSTRequest() throws IOException {
        String requestEntityBody = getEntityBody();
        request.setRequestParams(requestEntityBody);
        sendRequestedFileToClient();
    }

    /**
     * Extracts the entity body of a POST request.
     *
     * @return the entity body of the request as a String
     * @throws IOException if unable to read from the socket's buffer
     */
    private String getEntityBody()throws IOException {
        String entityBodyFromRequest = "";

        if(request.getMethodType().equalsIgnoreCase("post")){
            char[] entityBodyInfo = new char[request.getContentLength()];
            int numOfCharsReadFromReader = socketDataReader.read(entityBodyInfo);
            entityBodyFromRequest = (numOfCharsReadFromReader > -1) ? new String(entityBodyInfo): entityBodyFromRequest;
        }

        return URLDecoder.decode(entityBodyFromRequest, StandardCharsets.UTF_8);
    }

    /**
     * Handles a HEAD request by sending all the headers to the client, except for the data.
     *
     * @throws IOException if the connection between the server and the socket has disconnected
     */
    private void handleHEADRequest() throws IOException {
        Path requestedPagePath = request.getRequestedPage();

        if (Files.exists(requestedPagePath) && requestedPagePath.startsWith(serverConfig.getRoot())) {
            response.sendHttpResponse(200);
            request.setContentLength((int) Files.size(requestedPagePath));
        } else {
            response.sendErrorResponse(404);
        }
    }

    /**
     *  Handles a TRACE request.
     * It calls the response's TRACE handler which sends OK,200 To a client, as well as the client's request.
     */
    private void handleTRACERequest() {
        response.sendRequestOnTraceCommand();
    }
}
