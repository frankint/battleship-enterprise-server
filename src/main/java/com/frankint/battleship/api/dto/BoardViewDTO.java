package com.frankint.battleship.api.dto;

import java.util.List;

public record BoardViewDTO(
        List<String> grid
) {
}