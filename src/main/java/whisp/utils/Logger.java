package whisp.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    //*******************************************************************************************
    //* STATIC METHODS
    //*******************************************************************************************

    /**
     * Log para mostrar información durante la ejecución
     *
     * <p>
     *     Acompaña la información con la hora
     * </p>
     *
     * @param message información que mostrar
     */
    public static void info(String message)
    {
        LocalDate actualDate = LocalDate.now();
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String actualTime =  now.format(formatter);
        System.out.println("[INFO] " + actualDate + actualTime + ": " + message);
    }

    /**
     * Log para mostrar un error durante la ejecución
     *
     * <p>
     *     Acompaña el error con la hora
     * </p>
     *
     * @param message error que mostrar
     */
    public static void error(String message)
    {
        LocalDate actualDate = LocalDate.now();
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String actualTime =  now.format(formatter);
        System.err.println("[ERROR] " + actualDate + actualTime + ": " + message);

    }
}
