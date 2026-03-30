package ru.wert.tubus.chogori.chat.socketwork.socketservice;

import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.client.retrofit.AppProperties;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static ru.wert.tubus.winform.statics.WinformStatic.TEST_VERSION;

@Slf4j
public class SocketConnectionManager {

    // Адрес сервера, полученный из настроек приложения
    private static final String SERVER_ADDRESS = AppProperties.getInstance().getIpAddress();
    // Порт, на котором работает сервер
    private static final int PORT = TEST_VERSION ? 9081 : 8081;
    // Таймаут для установления соединения с сервером (в миллисекундах)
    private static final int CONNECT_TIMEOUT_MS = 5000;
    // Таймаут для чтения данных из сокета (в миллисекундах)
    private static final int SOCKET_TIMEOUT_MS = 30000;

    // Сокет для соединения с сервером
    private Socket socket;
    // Поток для отправки данных на сервер
    private PrintWriter out;
    // Поток для чтения данных от сервера
    private BufferedReader in;

    // Метод для установления соединения с сервером
    public void connect() throws IOException {
        // Создание нового сокета
        socket = new Socket();
        // Установление соединения с сервером с указанием таймаута
        socket.connect(new InetSocketAddress(SERVER_ADDRESS, PORT), CONNECT_TIMEOUT_MS);
        // Установка таймаута для чтения данных из сокета
        socket.setSoTimeout(SOCKET_TIMEOUT_MS);

        // Инициализация потока для отправки данных на сервер с использованием UTF-8
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        // Инициализация потока для чтения данных от сервера с использованием UTF-8
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        // Логирование успешного подключения
        log.info("Подключение к серверу {}:{} установлено.", SERVER_ADDRESS, PORT);
    }

    // Метод для закрытия соединения и освобождения ресурсов
    public void close() {
        try {
            // Закрытие потока отправки данных, если он был открыт
            if (out != null) out.close();
            // Закрытие потока чтения данных, если он был открыт
            if (in != null) in.close();
            // Закрытие сокета, если он был открыт
            if (socket != null) socket.close();
            // Логирование успешного закрытия ресурсов
            log.info("Ресурсы сокета закрыты.");
        } catch (IOException e) {
            // Логирование ошибки при закрытии ресурсов
            log.error("Ошибка при закрытии ресурсов: {}", e.getMessage());
        }
    }

    // Метод для проверки состояния соединения
    public boolean isConnected() {
        // Проверка, что сокет не равен null, подключен и не закрыт
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // Метод для получения потока отправки данных
    public PrintWriter getOut() {
        return out;
    }

    // Метод для получения потока чтения данных
    public BufferedReader getIn() {
        return in;
    }
}
