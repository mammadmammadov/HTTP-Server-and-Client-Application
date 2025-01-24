// importing the necessary classes
import java.io.OutputStream;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.InetSocketAddress;
import java.util.List;
import java.time.LocalDateTime;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.util.regex.Pattern;

public class Server {

    // Logs are stored to "logs.log" file
    private static final String log_file = "assignment-1/logs.log";
    private static final String IPV4_regex =
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    private static  Pattern IPV4_pattern = Pattern.compile(IPV4_regex);

    public static void main(String[] args) throws IOException {

        // default port will be set to 8080 in case there is no argument provided
        int port = 8080;

        // default IP will be "0.0.0.0" binding to all available IP addresses
        String ipAddress = "0.0.0.0";

        // checking whether the user provided a valid IP and custom port number
        if (args.length > 0) {
            ipAddress = args[0]; // Taking the IP address from the first argument

            // if (!isValidIP(ipAddress)) {
            //     System.out.println("Invalid IP address provided. Using default IP address: " + ipAddress);
            //     ipAddress = "0.0.0.0"; // back to default
            // }

            if (args.length > 1) {
                String portArg = args[1];
                if (isValidPort(portArg)) {
                    port = Integer.parseInt(portArg);
                } else {
                    System.out.println("Invalid port number provided. Default port " + port + " will be used.");
                }
            }
        }

        // initializing an HTTP server accepting incoming requests on the provided IP address and port (provided in arguments or the default one)
        HttpServer server = HttpServer.create(new InetSocketAddress(ipAddress, port), 0);

        // setting up routes in the server for responding accordingly to different requests
        server.createContext("/getbalance", new BalanceHandler());
        server.createContext("/getlogs", new LogsHandler());

        // finally, the server is ready to operate
        server.start();

        System.out.println("Server started on " + ipAddress + ":" + port);
    }

    static class BalanceHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            // getting the combination of an IP address and port number from which the client made the request
            InetSocketAddress remoteAddress = exchange.getRemoteAddress();

            // getting the IP address of the client in human-readable string format
            String clientIP = remoteAddress.getAddress().getHostAddress();

            // getting the current timestamp
            LocalDateTime time = LocalDateTime.now();

            // initializing the variable for later assigning the status codes
            int resultCode;

            // generating a random number between 0 and 99
            int chance = (int)(Math.random() * 100);

            // we check if it is between 0 and 19 (both inclusive) for 20% probability of timeout
            if (chance < 20) {
                // HTTP status code for Request Timeout
                resultCode = 408;
            }

            // we check if our random number is between 20 and 39 (both inclusive) for 20% probability of status code 403
            else if (chance < 40) {
                resultCode = 403; // forbidden
                sendResponse(exchange, resultCode, "403 Forbidden: Access Denied");
            }

            // we check if our random numbers is between 40 and 49 (both inclusive) for 10% probability of status code 500
            else if (chance < 50) {
                resultCode = 500; // Internal Server Error
                sendResponse(exchange, resultCode, "500: Internal Server Error");
            }

            // all the remaining cases constitute 50% probability returning OK by the server            
            else {

                resultCode = 200; // OK

                // path of the html file containing fake information
                String htmlFilePath = "C:\\Users\\gulma\\Desktop\\assignments-1-and-2-mammadmammadov\\assignment-1\\response.html";

                String response = "";
                try {

                    // reading the entire content of an HTML file as a byte array and converting it into a String
                    response = new String(Files.readAllBytes(Paths.get(htmlFilePath)), StandardCharsets.UTF_8);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                sendResponse(exchange, resultCode, response);
            }

            // logging each time a request is made
            logRequest(time, clientIP, resultCode);
        }
    }

    static class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // reading all the logs in the file and storing them in a list where each line represents one log entry
            List<String> logs = Files.readAllLines(Paths.get(log_file));

            // JSON response will be constructed as a String
            StringBuilder jsonBuilder = new StringBuilder();

            jsonBuilder.append("["); // start of the construction of JSON array

            // looping through each log entry in the list
            for (int i = 0; i < logs.size(); i++) {

                // splitting each log etnry by " - " to break it into timestamp, IP, and outcome
                String[] logComponents = logs.get(i).split(" - ");

                if (logComponents.length == 3) {
                    jsonBuilder.append(String.format
                    ("{\"timestamp\":\"%s\",\"ip\":\"%s\",\"outcome\":\"%s\"}", 
                    logComponents[0], // timestamp
                    logComponents[1], // IP address
                    logComponents[2].split(": ")[1])) // outcome
                    .append(i < logs.size() - 1 ? "," : ""); // appending comma between entries
                }
            }

            jsonBuilder.append("]"); // completion of the contruction of JSON array

            // preparing the JSON response as a String
            String jsonResponse = jsonBuilder.toString();

            // we indicate that the content-type is JSON
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

            // here we send the response body
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        }
    }

    private static boolean isValidPort(String port) {
        try {
            // parsing port which is taken as String from user into Integer  
            int portNum = Integer.parseInt(port); 
            return portNum > 0 && portNum <= 65535; // valid port range
        } catch (Exception e) {
            return false; // invalid integer
        }
    }

    // Method to check if the input is a valid IPv4 address
    public static boolean isValidIP(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        return IPV4_pattern.matcher(ipAddress).matches();
    }

    // sending a HTTP response back to a client 
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        try (OutputStream os = exchange.getResponseBody()) {
            // sending the appropriate HTTP headers, including the status code and the length of response
            exchange.sendResponseHeaders(statusCode, response.length());

            // converting the response string into a byte array and writing it to the output stream
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    // logging details of an HTTP request received by the server
    private static void logRequest(LocalDateTime time, String clientIP, int resultCode) {
        String log = String.format("%s - %s - Status Code: %d%n", time, clientIP, resultCode);
        try {
            // writing the log to "logs.log" file
            // if the file does not exist, it will be created, otherwise appended to the end of the existing file
            Files.write(Paths.get(log_file), log.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}