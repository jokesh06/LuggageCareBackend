package com.example.demo.dto;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class UserToZohoContactMapper {

    private final ObjectMapper objectMapper;

    public UserToZohoContactMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode toZohoContact(UserRegistrationRequest request) {

        ObjectNode contact = objectMapper.createObjectNode();

        contact.put("First_Name", request.getFirstName());
        contact.put("Last_Name", request.getLastName());
        contact.put("Email", request.getEmailAddress());
        contact.put("Catalyst_ID", request.getCataystId());
        return contact;
    }
}
