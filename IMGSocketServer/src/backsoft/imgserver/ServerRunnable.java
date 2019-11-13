package backsoft.imgserver;

import javafx.application.Platform;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;

public class ServerRunnable implements Runnable {

    private int port;
    private Controller controller;
    private ServerSocket serverSocket;
    private ArrayList<Socket> currentSockets = new ArrayList<>();

    class SocketTask implements Runnable {

        Socket clientSocket;
        String command;

        SocketTask(Socket client){
            super();
            clientSocket = client;
        }

        @Override
        public void run() {

            while (!clientSocket.isClosed()) {
                try {
                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    command = in.readUTF();
                    if (command.equals("image")) {
                        String filename = in.readUTF();
                        BufferedImage bImage = readImageFromClient(clientSocket, filename, in);
                        controller.setNewMessageToConsole("ОтКлиента "
                                +(currentSockets.indexOf(clientSocket)+1)
                                +" получено изображение - \n" + filename);
                        Platform.runLater(() -> Loader.showImageInAWindow(filename, bImage));

                        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                        out.writeUTF("gotit");
                        out.flush();
                    }

                    if (command.equals("quit")) {
                        int num = currentSockets.indexOf(clientSocket)+1;
                        controller.setNewMessageToConsole("Клиент " + num + " запрашивает выход...");

                        currentSockets.remove(clientSocket);
                        clientSocket.close();
                        controller.setNewMessageToConsole("Клиент " + num + " отключён");
                    }
                } catch (IOException e) {
                    Platform.runLater(() ->
                            AlertHandler.makeError(
                                    "Ошибка при работе с клиентом. Соединение остановлено.\n"
                                            + e.getLocalizedMessage(),
                                    Loader.getStage()
                            ));
                    try {
                        currentSockets.remove(clientSocket);
                        clientSocket.close();
                    } catch (IOException ex) {
                        Platform.runLater(() -> AlertHandler.makeError(e.getLocalizedMessage(), Loader.getStage()));
                    }
                }
            }

            currentSockets.remove(clientSocket);
            controller.setNewMessageToConsole("Соединение с клиентом закрыто");
        }
    }

    ServerRunnable(int port, Controller controller){
        this.port = port;
        this.controller = controller;
    }

   boolean stop() {
       try {
           for (Socket sock : currentSockets) {
               if (sock.isConnected()) {
                   DataOutputStream out = new DataOutputStream(sock.getOutputStream());
                   out.writeUTF("close");
                   out.flush();
                   sock.close();
               }
           }
           currentSockets.clear();
           Platform.runLater(()-> {
               try {
                   serverSocket.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           });
           return true;
       } catch (IOException e) {
           Platform.runLater(() -> AlertHandler.makeError("Сервер не остановлен. Ошибка:\n"
                   + e.getLocalizedMessage(), Loader.getStage()));
           return false;
       }
   }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(180000);
            controller.setServerWorking(true);
            controller.setNewMessageToConsole("Ожидание подключения новых клиентов...");

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                currentSockets.add(clientSocket);

                if (clientSocket.isConnected()) {
                    controller.setNewMessageToConsole("Клиент "+currentSockets.size()+" подключился!");
                    Thread thread = new Thread(new SocketTask(clientSocket));
                    thread.start();
                }
            }
            controller.setServerWorking(false);

        } catch (SocketException e){
            Platform.runLater(()->controller.setNewMessageToConsole(
                    "Сервер остановлен"));
        }
        catch (IOException e) {
            controller.setServerWorking(false);
            e.printStackTrace();
        }
    }

    private static File saveAsFile(String filepath, byte[] fileByte) throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(fileByte);
        File imageFile = new File(filepath);
        OutputStream out = new FileOutputStream(imageFile);
        byte[] bytes = new byte[16*1024];
        int count;
        while ((count = bais.read(bytes)) > 0) {
            out.write(bytes, 0, count);
        }
        return imageFile;
    }


    private BufferedImage readImageFromClient(Socket clientSocket, String filename, DataInputStream in) throws IOException {
        controller.setNewMessageToConsole("Клиент"+(currentSockets.indexOf(clientSocket)+1)+" передаёт изображение...");
        File imageFile = new File("images"+File.separator+filename);
        if (!imageFile.exists()) imageFile.getParentFile().mkdir();

        StringBuilder imgAsString = new StringBuilder();
        String utf = in.readUTF();
        while (!utf.equals("image-end")){
            imgAsString.append(utf);
            utf = in.readUTF();
        }
        byte[] fileByte = Base64.getDecoder().decode(imgAsString.toString());
        File file = saveAsFile(imageFile.getPath(), fileByte);
        return  readBuffImage(file);
    }

    private static BufferedImage readBuffImage(File imageFile){

        BufferedImage bImageFromConvert = null;
        try {
            bImageFromConvert = ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bImageFromConvert;
    }
}
