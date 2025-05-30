package ru.practicum.ewm.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@Valid
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewUserRequest {
    @NotBlank
    @NotNull
    @Size(min = 2, max = 250)
    String name;
    @NotNull
    @NotBlank
    @Email
    @Size(min = 6, max = 254)
    String email;
}
