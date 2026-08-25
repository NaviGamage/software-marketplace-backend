package com.marketplace.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Slug is required")
        @Size(max = 255, message = "Slug must not exceed 255 characters")
        @Pattern(
                regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Slug must be lowercase, alphanumeric, and hyphen-separated"
        )
        String slug,

        @NotBlank(message = "Description is required")
        String description,

        @Pattern(
                regexp = "^https?://.+",
                message = "Demo URL must be a valid http(s) URL"
        )
        String demoUrl,

        @NotNull(message = "Regular price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Regular price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Regular price must have at most 2 decimal places")
        BigDecimal priceRegular,

        @NotNull(message = "Extended price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Extended price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Extended price must have at most 2 decimal places")
        BigDecimal priceExtended,

        @NotNull(message = "Category is required")
        Long categoryId
) {
}