package org.example.service.impl;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.example.dao.AppUserDao;
import org.example.dao.RawDataDao;
import org.example.entity.AppUser;
import org.example.entity.RawData;
import org.example.service.MainService;
import org.example.service.ProducerService;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.example.service.enums.ServiceCommands.*;

@AllArgsConstructor
@Service
public class MainServiceImpl implements MainService {
    private final RawDataDao rawDataDao;
    private final ProducerService producerService;
    private final AppUserDao appUserDao;

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        AppUser appUser = findOrSaveAppUser(update);
        var text = update.getMessage().getText();
        var chatId = update.getMessage().getChatId();
        var output = "";

        if (text.charAt(0) == '/') {
            output = processServiceCommand(appUser, text);
        } else {
            output = processPrompt(text, chatId);
        }

        sendAnswer(output, chatId);
    }

    @SneakyThrows
    private String processPrompt(String text, Long chatId) {
        String urlString = "http://localhost:11434/api/generate";
        String jsonInputString = "{ \"model\": \"qwen2.5:0.5b\", \"prompt\": \"" +
                text + "\", \"stream\": false }";


        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        OutputStream os = connection.getOutputStream();
        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);

        sendAnswer("Связываюсь с космосом, чтобы ответить вам...", chatId);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            String message = jsonObject.getString("response");

            return message;
        } else {
            return "Не удалось получить ответ от qwen2.5";
        }
    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        if (HELP.equals(cmd)) {
            return help();
        } else if (START.equals(cmd)) {
            return "Приветствую! Чтобы посмотреть список доступных команд введите /help";
        } else if (INFO.equals(cmd)) {
            return info();
        } else if (MODELS.equals(cmd)) {
            return models();
        } else if (SUBSCRIBE.equals(cmd)) {
            return "На данный момент эта функция недоступна";
        } else {
            return "Неизвестная команда. Чтобы посмотреть список доступных команд введите /help";
        }
    }

    private String models() {
        return "На данный момент я работаю лишь с одной моделью искусственного интеллекта - qwen-2.5\n"
                + "Qwen 2.5 — мощная модель искусственного интеллекта, созданная для того, " +
                "чтобы сделать вашу жизнь проще и эффективнее! Эта инновационная система " +
                "обладает выдающимися возможностями обработки естественного языка и машинного " +
                "обучения, позволяя вам решать самые разнообразные задачи с легкостью и точностью.\n" +
                "\n" +
                "С Qwen 2.5 вы можете:\n" +
                "\n" +
                "• Автоматизировать рутинные задачи: Освободите время для креативной работы, " +
                "доверив модели выполнение повторяющихся задач.\n" +
                "\n" +
                "• Получать точные ответы: Благодаря продвинутым алгоритмам, " +
                "Qwen 2.5 обеспечивает высокую степень точности и актуальности информации.\n" +
                "\n" +
                "• Создавать уникальный контент: Генерируйте тексты, статьи и идеи, " +
                "которые будут привлекать внимание и вызывать интерес у вашей аудитории.\n" +
                "\n" +
                "• Поддерживать общение на естественном языке: Взаимодействуйте с пользователями " +
                "так, как будто вы говорите с человеком — быстро и удобно.";
    }

    private String info() {
        return "NosedStickBot телеграм-бот с генеративным искусственным интеллектом — " +
                "это инновационное решение, которое позволяет пользователям взаимодействовать " +
                "с мощной моделью ИИ прямо в мессенджере Telegram. Такой бот способен выполнять " +
                "разнообразные задачи, начиная от генерации текстов и ответов на вопросы " +
                "до создания креативного контента и помощи в обучении.";
    }

    private String help() {
        return "Список доступных команд:\n"
                + "/info - краткая информация о боте;\n"
                + "/models - информация об используемых моелях ИИ;\n"
                + "/subscribe - оформление премиум подписки на бота;";
    }

    private AppUser findOrSaveAppUser(Update update) {
        User telegramUser = update.getMessage().getFrom();
        AppUser persistentAppUser = appUserDao.findAppUserByTelegramUserId(telegramUser.getId());
        if (persistentAppUser == null) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .build();
            return appUserDao.save(transientAppUser);
        }
        return persistentAppUser;
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDao.save(rawData);
    }
}
