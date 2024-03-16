TCP-Based Multithreaded HTTP Web Server


<img width="187" alt="Screenshot 2024-03-02 at 21 00 10" src="https://github.com/matanyaniv8/Web-Server/assets/95882684/6a64dfad-7832-413b-818e-a580feb64341">

Introduction:
This project is a Multithreaded HTTP server implemented in Java that supports handling GET, POST, TRACE, and HEAD HTTP requests.
The server reads its configuration from a config.ini file, which specifies the port number, the maximum number of concurrent threads, the root directory, and the default page.
The server processes incoming requests and generates responses accordingly.


Supported HTTP Methods:
- GET - Retrieves data from the server.
- POST - Sends data to the server for processing.
- TRACE - Echoes back the received request for testing purposes.
- HEAD - Requests the headers of a resource without requesting the resource itself.

Supported Responses:
- 200 OK - The request was successful, and the requested object is included later in this message. This is also the response for a successful TRACE request.
- 404 Not Found - The requested file was not found in the server’s root directory.
- 501 Not Implemented - The method specified in the request is not known or supported by our server.
- 400 Bad Request - The request’s format is invalid (e.g., the method is not specified, it’s not an “HTTP/” request, or it does not include the three parts of the template: “method/ path HTTP/1.0”)
- 500 Internal Server Error - The server crashes while reading from or writing to the client.

Multithreading:
The server is designed as a multithreaded application to serve multiple clients simultaneously.
The maximum number of threads is determined by the max_threads parameter in the config.ini file.


Setup:
- uncompressed the server root directory and place it in your computer's "user.home" directory.
- Download the repository and open a terminal/CMD on the repository directory:
  - Compile the server using the command on the bash file -> ./compile.sh.
  - Run the server using the run.sh bash file using the command -> ./run.sh.

Explanation on the classes:
WebServer class:
This class represents the main entry point for the TCP multi-threaded web server. 
It handles incoming client connections, creates a new thread for each client request, and delegates request handling to the `ClientHandler` class. It also manages the server configuration and shutdown procedures.

ClientHandler class:
This class is responsible for handling individual client requests. 
It parses incoming HTTP requests, processes different HTTP methods (GET, POST, HEAD, TRACE), generates appropriate HTTP responses, and interacts with the server based on the request.

HTTPRequest class:
This class parses and extracts information from incoming HTTP requests. 
It handles the parsing of request headers, extraction of parameters, and management of request content. 
It also updates params_info.html file to reflect parameter changes in case the user included one in the request.

HTTPResponse class:
This class is responsible for generating and sending HTTP responses back to clients. 
It handles the formatting of HTTP headers and response bodies, including support for chunked encoding. 
It also manages error responses and handles TRACE requests.


Design Overview:
The implemented server follows a multi-threaded architecture, allowing it to handle multiple client connections simultaneously. 
The `WebServer` class listens for incoming client connections and delegates request handling to individual `ClientHandler` instances. 
Each `ClientHandler` instance processes an incoming request, extracts necessary information, and generates appropriate responses. 
The server utilizes a thread pool to manage concurrent request handling efficiently. 
The design emphasizes modularity and separation of concerns, with each class responsible for a specific aspect of the server's functionality.
