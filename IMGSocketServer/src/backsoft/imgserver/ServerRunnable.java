package backsoft.imgserver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerRunnable implements Runnable {

    private int port;
    private Controller controller;
    private ServerSocket serverSocket;

    ServerRunnable(int port, Controller controller){
        this.port = port;
        this.controller = controller;
    }

    void stopServer(){

        try {
            serverSocket.close();
        } catch (IOException e) {
            AlertHandler.makeError("Ошибка при закрытии сервера\n"+e.getLocalizedMessage());
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(180000);
            controller.setServerWorking(true);
            controller.setNewMessageToConsole("Сервер Запущен!");
            controller.setNewMessageToConsole("Ожидание подключения новых клиентов...");
            while (!serverSocket.isClosed()){
                Socket clientSocket = serverSocket.accept();

                if (clientSocket.isConnected()) {
                    controller.setNewMessageToConsole("Клиент Подключился!");
                    Thread clientThread = new Thread(() -> listenToAClient(clientSocket));
                    clientThread.start();
                }
            }
            controller.setServerWorking(false);
            serverSocket.close();

        } catch (IOException e) {
            controller.setServerWorking(false);
            e.printStackTrace();
        }
    }

    public static BufferedImage readBufferedImage(byte[] imageInByte){
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(imageInByte);
            return ImageIO.read(byteStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void listenToAClient(Socket clientSocket){
        try {

            while (!clientSocket.isClosed()){

                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                String command = dis.readUTF();
                if (command.equals("image")){
                    controller.setNewMessageToConsole("Клиент передаёт изображение...");
                    int len = dis.readInt();
                    byte[] imgAsByte = new byte[len];
                    BufferedImage bImage = ImageIO.read(new ByteArrayInputStream(imgAsByte));
                    controller.askToDraw(bImage);
                }
                if (command.equals("quit")){
                    controller.setNewMessageToConsole("Клиент запрашивает выход...");
                    clientSocket.close();
                }
                dis.close();
            }
            controller.setNewMessageToConsole("Соединение с клиентом закрыто");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
