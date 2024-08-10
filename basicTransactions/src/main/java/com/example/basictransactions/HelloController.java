package com.example.basictransactions;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;

public class HelloController {

    @FXML
    private TableView<client> clientTable;

    @FXML
    private TableColumn<client, String> nameColumn;

    @FXML
    private TableColumn<client, String> lastNameColumn;

    @FXML
    private TableColumn<client, String> addressColumn;

    @FXML
    private TableColumn<client, String> phoneColumn;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtLastName;

    @FXML
    private TextField txtAddress;

    @FXML
    private TextField txtPhone;

    private final ObservableList<client> clientData = FXCollections.observableArrayList();
    private Connection connection;

    private void clearTxt(){
        txtName.clear();
        txtLastName.clear();
        txtAddress.clear();
    }

    private void loadClientsFromDatabase() {
        try {
            String query = "SELECT c.Nombre, c.Apellido, c.Direccion, t.Numero FROM Cliente c LEFT JOIN Telefono t ON c.idCliente = t.Cliente_idCliente";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            // Clear existing data
            clientData.clear();

            // Populate the ObservableList with new data
            while (resultSet.next()) {
                String name = resultSet.getString("Nombre");
                String lastName = resultSet.getString("Apellido");
                String address = resultSet.getString("Direccion");
                String phone = resultSet.getString("Numero");
                clientData.add(new client(name, lastName, address, phone));
            }

            // Set the updated list to the TableView
            clientTable.setItems(clientData);
            clientTable.refresh();  // Ensure the table view is refreshed

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void initialize() {
        DataBaseConnection dataBaseConnection = new DataBaseConnection();
        connection = dataBaseConnection.getConnection();

        if (connection != null) {
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
            phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

            // Load data from the database
            loadClientsFromDatabase();

            // Assign the data to the table
            clientTable.setItems(clientData);
        } else {
            System.out.println("Failed to establish database connection.");
        }
    }

    @FXML
    public void startTransaction(ActionEvent actionEvent) {
        try {
            DataBaseConnection dataBaseConnection = new DataBaseConnection();
            connection = dataBaseConnection.getConnection();
            connection.setAutoCommit(false);
            System.out.println("Transaction started.");

            clearTxt();
            txtName.clear();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void commit(ActionEvent actionEvent) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.commit();
                System.out.println("Transaction committed.");
                // Clear and reload the data from the database
                clientData.clear();
                loadClientsFromDatabase();

                // Refresh the TableView to display the latest data
                clientTable.refresh();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void rollBack(ActionEvent actionEvent) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                System.out.println("Transaction rolled back.");
                // Clear and reload the data from the database
                clientData.clear();
                loadClientsFromDatabase();

                // Refresh the TableView to display the latest data
                clientTable.refresh();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void insertClient() {
        if (connection == null) {
            System.out.println("Transaction not started. Call startTransaction first.");
            return;
        }

        if (txtName.getText().isEmpty() || txtLastName.getText().isEmpty() || txtAddress.getText().isEmpty()) {
            System.out.println("Client name, last name, and address must be provided.");
            return;
        }

        String insertQuery = "INSERT INTO Cliente (Nombre, Apellido, Direccion) VALUES (?, ?, ?)";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
            preparedStatement.setString(1, txtName.getText());
            preparedStatement.setString(2, txtLastName.getText());
            preparedStatement.setString(3, txtAddress.getText());

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println(rowsAffected + " row(s) inserted.");

            // Clear and reload the data from the database
            clientData.clear();
            loadClientsFromDatabase();

            // Refresh the TableView to display the latest data
            clientTable.refresh();

            clearTxt();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void insertPhone(ActionEvent actionEvent) {
        if (connection == null) {
            System.out.println("Transaction not started. Call startTransaction first.");
            return;
        }

        String clientName = txtName.getText();
        String phone = txtPhone.getText();

        if (clientName.isEmpty() || phone.isEmpty()) {
            System.out.println("Client name and phone number must be provided.");
            return;
        }

        String getClientIdQuery = "SELECT idCliente FROM Cliente WHERE Nombre = ?";
        String insertPhoneQuery = "INSERT INTO Telefono (Numero, Cliente_idCliente) VALUES (?, ?)";

        try {
            // Get the client ID based on the client's name
            PreparedStatement getClientIdStmt = connection.prepareStatement(getClientIdQuery);
            getClientIdStmt.setString(1, clientName);
            ResultSet resultSet = getClientIdStmt.executeQuery();

            if (resultSet.next()) {
                int clientId = resultSet.getInt("idCliente");

                // Insert the phone number for the client
                PreparedStatement insertPhoneStmt = connection.prepareStatement(insertPhoneQuery);
                insertPhoneStmt.setString(1, phone);
                insertPhoneStmt.setInt(2, clientId);

                int rowsAffected = insertPhoneStmt.executeUpdate();
                System.out.println(rowsAffected + " row(s) inserted.");

                // Clear and reload the data from the database
                clientData.clear();
                loadClientsFromDatabase();

                // Refresh the TableView to display the latest data
                clientTable.refresh();

                txtPhone.clear();
            } else {
                System.out.println("Client not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
