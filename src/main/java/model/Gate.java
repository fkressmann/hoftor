package model;

public class Gate extends Sensor {
    public boolean moving;

    public boolean locked;

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

    public void setOpening() {
        state = Status.OPENING;
        sendState();
    }

    public void setClosing() {
        state = Status.CLOSING;
        sendState();
    }

    public void still(boolean moving) {
        this.moving = false;
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

    public boolean isClosing() {
        return state == Status.CLOSING;
    }

    public void setStill() {
        moving = false;
    }
}
