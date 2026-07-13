package co.com.hermes.calendar.identity.config;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el {@link GlobalExceptionHandler} traduce las entradas malformadas del cliente a
 * {@code 400} (antes producían {@code 500}), incluidas las que gestiona la clase base
 * ({@code MethodArgumentTypeMismatchException}) y las añadidas aquí.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void malformedUuidParamYields400() throws Exception {
        mvc.perform(get("/t/uuid").param("id", "no-es-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void constraintViolationYields400() throws Exception {
        mvc.perform(get("/t/constraint")).andExpect(status().isBadRequest());
    }

    @Test
    void illegalArgumentYields400() throws Exception {
        mvc.perform(get("/t/illegal")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortPropertyYields400() throws Exception {
        mvc.perform(get("/t/property")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidDataAccessUsageYields400() throws Exception {
        mvc.perform(get("/t/dao")).andExpect(status().isBadRequest());
    }

    @RestController
    static class DummyController {

        @GetMapping("/t/uuid")
        String uuid(@RequestParam UUID id) {
            return id.toString();
        }

        @GetMapping("/t/constraint")
        String constraint() {
            throw new ConstraintViolationException("violación de restricción", Set.of());
        }

        @GetMapping("/t/illegal")
        String illegal() {
            throw new IllegalArgumentException("argumento ilegal");
        }

        @GetMapping("/t/property")
        String property() {
            throw new PropertyReferenceException("inexistente", TypeInformation.of(String.class), List.of());
        }

        @GetMapping("/t/dao")
        String dao() {
            throw new InvalidDataAccessApiUsageException("Page offset exceeds Integer.MAX_VALUE");
        }
    }
}
