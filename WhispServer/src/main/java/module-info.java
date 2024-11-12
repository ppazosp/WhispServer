module whisp.server {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.rmi;


    opens whisp.server to javafx.fxml;
    exports whisp.server;

}