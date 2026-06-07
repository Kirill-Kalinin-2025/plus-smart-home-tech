package ru.yandex.practicum.interaction.dto.shoppingstore;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetQuantityRequest {

    @NotNull
    private QuantityState quantityState;
}