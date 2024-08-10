module com.example.basictransactions {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires java.desktop;


    opens com.example.basictransactions to javafx.fxml;
    exports com.example.basictransactions;
}