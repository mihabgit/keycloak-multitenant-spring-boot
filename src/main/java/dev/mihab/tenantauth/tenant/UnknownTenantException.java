package dev.mihab.tenantauth.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnknownTenantException extends RuntimeException {

    public UnknownTenantException(String message) {
        super(message);
    }
}
