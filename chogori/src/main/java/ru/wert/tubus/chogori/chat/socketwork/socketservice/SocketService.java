package ru.wert.tubus.chogori.chat.socketwork.socketservice;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import ru.wert.tubus.chogori.chat.socketwork.ServiceMessaging;
import ru.wert.tubus.client.entity.models.Message;
import ru.wert.tubus.winform.statics.WinformStatic;

import java.io.IOException;

import static ru.wert.tubus.chogori.setteings.ChogoriSettings.CH_CURRENT_USER;

/**
 * Сервис для управления сокет-соединением с чат-сервером.
 * Обеспечивает подключение, переподключение и обмен сообщениями.
 */
@Slf4j
public class SocketService {

    /** Свойство для отслеживания доступности сервера */
    public static final BooleanProperty CHAT_SERVER_AVAILABLE_PROPERTY = new SimpleBooleanProperty(true);

    // Настройки переподключения
    private static final int INITIAL_RECONNECT_DELAY_MS = 1000;
    private static final int MAX_RECONNECT_DELAY_MS = 30000;
    private static int reconnectAttempts = 0;
    private static volatile boolean running = true;
    private static volatile boolean isReconnecting = false;

    // Компоненты для работы с соединением
    private static final SocketConnectionManager connectionManager = new SocketConnectionManager();
    private static MessageReceiver messageReceiver;
    private static MessageSender messageSender;

    /** Сервис для работы в фоновом потоке */
    private static final Service<Void> socketService = new Service<Void>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    while (running) {
                        try {
                            connectToServer();
                            waitWhileConnected();
                        } catch (Exception e) {
                            handleConnectionError(e);
                        } finally {
                            cleanupAndScheduleReconnect();
                        }
                    }
                    log.info("Сервис сокета остановлен.");
                    return null;
                }
            };
        }
    };

    /**
     * Обновляет статус доступности сервера в UI-потоке.
     * @param isAvailable true если сервер доступен, false в противном случае
     */
    public static void updateServerStatus(boolean isAvailable) {
        Platform.runLater(() -> {
            if (CHAT_SERVER_AVAILABLE_PROPERTY.get() != isAvailable) {
                CHAT_SERVER_AVAILABLE_PROPERTY.set(isAvailable);
                log.info("Статус сервера изменен: {}", isAvailable ? "доступен" : "недоступен");
            }
        });
    }

    /**
     * Инициирует переподключение к серверу.
     * Выполняется в UI-потоке для безопасности.
     */
    public static void reconnect() {
        if (isReconnecting) return;

        Platform.runLater(() -> {
            isReconnecting = true;
            try {
                log.info("Инициировано переподключение...");
                stopComponents();
                Thread.sleep(1000); // Даем время на завершение
                startService();
            } catch (Exception e) {
                log.error("Ошибка при переподключении: {}", e.getMessage());
            } finally {
                isReconnecting = false;
            }
        });
    }

    /** Запускает сервис сокета */
    public static void start() {
        if (!socketService.isRunning()
                && WinformStatic.USE_CHAT_SERVER) { //Если чат-сервис нужен
            socketService.restart();
        }
    }

    /** Останавливает сервис сокета */
    public static void stop() {
        Platform.runLater(() -> {
            running = false;
            ServiceMessaging.sendMessageUserOut();
            stopComponents();
            socketService.cancel();
            log.info("Сервис сокета завершает работу...");
        });
    }

    /**
     * Отправляет сообщение на сервер.
     * @param message сообщение для отправки
     */
    public static void sendMessage(Message message) {
        if (messageSender != null) {
            messageSender.sendMessage(message);
        } else {
            log.error("Не удалось отправить сообщение {}, т.к. messageSender = null", message.toUsefulString());
        }
    }

    // Приватные вспомогательные методы

    private static void connectToServer() throws IOException {
        log.info("Попытка подключения к серверу...");
        connectionManager.connect();

        messageReceiver = new MessageReceiver(connectionManager.getIn());
        messageSender = new MessageSender(connectionManager.getOut());

        messageReceiver.start();
        messageSender.start();

        ServiceMessaging.sendMessageUserIn(CH_CURRENT_USER.getId());
        log.info("Сокет успешно подключен, потоки запущены.");
        CHAT_SERVER_AVAILABLE_PROPERTY.set(true);
        reconnectAttempts = 0; // Сброс счетчика при успешном подключении
    }

    private static void waitWhileConnected() throws InterruptedException {
        while (running && connectionManager.isConnected()) {
            Thread.sleep(1000);
        }
    }

    private static void handleConnectionError(Exception e) {
        if (e instanceof IOException) {
            log.error("Ошибка подключения к серверу: {}", e.getMessage());
        } else {
            log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
        }
        CHAT_SERVER_AVAILABLE_PROPERTY.set(false);
    }

    private static void cleanupAndScheduleReconnect() throws InterruptedException {
        connectionManager.close();
        if (running) {
            int delay = calculateReconnectDelay();
            log.info("Попытка переподключения через {} мс...", delay);
            Thread.sleep(delay);
        }
    }

    private static int calculateReconnectDelay() {
        reconnectAttempts++;
        return Math.min(INITIAL_RECONNECT_DELAY_MS * (1 << Math.min(reconnectAttempts, 10)), MAX_RECONNECT_DELAY_MS);
    }

    private static void stopComponents() {
        if (messageReceiver != null) {
            messageReceiver.stop();
            messageReceiver = null;
        }
        if (messageSender != null) {
            messageSender.stop();
            messageSender = null;
        }
        connectionManager.close();
    }

    private static void startService() {
        if (socketService.isRunning()) {
            socketService.cancel();
        }
        socketService.restart();
    }
}

