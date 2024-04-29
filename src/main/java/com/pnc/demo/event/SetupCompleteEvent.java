package com.pnc.demo.event;

import org.springframework.context.ApplicationEvent;

public class SetupCompleteEvent extends ApplicationEvent {
    public SetupCompleteEvent(Object source) {
        super(source);
    }
}
