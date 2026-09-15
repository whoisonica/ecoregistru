package ro.ecoregistru.util;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Un cod numeric personal: 13 cifre, cu cifra de control corectă. Gol (null sau spaţii) e voie —
 * rubrica e opţională.
 *
 * <p>Cifra de control se verifică fiindcă un CNP cu o cifră greşită nu e un CNP invalid, e al altcuiva:
 * cheia {@code 279146358279}, suma produselor modulo 11, iar restul 10 se scrie 1.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCnp.Validator.class)
public @interface ValidCnp {

    String message() default "CNP invalid: 13 cifre, cu cifra de control corectă.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidCnp, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.isBlank() || isValidCnp(value.trim());
        }

        /** Public pentru serviciul de operațiuni: la metal CNP-ul e obligatoriu, nu doar bine format. */
        public static boolean isValidCnp(String cnp) {
            if (!cnp.matches("\\d{13}")) {
                return false;
            }
            String key = "279146358279";
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                sum += (cnp.charAt(i) - '0') * (key.charAt(i) - '0');
            }
            int control = sum % 11 == 10 ? 1 : sum % 11;
            return control == cnp.charAt(12) - '0';
        }
    }
}
