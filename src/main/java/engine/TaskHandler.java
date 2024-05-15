package engine;

import command.CreateCommand;
import command.TransactionsCommand;
import utils.XMLParser;
import utils.XMLResponseGenerator;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TaskHandler implements Runnable {
    Socket clientSocket;
    public TaskHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String response = "";

            String lengthLine = reader.readLine();
            int xmlLength = Integer.parseInt(lengthLine.trim());

            // Now, read exactly xmlLength characters of XML data
            char[] xmlChars = new char[xmlLength];
            int totalRead = 0;
            while (totalRead < xmlLength) {
                int charsRead = reader.read(xmlChars, totalRead, xmlLength - totalRead);
                if (charsRead == -1) { // EOF, should not happen if XML length is correct
                    response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><results><error>Invalid XML Length</error></results>";;
                }
                totalRead += charsRead;
            }
            String message = new String(xmlChars);
            System.out.println(message);

            Object command;

            try {
                command = XMLParser.parse(message);

                if (command instanceof CreateCommand) {
                    CreateExecutor createExecutor = new CreateExecutor((CreateCommand) command);
                    response = createExecutor.execute();
                } else if (command instanceof TransactionsCommand) {
                    TransactionsExecutor transactionsExecutor = new TransactionsExecutor((TransactionsCommand) command);
                    response = transactionsExecutor.execute();
                }
            } catch (XMLStreamException | JAXBException e) {
                response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><results><error>Invalid XML format or structure</error></results>";
                e.printStackTrace();
            }

            outputStream.write(XMLResponseGenerator.generateResponseString(response).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
