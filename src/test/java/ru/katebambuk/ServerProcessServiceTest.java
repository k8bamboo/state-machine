package ru.katebambuk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.katebambuk.dictionary.State;
import ru.katebambuk.service.ServerProcessService;
import ru.katebambuk.util.PropertyResolver;

import java.lang.reflect.Field;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static ru.katebambuk.util.PropertyResolver.getProcessingDuration;
import static ru.katebambuk.util.PropertyResolver.getSendingDuration;


class ServerProcessServiceTest {

    private static ServerProcessService serverProcessService;
    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    private static int processDelay;
    private static int sendDelay;
    private static int generalTimeout;


    @BeforeAll
    static void setUp() {
        PropertyResolver.loadProperties();

        processDelay = getProcessingDuration() / 3;
        sendDelay = getSendingDuration() / 3;
        generalTimeout = getProcessingDuration() + getSendingDuration() + 500;

        serverProcessService = ServerProcessService.getInstance();
        serverProcessService.start();
    }

    @Test
    void shouldCorrectProcessAndSend() throws Exception {
        assertEquals(State.WAITING, serverProcessService.getState());

        Future<String> futureProcess = executorService
                .submit(() -> serverProcessService.doProcess());
        Thread.sleep(processDelay);
        assertEquals(State.PROCESSING, serverProcessService.getState());

        String result = futureProcess.get(generalTimeout, TimeUnit.SECONDS);
        assertNotNull(result);

        Future<?> futureResponse = executorService
                .submit(() -> serverProcessService.sendResponse());
        Thread.sleep(sendDelay);
        assertEquals(State.SENDING, serverProcessService.getState());

        futureResponse
                .get(generalTimeout, TimeUnit.MILLISECONDS);
        assertEquals(State.WAITING, serverProcessService.getState());
    }

    @Test
    void shouldNotProcessMoreThanOneTaskInSameTime() throws Exception {
        assertEquals(State.WAITING, serverProcessService.getState());

        Future<String> successProcess = executorService
                .submit(() -> serverProcessService.doProcess());
        Future<String> failedProcess = executorService
                .submit(() -> serverProcessService.doProcess());

        ExecutionException e = assertThrows(ExecutionException.class,
                () -> failedProcess.get(generalTimeout, TimeUnit.SECONDS));
        assertTrue(e.getCause() instanceof IllegalStateException);

        successProcess
                .get(generalTimeout, TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldNotSendResponseFromWaitingState() {
        assertEquals(State.WAITING, serverProcessService.getState());

        Future<?> future = executorService
                .submit(() -> serverProcessService.sendResponse());

        ExecutionException e = assertThrows(ExecutionException.class,
                () -> future.get(generalTimeout, TimeUnit.SECONDS));
        assertTrue(e.getCause() instanceof IllegalStateException);
    }

    @Test
    void shouldNotSendResponseIfProcessingNotCompleted() throws Exception {
        assertEquals(State.WAITING, serverProcessService.getState());

        Future<String> processFuture = executorService
                .submit(() -> serverProcessService.doProcess());
        Thread.sleep(processDelay);

        assertEquals(State.PROCESSING, serverProcessService.getState());
        assertThrows(IllegalStateException.class,
                () -> serverProcessService.sendResponse());

        processFuture
                .get(generalTimeout, TimeUnit.MILLISECONDS);
    }

    @AfterEach
    void returnToWaitMode() throws Exception {
        Field state = ServerProcessService.class.getDeclaredField("state");
        state.setAccessible(true);
        state.set(serverProcessService, State.WAITING);
        state.setAccessible(false);
    }
}
