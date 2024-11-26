module whisp.server {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.rmi;
    requires java.sql;
    requires jdk.jshell;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires googleauth;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires commons.logging;


    opens whisp.server to javafx.fxml;
    exports whisp.server;
    exports whisp.interfaces;
    opens whisp.interfaces to javafx.fxml;
    exports whisp.utils;
    opens whisp.utils to javafx.fxml;

}