package com.example.demo.dto;

public class UserRegistrationRequest {

    private String firstName;
    private String lastName;
    private String emailAddress;
    private String cataystId;
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getEmailAddress() {
        return emailAddress;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    public String getCataystId() {
        return cataystId;
    }
    public void setCataystId(String cataystId) {
        this.cataystId = cataystId;
    }
    
    
}