package com.example.basictransactions;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @FXML
    private Label labelIsolation;

    private final ObservableList<client> clientData = FXCollections.observableArrayList();
    private Connection connection;
    private int currentIsolationLevel = Connection.TRANSACTION_READ_COMMITTED; // Default level

    private void initializeConnection() {
        try {
            DataBaseConnection dataBaseConnection = new DataBaseConnection();
            connection = dataBaseConnection.getConnection();
            if (connection != null) {
                connection.setAutoCommit(false); // Start in manual commit mode
                connection.setTransactionIsolation(currentIsolationLevel);
                labelIsolation.setText("Nivel actual: " + getIsolationLevelName(currentIsolationLevel));
            } else {
                System.out.println("Failed to establish database connection.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupTableView() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        clientTable.setItems(clientData);
    }

    private void reloadData(){
        loadClientsFromDatabase();
    }

    private void clearTxt(){
        txtName.clear();
        txtLastName.clear();
        txtAddress.clear();
        txtPhone.clear();
    }

    private void loadClientsFromDatabase() {
        try {
            String query = "SELECT c.Nombre, c.Apellido, c.Direccion, t.Numero FROM Cliente c LEFT JOIN Telefono t ON c.idCliente = t.Cliente_idCliente";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            clientData.clear(); // Clear existing data

            while (resultSet.next()) {
                String name = resultSet.getString("Nombre");
                String lastName = resultSet.getString("Apellido");
                String address = resultSet.getString("Direccion");
                String phone = resultSet.getString("Numero");
                clientData.add(new client(name, lastName, address, phone));
            }

            clientTable.setItems(clientData);
            clientTable.refresh();  // Ensure the table view is refreshed

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getIsolationLevelName(int isolationLevel) {
        return switch (isolationLevel) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "Lecturas no comprometidas";
            case Connection.TRANSACTION_READ_COMMITTED -> "Lecturas comprometidas";
            case Connection.TRANSACTION_REPEATABLE_READ -> "Lecturas repetibles";
            case Connection.TRANSACTION_SERIALIZABLE -> "Serializable";
            case Connection.TRANSACTION_NONE -> "Ninguno";
            default -> "Desconocido";
        };
    }

    @FXML
    private void initialize() {
        initializeConnection();
        setupTableView();
        loadClientsFromDatabase();

        // Set up a periodic refresh task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            reloadData();
        }, 0, 2, TimeUnit.SECONDS);  // Refresh every 2 seconds

        // Add this shutdown hook to cleanly exit the scheduler
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
    }

    @FXML
    public void startTransaction(ActionEvent actionEvent) {
        initializeConnection();  // Re-initialize the connection for a fresh transaction
        try {
            connection.setAutoCommit(false);
            System.out.println("Transaction started with isolation level: " + getIsolationLevelName(currentIsolationLevel));
            labelIsolation.setText("Nivel actual: " + getIsolationLevelName(currentIsolationLevel));
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
                reloadData();  // Reload data to reflect changes
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
                reloadData();  // Reload data to reflect the rollback
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

            reloadData();
            clearTxt();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void updateClient(ActionEvent actionEvent) {
        if (connection == null) {
            System.out.println("Transaction not started. Call startTransaction first.");
            return;
        }

        String clientName = txtName.getText();
        String lastName = txtLastName.getText();
        String address = txtAddress.getText();

        if (clientName.isEmpty()) {
            System.out.println("Client name must be provided.");
            return;
        }

        String getClientIdQuery = "SELECT idCliente FROM Cliente WHERE Nombre = ?";
        String updateClientQuery = "UPDATE Cliente SET Apellido = ?, Direccion = ? WHERE idCliente = ?";

        try {
            // Get the client ID based on the client's name
            PreparedStatement getClientIdStmt = connection.prepareStatement(getClientIdQuery);
            getClientIdStmt.setString(1, clientName);
            ResultSet resultSet = getClientIdStmt.executeQuery();

            if (resultSet.next()) {
                int clientId = resultSet.getInt("idCliente");

                // Update the client's last name and address
                PreparedStatement updateClientStmt = connection.prepareStatement(updateClientQuery);
                updateClientStmt.setString(1, lastName);
                updateClientStmt.setString(2, address);
                updateClientStmt.setInt(3, clientId);

                int rowsAffectedClient = updateClientStmt.executeUpdate();
                System.out.println(rowsAffectedClient + " row(s) updated in Cliente.");

                reloadData();
                clearTxt();

            } else {
                System.out.println("Client not found.");
            }

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

                reloadData();
                txtPhone.clear();
            } else {
                System.out.println("Client not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void changeIsolation(ActionEvent actionEvent) {
        try {
            // Ensure the connection is initialized for the session
            if (connection == null) {
                initializeConnection();
            }

            // Cycle through isolation levels only for the current session
            switch (currentIsolationLevel) {
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    currentIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    currentIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    currentIsolationLevel = Connection.TRANSACTION_SERIALIZABLE;
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                default:
                    currentIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED;
                    break;
            }

            // Set the new isolation level on the current session's connection
            connection.setTransactionIsolation(currentIsolationLevel);

            // Update the label to reflect the new isolation level
            labelIsolation.setText("Nivel actual: " + getIsolationLevelName(currentIsolationLevel));
            System.out.println("Isolation level changed to: " + getIsolationLevelName(currentIsolationLevel) + " for this session.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void reload(ActionEvent actionEvent) {
        reloadData();
        initializeConnection();
    }
}
