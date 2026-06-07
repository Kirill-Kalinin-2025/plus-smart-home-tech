-- Удаляем старые таблицы (для чистоты)
DROP TABLE IF EXISTS scenario_actions CASCADE;
DROP TABLE IF EXISTS scenario_conditions CASCADE;
DROP TABLE IF EXISTS actions CASCADE;
DROP TABLE IF EXISTS conditions CASCADE;
DROP TABLE IF EXISTS sensors CASCADE;
DROP TABLE IF EXISTS scenarios CASCADE;

-- создаём таблицу scenarios
CREATE TABLE IF NOT EXISTS scenarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hub_id VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    UNIQUE(hub_id, name)
);

-- создаём таблицу sensors
CREATE TABLE IF NOT EXISTS sensors (
    id VARCHAR PRIMARY KEY,
    hub_id VARCHAR NOT NULL
);

-- создаём таблицу conditions (без привязки к сценарию и датчику)
CREATE TABLE IF NOT EXISTS conditions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR NOT NULL,
    operation VARCHAR NOT NULL,
    value INTEGER
);

-- создаём таблицу actions (без привязки к сценарию и датчику)
CREATE TABLE IF NOT EXISTS actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR NOT NULL,
    value INTEGER
);

-- создаём таблицу scenario_conditions, связывающую сценарий, датчик и условие
CREATE TABLE IF NOT EXISTS scenario_conditions (
    scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR REFERENCES sensors(id) ON DELETE CASCADE,
    condition_id BIGINT REFERENCES conditions(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, sensor_id, condition_id)
);

-- создаём таблицу scenario_actions, связывающую сценарий, датчик и действие
CREATE TABLE IF NOT EXISTS scenario_actions (
    scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR REFERENCES sensors(id) ON DELETE CASCADE,
    action_id BIGINT REFERENCES actions(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, sensor_id, action_id)
);