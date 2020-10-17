package android.net.networkstack.util;

import android.os.Message;

public class State implements IState {
    public void enter() {
    }

    public void exit() {
    }

    public boolean processMessage(Message message) {
        return false;
    }

    protected State() {
    }

    @Override // android.net.networkstack.util.IState
    public String getName() {
        String name = getClass().getName();
        return name.substring(name.lastIndexOf(36) + 1);
    }
}
