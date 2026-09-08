package es.udc.fic.corpuslab.common.validation;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {

    private int minimumAge;

    @Override
    public void initialize(MinAge constraintAnnotation) {
        this.minimumAge = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate latestAllowedBirthDate = LocalDate.now(Clock.systemUTC()).minusYears(minimumAge);
        return !value.isAfter(latestAllowedBirthDate);
    }
}
