package model;

public class Client {
    private final String name;
    private final String email;
    private final String phone;

    public Client(String name, String email, String phone) {
        this.name = name.trim();
        this.email = email.trim();
        this.phone = phone.trim();

        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Enter the client's name.");
        }
        if (!this.email.isEmpty()) {
            int at = this.email.indexOf('@');
            if (at < 1 || at == this.email.length() - 1
                    || at != this.email.lastIndexOf('@') || this.email.contains(" ")) {
                throw new IllegalArgumentException("Enter an email such as name@example.com.");
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean hasSameContact(Client client) {
        return name.equals(client.name) && email.equals(client.email) && phone.equals(client.phone);
    }

    @Override
    public String toString() {
        if (!email.isEmpty()) {
            return name + " (" + email + ")";
        }
        if (!phone.isEmpty()) {
            return name + " (" + phone + ")";
        }
        return name;
    }
}
