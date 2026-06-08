package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.analyzer.entity.ScenarioConditionId;

import java.util.List;

@Repository
public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {

    void deleteByScenario(Scenario scenario);

    List<ScenarioCondition> findByScenario(Scenario scenario);
}