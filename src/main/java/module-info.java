module com.example.microscopeapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.microscopeapp to javafx.fxml;
    exports com.example.microscopeapp;
}