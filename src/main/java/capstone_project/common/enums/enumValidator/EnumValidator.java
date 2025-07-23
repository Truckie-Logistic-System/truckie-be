package capstone_project.common.enums.enumValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EnumValidatorImpl.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValidator {
    Class<? extends Enum<?>> enumClass();
    String message() default "Value is not valid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
