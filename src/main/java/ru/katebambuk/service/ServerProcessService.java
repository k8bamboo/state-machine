package ru.katebambuk.service;

import ru.katebambuk.util.PropertyResolver;
import ru.katebambuk.dictionary.State;

import java.util.Objects;
import java.util.logging.Logger;

import static ru.katebambuk.dictionary.State.*;

public class ServerProcessService {

    private static final Logger LOGGER = Logger.getLogger(ServerProcessService.class.getName());

    private static ServerProcessService instance;

    private State state;
    private boolean processingCompleted;
    private boolean responseSent;
    private int processingDuration = PropertyResolver.getProcessingDuration();
    private int sendingDuration = PropertyResolver.getSendingDuration();

    private ServerProcessService() {
    }

    public static ServerProcessService getInstance() {
        if (Objects.isNull(instance)) {
            instance = new ServerProcessService();
        }
        return instance;
    }

    public State getState() {
        return state;
    }

    public void start() {
        state = WAITING;
    }

    public String doProcess() {
        changeState(PROCESSING);

        processingCompleted = false;
        emulateWork(processingDuration);
        processingCompleted = true;
        return "process result";
    }

    public void sendResponse() {
        changeState(SENDING);

        responseSent = false;
        emulateWork(sendingDuration);
        responseSent = true;
        changeState(WAITING);
    }

    private synchronized void changeState(State newState) {
        LOGGER.info(String.format("Try to change state to %s", newState.name()));
        switch (newState) {
            case PROCESSING:
                tryChangeState(newState, WAITING, null);
                break;

            case SENDING:
                tryChangeState(newState, PROCESSING, processingCompleted);
                break;

            case WAITING:
                tryChangeState(newState, SENDING, responseSent);
                break;
        }
        LOGGER.info(String.format("Current state is %s", state.name()));
    }

    private void tryChangeState(State newState, State acceptableState, Boolean additionalParam) {
        if (acceptableState != this.state || (additionalParam != null && !additionalParam)) {
            throw new IllegalStateException("Transition is impossible");
        }
        state = newState;
    }

    private void emulateWork(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
