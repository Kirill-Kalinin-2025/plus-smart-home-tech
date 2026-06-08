package ru.yandex.practicum.interaction.dto.warehouse;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionDto {

    @Positive
    private Double width;

    @Positive
    private Double height;

    @Positive
    private Double depth;
}