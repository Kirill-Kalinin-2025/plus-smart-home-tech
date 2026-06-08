package ru.yandex.practicum.interaction.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseProductDto {

    private UUID productId;

    @NotBlank
    private String name;

    @Positive
    private Integer quantity;

    private Double width;
    private Double height;
    private Double depth;
    private Double weight;
    private Boolean fragile;
}