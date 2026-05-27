package ro.upt.pontaje.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceptie pentru resurse negasite (404)
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, String field, Object value) {
        super(String.format("%s nu a fost găsit cu %s: '%s'", resourceName, field, value));
    }
}
