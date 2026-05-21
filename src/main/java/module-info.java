module Zenora {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.google.gson;
    requires java.desktop;
    requires java.logging;

    opens com.zenora.app        to javafx.fxml;
    opens com.zenora.controller to javafx.fxml;
    opens com.zenora.model      to javafx.fxml, com.google.gson;
    opens com.zenora.service    to com.google.gson;

    exports com.zenora.app;
    exports com.zenora.controller;
    exports com.zenora.model;
    exports com.zenora.service;
    exports com.zenora.util;
}
