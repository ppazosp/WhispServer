package whisp;

import java.time.LocalDate;

public class Logger {

    public static void info(String message)
    {
        LocalDate actualDate = LocalDate.now();
        System.out.println("[INFO] " + actualDate + ": " + message);
    }

    public static void error(String message)
    {
        LocalDate actualDate = LocalDate.now();
        System.err.println("[ERROR] " + actualDate + ": " + message);
    }
}
