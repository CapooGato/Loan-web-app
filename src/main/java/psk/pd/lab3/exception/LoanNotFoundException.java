package psk.pd.lab3.exception;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String id) {
        super("Nie znaleziono oferty kredytowej o ID: " + id);
    }
}
