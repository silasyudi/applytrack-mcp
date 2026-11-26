package silas.yudi.applytrackmcp.application.port.out;

/** O banco não respondeu. A mensagem diz ao usuário o que fazer. */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
