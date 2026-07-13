package co.com.hermes.calendar.identity.config;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.format.DateTimeParseException;

/**
 * Manejador global de errores. Normaliza las entradas malformadas del cliente a {@code 400 Bad Request}
 * con cuerpo {@link ProblemDetail} (RFC 7807), en lugar de propagarlas como {@code 500}.
 *
 * <p>Las excepciones estándar de Spring MVC (tipo/binding, cuerpo ilegible, parámetro ausente,
 * {@code @Valid}) las mapea la clase base {@link ResponseEntityExceptionHandler} a 400. Aquí se cubren
 * además los casos que hoy escapaban a 500: violaciones de {@code @Validated} en parámetros,
 * propiedad de ordenación/{@code Pageable} inexistente y argumentos ilegales de negocio. Las
 * {@code ResponseStatusException} conservan su estado (las gestiona la clase base).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        return badRequest("Parámetros de la petición inválidos", ex.getMessage());
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    ProblemDetail handleInvalidDataAccessUsage(InvalidDataAccessApiUsageException ex) {
        return badRequest("Parámetro de paginación u ordenación inválido", ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handlePropertyReference(PropertyReferenceException ex) {
        return badRequest("Parámetro de ordenación o filtrado inválido", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    ProblemDetail handleBadArgument(RuntimeException ex) {
        return badRequest("Petición inválida", ex.getMessage());
    }

    private ProblemDetail badRequest(String title, String detail) {
        log.warn("Petición rechazada (400): {}", detail);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }
}
