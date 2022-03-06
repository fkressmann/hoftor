package model;

public class Gate extends Sensor {
    private boolean moving;

    private boolean locked;

    public Gate(String mqttPrefix) {
        super(mqttPrefix);
        this.locked = false;
        this.moving = false;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isStill() {
        return !moving;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setMoving() {
        this.moving = true;

    }

    public void setMoving(boolean moving) {
        this.moving = moving;

    }

    public void setOpening() {
        state = Status.OPENING;
        moving = true;
        sendState();
    }

    public void setClosing() {
        state = Status.CLOSING;
        moving = true;
        sendState();
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }


    public boolean isOpening() {
        return state == Status.OPENING;
    }

    public boolean isOpeningInProgress() {
        return state == Status.OPENING || state == Status.OPEN;
    }

    public boolean isClosing() {
        return state == Status.CLOSING;
    }

    public boolean isClosingInProgress() {
        return state == Status.CLOSING || state == Status.CLOSED;
    }

    public void setStill() {
        moving = false;
    }
}
