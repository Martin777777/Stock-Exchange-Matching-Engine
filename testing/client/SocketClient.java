import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketClient {

    private static String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 12345;

    private static long operationsTime = 0;


    public static void sendXmlToServer(String xmlData) {
        String threadInfo = "Thread ID: " + Thread.currentThread().getId() + ", Name: " + Thread.currentThread().getName();
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputStream = new DataInputStream(socket.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            // Log sending data with thread info
            System.out.println(threadInfo + " - Sending XML data to server.");

            // Send XML data to server
            outputStream.write(xmlData.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            socket.setSoTimeout(10000);
            // Read response from server and log with thread info
            long startTime = System.currentTimeMillis();
            String lengthLine = reader.readLine();
            int xmlLength = Integer.parseInt(lengthLine.trim());

            // Now, read exactly xmlLength characters of XML data
            char[] xmlChars = new char[xmlLength];
            int totalRead = 0;
            while (totalRead < xmlLength) {
                int charsRead = reader.read(xmlChars, totalRead, xmlLength - totalRead);
                if (charsRead == -1) { // EOF, should not happen if XML length is correct
                    System.out.println("Invalid XML length");
                }
                totalRead += charsRead;
            }
            String response = new String(xmlChars); // This will block until a response is received
            long endTime = System.currentTimeMillis();
            operationsTime += endTime - startTime;

            System.out.println(threadInfo + " - Server response: " + response);

        } catch (IOException e) {
            System.out.println(threadInfo + " - Error: ");
            e.printStackTrace();
        }
    }


    public static String readXmlFromResources(String fileName) {
        try (InputStream inputStream = new FileInputStream(fileName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String generateXml() {
        Random random = new Random();
        // Generate a random account ID in the range of 10000 to 99999
        int accountId = 10000 + random.nextInt(90000);

        // Assemble the XML text

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<create>\n" +
                "  <account id=\"" + accountId + "\" balance=\"" + 100 + "\"/>\n" +
                "  <symbol sym=\"" + "SYM" + "\">\n" +
                "    <account id=\"" + accountId + "\">" + 100 + "</account>\n" +
                "  </symbol>\n" +
                "</create>";
    }

    public static String generateSendData(String xmlData) {
        byte[] xmlBytes = xmlData.getBytes(StandardCharsets.UTF_8);
        int length = xmlBytes.length;

        return length + "\n" + xmlData;
    }


    public static void main(String[] args) {
        SERVER_HOST = "localhost";
        SERVER_PORT = 12345;

        int numberOfThreads = 1;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 21; i <= 31; i++) {
            String fileName = "../testxml/test" + i + ".xml";
            String xmlData = readXmlFromResources(fileName);

            if (xmlData == null) {
                System.err.println("Failed to read XML data from file: " + fileName);
                continue;
            }

            // log
            System.out.println("Thread will send data from: " + fileName);
            executor.submit(() -> sendXmlToServer(generateSendData(xmlData)));
        }


//        for (int i = 0; i < 1000; i++) {
//            executor.submit(() -> sendXmlToServer(generateSendData(generateXml())));
//        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            System.out.println(operationsTime);
            executor.shutdownNow();
        }));
    }
}

