package com.zenora.controller;

import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;


public abstract class BaseModuleController {


    private String homeFxml = "/com/zenora/fxml/Dashboard.fxml";

    protected final String getHomeFxml()              { return homeFxml; }
    protected final void   setHomeFxml(String fxml)   { this.homeFxml = fxml; }


    public abstract String moduleTitle();

    @FXML
    protected void back() {
        SceneNavigator.navigateTo(homeFxml);
    }
}
