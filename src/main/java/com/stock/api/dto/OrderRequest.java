package com.stock.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    @NotEmpty(message = "La commande doit contenir au moins une ligne")
    @Size(max = 50, message = "La commande ne peut pas contenir plus de 50 lignes")
    @Valid
    private List<OrderLineRequest> lines;

    @Size(max = 500, message = "Les notes ne doivent pas dépasser 500 caractères")
    private String notes;
}
