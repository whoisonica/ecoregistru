package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.DevicePlatform;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        /**
         * G1 — numele telefonului („iPhone 17”), trimis numai de aplicația mobilă. Când vine, în
         * răspuns apare și un token de reîmprospătare; webul nu-l trimite și nu-l primește, deci
         * o sesiune de browser rămâne exact ce era: opt ore și atât.
         */
        @Size(max = 80) String deviceName,
        DevicePlatform devicePlatform
) {}
