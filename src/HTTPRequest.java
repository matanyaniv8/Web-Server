import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * An HTTP Request parser class.
 * Gets an HTTP request and extracts the header's values from it.
 */
public class HTTPRequest {
    private static final String CRLF = "\r\n";
    private final String request;
    private final ServerConfiguration currentConfig;
    private final HashMap<String, String> requestParams = new HashMap<>();

    private Path requestedPage;
    private String methodType = "";
    private String contentType = "";
    private String refererHeader = "";
    private String userAgent = "";
    private byte[] requestContent = new byte[]{};
    private int contentLength = 0;
    private boolean isErrorOccurred = false;
    private boolean isRequestValid;

    /**
     * Constructs an HTTPRequest object with the given HTTP request string and server configuration.
     *
     * @param httpRequestString - the HTTP request string
     * @param serverConfig      - the server configuration
     * @throws IllegalArgumentException if the HTTP request string is null or empty
     */
    public HTTPRequest(String httpRequestString, ServerConfiguration serverConfig) throws IOException {
        isRequestValid = isValidHttpRequestFormat(httpRequestString);
        request = httpRequestString;
        currentConfig = serverConfig;
        requestedPage = currentConfig.getRoot();

        if (isRequestValid) {
            extractParamsAndInitParams(httpRequestString);
        }
    }

    public boolean isRequestValid() {
        return isRequestValid;
    }

    public String getDefaultContentType(){
        return "application/octet-stream";
    }

    public String getRequest() {
        return request;
    }

    public String getMethodType() {
        return methodType;
    }

    public Path getRequestedPage() {
        return requestedPage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int length) {
        contentLength = length;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String type) {
        if (type != null && !type.isEmpty()) {
            contentType = type;
        }
    }

    public String getRefererHeader() {
        return refererHeader;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public HashMap<String, String> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String paramsStr) throws IOException {
        extractQueryParamsFromStr(paramsStr);
    }

    public boolean getErrorFlag() {
        return isErrorOccurred;
    }

    public void setErrorFlag(boolean errorValue) {
        this.isErrorOccurred = errorValue;
    }

    public byte[] getRequestContent() {
        return requestContent;
    }

    public void setRequestContent(byte[] requestData) {
        if (requestData != null) {
            requestContent = requestData;
            contentLength = requestData.length;
        }
    }

    /**
     * Extracts all the header's values from a given HTTP request and initialize the class fields.
     *
     * @param httpRequest - the HTTP request string
     * @throws IOException if unable to extract data and write it to file or if the request is invalid.
     */
    private void extractParamsAndInitParams(String httpRequest) throws IOException {
        extractMethodTypeAndPath(httpRequest);
        contentType = getRequestedContentType(requestedPage);
        refererHeader = getHeaderValue(httpRequest, "Referer");
        userAgent = getHeaderValue(httpRequest, "User-Agent");
        extractQueryParams(httpRequest);
        contentLength = extractContentLengthOnPostRequest(httpRequest);
    }

    /**
     * Extracts the Method-Type and the Path of a URL from a given HTTP request.
     *
     * @param HTTPRequest - the HTTP request string
     */
    private void extractMethodTypeAndPath(String HTTPRequest) {
        String[] requestParts = HTTPRequest.split("\\s+");
        methodType = extractMethodType(requestParts);
        if (requestParts.length >= 2) {
            requestedPage = extractRequestedPage(requestParts[1]);
        }
    }

    /**
     * Extracts the HTTP method type from the request parts.
     *
     * @param requestParts - The parts of the HTTP request, split by whitespace.
     * @return The HTTP method type if it exists, otherwise an empty string.
     */
    private String extractMethodType(String[] requestParts) {
        return (requestParts.length > 0) ? requestParts[0] : "";
    }

    /**
     * Extracts the requested page from the file path.
     * If the file path is empty or "/", the default page is returned.
     * If the file path starts with "/", the file path is resolved relative to the root directory.
     *
     * @param filePath - The file path from the HTTP request.
     * @return The path of the requested page.
     */
    private Path extractRequestedPage(String filePath) {
        if (filePath.isEmpty() || filePath.equals("/")) {
            filePath = currentConfig.getDefaultPage();
        } else if (filePath.startsWith("/")) {
            filePath = extractFilePath(filePath);
        }

        return currentConfig.getRoot().resolve(filePath);
    }

    /**
     * Extracts the file path from the given file path string.
     * The file path string is split by "?", and the first part is returned.
     *
     * @param filePath - The file path string from the HTTP request.
     * @return The extracted file path.
     */
    private String extractFilePath(String filePath) {
        String[] requestParts = filePath.split("\\?");
        return requestParts[0].substring(1);
    }

    /**
     * Extract the Content-Type header from an HTTP request.
     *
     * @param filePath - the requested file path
     */
    private String getRequestedContentType(Path filePath) {
        String contentType = "application/octet-stream";

        if (filePath != null) {
            String fileName = filePath.getFileName().toString().toLowerCase();
            String[] fileNameParts = fileName.split("\\.");
            String fileType = (fileNameParts.length <= 1) ? contentType : fileNameParts[1];

            switch (fileType) {
                case "html":
                    contentType = "text/html";
                    break;
                case "bmp", "jpg", "gif", "png", "jpeg":
                    contentType = "image";
                    break;
                case "ico":
                    contentType = "icon";
                    break;
            }
        }

        return contentType;
    }

    /**
     * Extract the headerName from the given HTTP request.
     *
     * @param HTTPRequest - the HTTP request string
     * @param headerName  - the name of the header to extract
     * @return the value of the specified header if found. otherwise, an empty string
     */
    private String getHeaderValue(String HTTPRequest, String headerName) {
        String headerValue = "";
        String [] parts;

        if (HTTPRequest != null && !HTTPRequest.isEmpty()) {
            String headerPrefix = headerName + ":";

            parts = HTTPRequest.split(CRLF);

            for (String line : parts) {
                if (line.startsWith(headerPrefix)) {
                    headerValue = line.substring(headerPrefix.length()).trim();
                }
            }
        }

        return headerValue;
    }

    /**
     * Extracts query parameters from the URL of the HTTP request.
     *
     * @param HTTPRequest- the HTTP request string
     * @throws IOException if unable to extract and write to file.
     */
    private void extractQueryParams(String HTTPRequest) throws IOException {
        if (HTTPRequest != null && !HTTPRequest.isEmpty()) {
            String[] queryParts = HTTPRequest.split("HTTP");
            queryParts = queryParts[0].split("\\?");

            if (queryParts.length >= 2) {
                extractQueryParamsFromStr(queryParts[1]);
            }
        }
    }


    /**
     * Extracts query parameters from the provided string of parameters.
     *
     * @param strParamsFromUrl - a string of parameters from a URL
     */
    private void extractQueryParamsFromStr(String strParamsFromUrl) throws IOException {
        HashMap<String, String> queryParams = requestParams;
        String[] pairs = strParamsFromUrl.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");

            if (keyValue.length >=2){
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = "";

                if (keyValue.length == 2) {
                    value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);

                } else {
                    //For a params that includes "=" in the value.
                    StringBuilder connectValueParts = new StringBuilder();

                    for(int i= 1; i< keyValue.length; i++){
                        connectValueParts.append(URLDecoder.decode(keyValue[i], StandardCharsets.UTF_8)).append("=");
                    }

                    value = connectValueParts.substring(0, connectValueParts.length()-1);
                }

                queryParams.put(key, value);
            }
        }

        // Updates the params_info.html upon POST request.
        if (methodType.equalsIgnoreCase("post") || methodType.equalsIgnoreCase("get")) {
            buildParamsHtmlFile(queryParams);
        }
    }

    /**
     * Extracts the Content-Length from an HTTP POST request.
     *
     * @param httpRequest - the HTTP request string
     * @return the content length header value
     */
    private int extractContentLengthOnPostRequest(String httpRequest) {
        int contentLength = 0;
        String strContentLength = "";

        if (this.methodType.equalsIgnoreCase("post")) {
            strContentLength = getHeaderValue(httpRequest, "Content-Length");

            try {
                contentLength = Integer.parseInt(strContentLength);
            } catch (NumberFormatException e) {
                System.out.println("No Entity Body in Post Request");
            }
        }

        return contentLength;
    }

    /**
     * Checks if chunked encoding is requested by the client.
     *
     * @return true if chunked encoding is requested, false otherwise
     */
    public boolean isChunkedEncoding() {
        String chunkedHeader = getHeaderValue(request, "chunked");
        return ("yes").equalsIgnoreCase(chunkedHeader);
    }

    /**
     * Reads from an HTML file named params_info.html that is located in root directory listed in the Server Configuration
     * Given a POST/ GET request the method reads the html page, locates its parameters table and updates it accordingly
     * by writing a new table into the html file.
     *
     * @param paramsInfo - HashMap with all the key & value parameters.
     * @ throws IOException if unable to write to html file.
     */
    private void buildParamsHtmlFile(HashMap<String, String> paramsInfo) throws IOException {
        String filePath = currentConfig.getRoot().resolve("params_info.html").toString();
        StringBuilder htmlFileContent = readParamsInfoFile(filePath);
        int tableTagIndex = htmlFileContent.indexOf("</table>");

        htmlFileContent.insert(tableTagIndex, generateHtmlParamsTableCode(paramsInfo));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(htmlFileContent.toString());
        }
    }

    /**
     * Reads the content of a given file.
     *
     * @param paramsInfoFilePath - path to the file being read/
     * @return String - the file content.
     * @throws IOException if unable to read from file.
     */
    private StringBuilder readParamsInfoFile(String paramsInfoFilePath) throws IOException {
        StringBuilder htmlContent = new StringBuilder();
        String line = "";
        boolean isTableRowsAreReadingFromFile = false;

        try (BufferedReader fileReader = new BufferedReader(new FileReader(paramsInfoFilePath))) {
            while ((line = fileReader.readLine()) != null) {
                if (line.startsWith("<table>") || line.startsWith("</table>")) {
                    isTableRowsAreReadingFromFile = !isTableRowsAreReadingFromFile;
                    htmlContent.append(line).append('\n');
                }

                if (!line.startsWith("</table>") && !isTableRowsAreReadingFromFile) {
                    htmlContent.append(line).append('\n');
                }
            }
        }

        return htmlContent;
    }

    /**
     * Generate an html table with all the parameters from a hashmap.
     *
     * @param paramsInfo - HashMap that stores our key&Value parameters.
     * @return String - html code of a table with all the values.
     */
    private String generateHtmlParamsTableCode(HashMap<String, String> paramsInfo) {
        StringBuilder paramsBuilder = new StringBuilder(" <tr><th>Parameter Name</th><th>Parameter Value</th></tr>\n");

        if (!paramsInfo.isEmpty()) {
            for (HashMap.Entry<String, String> keyValPair : paramsInfo.entrySet()) {
                paramsBuilder.append("<tr><td>").append(keyValPair.getKey()).append("</td><td>").append(keyValPair.getValue()).append("</td></tr>\n");
            }
        }

        return paramsBuilder.toString();
    }

    /**
     * Validates the format of the incoming HTTP request.
     *
     * @param requestString the HTTP request string
     * @return true if the request format is valid, false otherwise
     */
    private boolean isValidHttpRequestFormat(String requestString) {
        if (requestString.isEmpty()) {
            return false;
        }

        if (!(requestString.contains("HTTP/"))) {
            return false;
        }

        String[] requestParts = requestString.split("/");
        if (requestParts.length < 3) {
            return false;
        }

        if (!requestParts[0].endsWith(" ")) {
            return false;
        }

        if (requestParts[0].startsWith(" ") || requestParts[0].equals("/")) {
            return false;
        }

        return true;
    }
}
