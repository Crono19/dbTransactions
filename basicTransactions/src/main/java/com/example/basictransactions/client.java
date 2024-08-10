package com.example.basictransactions;

import javafx.beans.property.SimpleStringProperty;

public class client {
    private final SimpleStringProperty name;
    private final SimpleStringProperty lastName;
    private final SimpleStringProperty address;
    private final SimpleStringProperty phone;

    public client(String name, String lastName, String address, String phone) {
        this.name = new SimpleStringProperty(name);
        this.lastName = new SimpleStringProperty(lastName);
        this.address = new SimpleStringProperty(address);
        this.phone = new SimpleStringProperty(phone);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getLastName() {
        return lastName.get();
    }

    public void setLastName(String lastName) {
        this.lastName.set(lastName);
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public String getPhone() {
        return phone.get();
    }

    public void setPhone(String phone) {
        this.phone.set(phone);
    }
}
