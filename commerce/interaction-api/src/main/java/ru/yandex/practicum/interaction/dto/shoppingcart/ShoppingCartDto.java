package ru.yandex.practicum.interaction.dto.shoppingcart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCartDto {

    private UUID shoppingCartId;

    private Map<UUID, Integer> products;
}