package com.anima.villagers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранитель — молчаливый сторож систем.
 *
 * ФИЛОСОФИЯ:
 *  - Он НЕ управляет жителями. Он просто смотрит.
 *  - Он НЕ реагирует на игрока. Игрок — часть мира, его действия легитимны.
 *  - Он вмешивается ТОЛЬКО когда жители сами ведут деревню к вымиранию.
 *  - Он ведёт лог — что происходит в системах, для Telegram-бота.
 *
 * Пример: если жители перестали работать и еда кончилась — Хранитель
 * даст знак (алерт в ТГ), но не будет сам раздавать еду. Это должны
 * решить жители. Если они не могут — вот тогда Хранитель вмешивается.
 */
public class TheKeeper {
    public static final TheKeeper INSTANCE = new TheKeeper();
    private static final Logger LOGGER = LoggerFactory.getLogger("Anima/TheKeeper");

    // ====== РЕЕСТР (только чтение) ======
    private final Map<UUID, VillagerCharacter> villagers = new ConcurrentHashMap<>();

    // ====== СОСТОЯНИЕ МИРА ======
    private int population = 0;
    private int foodStock = 100;
    private int resources = 50;
    private int mood = 50;
    private int dayCounter = 0;

    // ====== СИСТЕМНЫЕ МЕТРИКИ ======
    private int systemErrorsToday = 0;      // ошибки в системах жителей
    private int deathsFromStarvation = 0;   // смерти от голода
    private int deathsFromWar = 0;          // смерти от гражданских войн
    private int daysInCrisis = 0;           // сколько дней подряд кризис
    private String crisisType = null;

    // ====== СОСТОЯНИЕ СТОРОЖА ======
    private final List<String> alerts = new ArrayList<>();       // для ТГ
    private final List<WorldEvent> timeline = new ArrayList<>(); // что вообще было

    private TheKeeper() {}

    // ================================================================
    //  НАБЛЮДЕНИЕ (только чтение)
    // ================================================================

    public void observeVillager(UUID uuid, VillagerCharacter character) {
        villagers.put(uuid, character);
        population = villagers.size();
        timeline.add(new WorldEvent("register", character.name + " появился в мире"));
        LOGGER.info("[Хранитель] Наблюдаю за {}", character.name);
    }

    public void noteVillagerGone(UUID uuid, String reason) {
        VillagerCharacter v = villagers.remove(uuid);
        population = villagers.size();
        if (v == null) return;

        timeline.add(new WorldEvent("death", v.name + " — " + reason));

        // Классифицируем смерти для аналитики
        if (reason.contains("голод")) deathsFromStarvation++;
        if (reason.contains("убит") || reason.contains("война")) deathsFromWar++;

        LOGGER.info("[Хранитель] {} исчез ({})", v.name, reason);
    }

    /** Сообщить о системной ошибке (баг в мозге жителя, LLM-сбой и т.д.). */
    public void noteSystemError(String source, String error) {
        systemErrorsToday++;
        timeline.add(new WorldEvent("error", source + ": " + error));
        LOGGER.warn("[Хранитель] Системная ошибка ({}): {}", source, error);

        // Если ошибок слишком много — тревога
        if (systemErrorsToday > 20) {
            alert("⚠️ Слишком много системных ошибок (" + systemErrorsToday + "). Мозг жителей сбоит.");
        }
    }

    // ================================================================
    //  ГЛАВНЫЙ ЦИКЛ — раз в игровой день
    // ================================================================

    public void tick() {
        dayCounter++;

        // Считаем естественный баланс (жители сами едят/тратят)
        foodStock += population * 2 - villagers.size() * 3;
        resources -= population / 2;
        foodStock = Math.max(0, Math.min(1000, foodStock));
        resources = Math.max(0, Math.min(1000, resources));

        // Анализ ситуации
        analyzeCrisis();
        checkSystemErrors();

        // Логируем раз в 5 дней
        if (dayCounter % 5 == 0) {
            logStatus();
        }
    }

    // ================================================================
    //  АНАЛИЗ КРИЗИСОВ
    // ================================================================

    private void analyzeCrisis() {
        String newCrisis = null;

        // Голод — деревня сама себя загнала
        if (foodStock < population * 3 && villagers.size() > 0) {
            newCrisis = "голод";
        }
        // Полное отсутствие ресурсов
        else if (resources <= 0 && villagers.size() > 0) {
            newCrisis = "крах экономики";
        }
        // Массовая гибель
        else if (deathsFromStarvation + deathsFromWar > population / 2) {
            newCrisis = "вымирание";
        }

        if (newCrisis != null) {
            if (newCrisis.equals(crisisType)) {
                daysInCrisis++;
            } else {
                crisisType = newCrisis;
                daysInCrisis = 1;
                alert("⚠️ Начался кризис: " + crisisType);
            }

            // === ТОЧКА ВМЕШАТЕЛЬСТВА ===
            // Хранитель молчит, пока жители справляются.
            // Но если кризис длится 7+ дней — деревня не справляется сама.
            if (daysInCrisis >= 7) {
                intervene();
            }
        } else {
            if (crisisType != null) {
                timeline.add(new WorldEvent("recovery", "Кризис \"" + crisisType + "\" пройден"));
                alert("✅ Кризис \"" + crisisType + "\" закончился сам");
            }
            crisisType = null;
            daysInCrisis = 0;
        }
    }

    private void checkSystemErrors() {
        // Сброс счётчика раз в день
        if (systemErrorsToday > 30) {
            alert("🚨 КРИТИЧНО: " + systemErrorsToday + " ошибок за день. Мозг жителей нестабилен.");
            // Это ЕДИНСТВЕННЫЙ случай, когда Хранитель глушит системы
            emergencyPause("Слишком много ошибок, мозг остановлен до стабилизации");
        }
        systemErrorsToday = 0;
    }

    // ================================================================
    //  ВМЕШАТЕЛЬСТВО — только в крайнем случае
    // ================================================================

    /**
     * Хранитель вмешивается ТОЛЬКО когда деревня уже 7 дней
     * не может выбраться из кризиса сама.
     */
    private void intervene() {
        LOGGER.warn("[Хранитель] ВМЕШАТЕЛЬСТВО. Причина: {} ({} дней)",
            crisisType, daysInCrisis);
        alert("🛠️ Вмешательство Хранителя: " + crisisType);

        switch (crisisType) {
            case "голод" -> {
                // Не раздаёт еду — просто "подкидывает" ресурсов в мир
                foodStock += 50;
                timeline.add(new WorldEvent("intervention",
                    "Хранитель дал миру знак — стада пришли к деревне"));
            }
            case "крах экономики" -> {
                resources += 30;
                timeline.add(new WorldEvent("intervention",
                    "Хранитель дал знак — в реке нашли руду"));
            }
            case "вымирание" -> {
                // Самое серьёзное — Хранитель будит "мигрирующих" жителей
                timeline.add(new WorldEvent("intervention",
                    "Хранитель послал странников — деревня получит свежую кровь"));
                alert("🚨 Деревня на грани вымирания. Требуется внешнее вмешательство.");
            }
        }

        daysInCrisis = 0;  // сбрасываем — дадим жителям шанс
    }

    /** Аварийная пауза — глушит мозг жителей (только для системных сбоев). */
    private void emergencyPause(String reason) {
        LOGGER.error("[Хранитель] АВАРИЙНАЯ ПАУЗА: {}", reason);
        alert("⛔ АВАРИЙНАЯ ПАУЗА: " + reason);
        timeline.add(new WorldEvent("emergency", reason));
    }

    // ================================================================
    //  АЛЕРТЫ (для Telegram-бота)
    // ================================================================

    public void alert(String msg) {
        alerts.add(msg);
        LOGGER.warn("[ALERT] {}", msg);
    }

    /** Забирает алерты для отправки в ТГ. */
    public List<String> drainAlerts() {
        List<String> copy = new ArrayList<>(alerts);
        alerts.clear();
        return copy;
    }

    private void logStatus() {
        LOGGER.info("[Хранитель] День {}: население={}, еда={}, ресурсы={}, ошибок={}{}",
            dayCounter, population, foodStock, resources, systemErrorsToday,
            crisisType != null ? ", кризис: " + crisisType + " (" + daysInCrisis + "д)" : "");
    }

    // ================================================================
    //  ГЕТТЕРЫ
    // ================================================================

    public int getPopulation() { return population; }
    public int getFoodStock() { return foodStock; }
    public int getResources() { return resources; }
    public int getMood() { return mood; }
    public int getDayCounter() { return dayCounter; }
    public String getCrisisType() { return crisisType; }
    public int getDaysInCrisis() { return daysInCrisis; }
    public List<WorldEvent> getTimeline() { return Collections.unmodifiableList(timeline); }
    public Collection<VillagerCharacter> getVillagers() { return villagers.values(); }
}
