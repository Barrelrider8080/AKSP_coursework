package org.example.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ConsumerService {
    void consumeTextMessageUpdates(Update update);
}
