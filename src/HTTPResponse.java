import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A Class for generating a response for HTTP requests.
 */
public class HTTPResponse {
    private static final Map<Integer, String> statusMessages = new HashMap<>();
    private final HTTPRequest request;
    private final String CRLF = "\r\n";
    private DataOutputStream outputStream;

    /**
     * Constructor for the HTTPResponse class.
     *
     * @param request - The HTTPRequest object that this response is associated with.
     */
    public HTTPResponse(HTTPRequest request) {
        this.request = request;
    }

    /**
     * Initializes the output stream for sending responses.
     *
     * @param outputStream - the output stream to be initialized
     */
    public void initialize(OutputStream outputStream) {
        this.outputStream = new DataOutputStream(outputStream);
        initializeStatusMessages();
    }

    /**
     * Initializes the status message map with the supported HTTP status codes and their corresponding messages.
     */
    void initializeStatusMessages() {
        statusMessages.put(200, "OK");
        statusMessages.put(400, "Bad Request");
        statusMessages.put(404, "Not Found");
        statusMessages.put(500, "Internal Server Error");
        statusMessages.put(501, "Not Implemented");
    }

    /**
     * Sends an HTTP response with the given status code and status message.
     *
     * @param statusCode - the HTTP status code
     * @throws IOException if an I/O error occurs while sending the response
     */
    void sendHttpResponse(int statusCode) throws IOException {
        try {
            String responseHeader = generateHTTPResponseHeader(statusCode);
            outputStream.writeBytes(responseHeader);

            if (request.isChunkedEncoding()) {
                sendResponseInChunks();
            } else {
                sendResponseInSingleChunk();
            }
            outputStream.flush();
            printHttpResponseHeaders(responseHeader);

        } catch (Exception e) {
            if (!request.getErrorFlag()) {
                request.setErrorFlag(true);
                sendErrorResponse(500);
            }
        }
    }

    /**
     * Sends the response in chunks if chunked encoding is requested by the client.
     *
     * @throws IOException if an I/O error occurs while sending the response
     */
    private void sendResponseInChunks() throws IOException {
        byte[] responseData = request.getRequestContent();
        int chunkSize = 1000;

        for (int i = 0; i < responseData.length; i += chunkSize) {
            int length = Math.min(chunkSize, responseData.length - i);
            outputStream.writeBytes(Integer.toHexString(length) + CRLF);
            outputStream.write(responseData, i, length);
            outputStream.writeBytes(CRLF);
        }
        outputStream.writeBytes("0" + CRLF + CRLF);
    }

    /**
     * Sends the entire response data in a single chunk.
     *
     * @throws IOException if an I/O error occurs while sending the response
     */
    private void sendResponseInSingleChunk() throws IOException {
        byte[] responseData = request.getRequestContent();
        outputStream.write(responseData);
    }

    /**
     * Sends an error response with the given status code and status message.
     *
     * @param statusCode - the HTTP status code
     */
    public void sendErrorResponse(int statusCode) {
        try {
            String statusMessage = statusMessages.get(statusCode);
            String content = String.join(" ", String.valueOf(statusCode), statusMessage);

            if (request != null) {
                request.setContentType("text/plain");
                request.setRequestContent(content.getBytes());

                if (!request.getErrorFlag()) {
                    request.setErrorFlag(true);
                    sendHttpResponse(statusCode);
                }
            }
        } catch (IOException e) {
            System.out.println("500 Internal Error\nClient connection has disconnected " + e.getMessage());
        }
    }

    /**
     * Generates the HTTP response for TRACE/ HEAD/ GET/ POST requests.
     * The method differentiates between a response to a TRACE request and the other requests.
     *
     * @param statusCode - status code to reply to a client
     * @return a response as a String.
     */
    private String generateHTTPResponseHeader(int statusCode) {
        String statusMessage = statusMessages.get(statusCode);
        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + CRLF;

        if (request.isChunkedEncoding()) {
            response += "Transfer-Encoding: chunked" + CRLF;
        } else {
            response += "Content-Length: " + request.getContentLength() + CRLF;
        }

        response += "Content-Type: " + this.request.getContentType() + CRLF;

        return response + CRLF;
    }


    /**
     * Sends a response to a TRACE request without headers to the client.
     */
    public void sendRequestOnTraceCommand() {
        try {
            request.setRequestContent(request.getRequest().getBytes());
            request.setContentType(request.getDefaultContentType());
            sendHttpResponse(200);
            System.out.println(request.getRequest());
        } catch (IOException e) {
            sendErrorResponse(500);
        }
    }

    /**
     * Prints the HTTP response headers to the console.
     *
     * @param responseHeader - the HTTP response header to be printed
     */
    private void printHttpResponseHeaders(String responseHeader) {
        System.out.println("HTTP Response:\n" + responseHeader);
    }

    /**
     * Closes the client output stream.
     */
    public void closeOutputStream() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            sendErrorResponse(500);
        }
    }
}
