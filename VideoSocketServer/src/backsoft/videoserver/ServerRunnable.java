package backsoft.videoserver;

import backsoft.utils.*;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;

import static backsoft.utils.CommonPhrases.*;
import static backsoft.utils.CommonPhrases.SIGNAL.*;

public class ServerRunnable implements Runnable {



    private int port;
    private Controller controller;
    private ServerSocket serverSocket;
    private ArrayList<Socket> currentSockets = new ArrayList<>();
    private Map<Socket, Thread> currentThreads = new HashMap<>();
    private Map<Socket, SocketTask> currentTasks = new HashMap<>();
    int SERVER_TIMEOUT = 6000;

    class SocketTask implements Runnable {

        Socket clientSocket;
        ClientStreamWindow streamWindow;
        Stage clientWindow;
        DataInputStream in;
        DataOutputStream out;

        SocketTask(Socket client) throws IOException {
            super();
            clientSocket = client;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            Pair<Parent, ClientStreamWindow> fxml =Loader.loadFXML(
                    ServerRunnable.class.getResource("clientStreamWindow.fxml"));

            streamWindow = fxml.getTwo();
            Platform.runLater(()-> {
                clientWindow = new Stage();
                clientWindow.setOnCloseRequest(event -> {
                    AlertHandler.makeInfo("не могу закрыть окно. Клиент ещё подключен.", clientWindow);
                    event.consume();
                });
                clientWindow.setTitle("Стрим Клиента " + client.getInetAddress().toString());
                Loader.openInAWindow(
                        clientWindow,
                        new Scene(fxml.getOne()),
                        true);
            });
        }

        private void handleVideoStreamReceive() throws IOException {

            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " передаёт видео...");

        }

        private void handleImageReceive() throws IOException {

            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " передаёт изображение...");

            String filename = in.readUTF();
            String hash = in.readUTF();
            int imageHASH = Integer.parseInt(hash);

            byte[] bytes = Streamer.readBytesFromBase64(byteFileSignal.get(STOP), in, controller.chunksProperty());

            controller.writeToConsole("От клиента "
                    + clientSocket.getRemoteSocketAddress()
                    + " получено изображение - " + filename);

            if (Arrays.hashCode(bytes) == imageHASH){
                out.writeUTF(byteFileSignal.get(CORRECT));
                out.flush();
                controller.writeToConsole("файл ё" + filename + ": пришел без потерь");
            } else {
                out.writeUTF(byteFileSignal.get(MISTAKE));
                out.flush();
                controller.writeToConsole(" пришел битым :(");
            }

            FileHandler.saveToFile("temp" + File.separator + filename, bytes);
            BufferedImage bImage = Loader.convertToBuffImage(bytes);

            if (bImage != null) streamWindow.showImage(bImage, filename);
        }

        private void handleQuit() {
            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " запрашивает выход...");
            removeClient(clientSocket);
            currentSockets.remove(clientSocket);
        }

        @Override
        public void run() {

            while (!clientSocket.isClosed()) {
                try {
                    String command = in.readUTF();
                    if (command.equals(byteFileSignal.get(START))) handleImageReceive();
                    else if (command.equals(videoSignal.get(START))) handleVideoStreamReceive();
                    else if (command.equals(system.get(BYEBYE))) handleQuit();
                } catch (SocketException | EOFException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        clientSocket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    AlertHandler.makeInfo(
                            "Ошибка при работе с клиентом " +
                                    clientSocket.getRemoteSocketAddress()+
                                    ". Соединение остановлено.",
                            null);
                }
            }
        }

        public void close(){
            Platform.runLater(()-> clientWindow.close());
            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " отсоединился");
        }
    }

    ServerRunnable(int port, Controller controller){
        this.port = port;
        this.controller = controller;
    }

   void stop() {
       try {
           for (Thread th : currentThreads.values()){
               th.interrupt();
           }
           for (int i = 0; i < currentSockets.size(); i++) removeClient(currentSockets.get(0));
           if (serverSocket != null) serverSocket.close();
       } catch (IOException e) {
           AlertHandler.makeError("Сервер не остановлен. Ошибка:\n"
                   +e.getLocalizedMessage(),null);
           e.printStackTrace();
       }
   }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(SERVER_TIMEOUT);
            controller.setServerWorking(true);
            controller.writeToConsole("Ожидание подключения новых клиентов...");

            while (!serverSocket.isClosed()) {

                Socket clientSocket = serverSocket.accept();
                addClient(clientSocket);
            }
        } catch (SocketException ignored) {
        } catch (SocketTimeoutException te){
            controller.writeToConsole("Время ожидания сервера ("+ SERVER_TIMEOUT/1000 +" сек) истекло");
        }
        catch (IOException e) {
            AlertHandler.makeError("Ошибка при работе сервера:\n"+e.getLocalizedMessage(), null);
        }
        controller.setServerWorking(false);
    }

    private void addClient(Socket client) throws IOException {

        currentSockets.add(client);
        controller.putClientToTable(client);
        if (client.isConnected()) {
            currentTasks.put(client, new SocketTask(client));
            serverSocket.setSoTimeout(0);

            currentThreads.put(client, new Thread(currentTasks.get(client)));
            currentThreads.get(client).start();
            controller.writeToConsole("Клиент "
                    + client.getRemoteSocketAddress()
                    + " подключился!");
        }
    }

    private void removeClient(Socket client) {

        try {
            controller.removeClientFromTable(client);
            if (!client.isClosed() && client.isConnected()) {
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                out.writeUTF(system.get(BYEBYE));
                out.flush();
                currentTasks.get(client).close();
                controller.writeToConsole("Клиент " + client.getRemoteSocketAddress() + " отключён");
                currentSockets.remove(client);
            }
        }catch (SocketException se){
            AlertHandler.makeError(
                    se.getLocalizedMessage()
                            +"\nПри установке Timeout у ServerSocket", null);
            se.printStackTrace();
        } catch (IOException e){
            AlertHandler.makeError(e.getLocalizedMessage(), null);
        }

        currentTasks.remove(client);
        if (currentThreads.get(client).isAlive() )
            currentThreads.get(client).interrupt();

        currentThreads.remove(client);
    }
}
