// importing the necessary classes
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class HttpClient {

    private static String serverUrl;

    public static void main(String[] args) {

        // requesting IP address and port from the user
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the server IP address: ");
        String ipAddress = scanner.nextLine().trim();

        System.out.print("Enter the server port: ");
        String port = scanner.nextLine().trim();

        // constructing the server URL using the input values for ip address and port
        serverUrl = String.format("http://%s:%s", ipAddress, port);

        // trying to connect to the server via /getbalance endpoint
        if (checkServerConnection()) {
            System.out.println("Connected to the server successfully.\n");

            // calling /getbalance 20 times
            for (int i = 0; i < 20; i++) {
                performGetBalance();
            }
            // calling /getlogs once
            callGetLogs();
        } else {
            System.out.println("Client could not connect to the server. Please check your IP address and port.");
        }

        scanner.close();
    }

    // verifying the connection by making a single call to /getbalance
    private static boolean checkServerConnection() {
        try {

            // complete url to which the GET request will be sent
            URL url = new URL(serverUrl + "/getbalance");

            // setting up a connection to the server
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            // waits for maximum 3 seconds while establishing the connection
            connection.setConnectTimeout(2000); 

            // waits for maximum 3 seconds for a response after a connection has been established
            connection.setReadTimeout(2000); 

            int responseCode = connection.getResponseCode();

            // since these status codes are the ones mentioned in assignment instructions we treat them as a positive response
            return responseCode == 200 || responseCode == 403 || responseCode == 500 || responseCode == 408;

        } catch (Exception e) {
            System.err.println("Error happened while trying to connect to the server: " + e.getMessage());
            return false;
        }
    }

    // method for calling /getbalance
    private static void performGetBalance() {
        try {
            URL url = new URL(serverUrl + "/getbalance");

            // opening a connection to url mentioned above for the /getbalance endpoint using the server url
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();


            connection.setRequestMethod("GET");

            // waits for maximum 1 second while establishing the connection
            connection.setConnectTimeout(1000);

            // waits for maximum 1 second for a response after a connection has been established
            connection.setReadTimeout(1000);

            int responseCode = connection.getResponseCode();

            // handling the response code
            switch (responseCode) {

                // HTTP 200 -> OK
                case 200:
                    System.out.println("GET /getbalance Response Code: " + responseCode);
                    break;

                // HTTP 403 -> Forbidden    
                case 403:
                    System.out.println("GET /getbalance Response Code: " + responseCode + " - Forbidden");
                    break;

                // HTTP 500 -> Internal Server Error
                case 500:
                    System.out.println("GET /getbalance Response Code: " + responseCode + " - Internal server error.");
                    break;
                
                // HTTP 408 -> Request Timeout
                case 408:
                    System.out.println("GET /getbalance Response Code: " + responseCode + " - Request timed out.");
                    break;
                
                // For any other unexpected response codes, we print a message indicating the responseCode as well
                default:
                    System.out.println("GET /getbalance Response Code: " + responseCode + " - Unexpected response.");
                    break;
            }

        } catch (IOException e) {
            System.err.println("Error while calling /getbalance: " + e.getMessage());
        }
    }

    // method for calling /getlogs
    private static void callGetLogs() {
        try {

            // creating a URL pointing to the /getlogs endpoint
            URL url = new URL(serverUrl + "/getlogs");

            // opening a connection to url mentioned above
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            // waits for maximum 3 seconds while establishing the connection
            connection.setConnectTimeout(3000); 

            // waits for maximum 3 seconds for a response after a connection has been established
            connection.setReadTimeout(3000);

            // getting the HTTP response code from the server
            int responseCode = connection.getResponseCode();

            // HTTP 200 -> OK
            if (responseCode == 200) {

                // opening a BufferedReader to read the response from the server
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                // we'll use StringBuilder to store the whole jsonResponse
                StringBuilder jsonResponse = new StringBuilder();
                String inputLine;

                // reading the response line by line and adding it to the jsonResponse
                while ((inputLine = in.readLine()) != null) {
                    jsonResponse.append(inputLine);
                }

                // closing the BufferedReader after reading the input
                in.close();

                // we'll parse the JSON response from scratch trying not use customized library
                String jsonString = jsonResponse.toString();

                // removing the leading '[' and trailing ']' brackets
                jsonString = jsonString.substring(1, jsonString.length() - 1);

                // Split the string into individual log entries
                // we split where }{ appears between objects
                String[] logEntries = jsonString.split("\\},\\{");

                Set<String> uniqueLogs = new HashSet<>();

                System.out.println("\nLogs:\n");

                // we loop through each log entry, format, then print it
                for (String logEntry : logEntries) {

                    // formatting the curly braces to ensure correct JSON structure
                    logEntry = logEntry.replaceAll("^\\{", "{").replaceAll("\\}$", "}");

                    // adding log entries to the Set (unique ones will be added)
                    if (uniqueLogs.add(logEntry)) {
                        System.out.println(logEntry); // Print each unique log entry on a new line
                    }
                }
            } else {
                // if HTTP response code is not 200, we print a message
                System.out.println("Failed to load logs. Status code: " + responseCode);
            }

        } catch (Exception e) {
            // printing a meesage for any kind of error message
            System.err.println("Error while calling /getlogs: " + e.getMessage());
        }
    }
}
