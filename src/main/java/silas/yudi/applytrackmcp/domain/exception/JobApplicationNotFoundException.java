package silas.yudi.applytrackmcp.domain.exception;

public class JobApplicationNotFoundException extends RuntimeException {

    public JobApplicationNotFoundException(String id) {
        super("Candidatura não encontrada: " + id);
    }
}
