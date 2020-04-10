package com.gluonhq.attach.pushnotifications.impl.gms;

public class GoogleServicesConfiguration {

    private String applicationId;
    private String projectNumber;

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setProjectNumber(String projectNumber) {
        this.projectNumber = projectNumber;
    }

    public String getProjectNumber() {
        return projectNumber;
    }
}