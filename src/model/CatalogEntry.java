package model;

import static model.Validation.require;

import java.io.Serializable;

public abstract class CatalogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    private CatalogEntry() {
    }

    public final String getName() {
        return name;
    }

    protected final void setName(String validatedName) {
        name = validatedName;
    }

    private static String requiredName(String name, String message) {
        String cleaned = clean(name);
        require(!cleaned.isEmpty(), message);
        return cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public final String toString() {
        return name;
    }

    public static final class Client extends CatalogEntry {
        private static final long serialVersionUID = 1L;
        private String email;
        private String phone;

        public Client(String name, String email, String phone) {
            update(name, email, phone);
        }

        public void update(String name, String email, String phone) {
            String cleanedName = requiredName(name, "Name can't be empty");
            String cleanedEmail = clean(email);
            require(cleanedEmail.isEmpty() || cleanedEmail.contains("@"), "Email must contain '@'");
            setName(cleanedName);
            this.email = cleanedEmail;
            this.phone = clean(phone);
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmailOrDefault() {
            return email.isEmpty() ? "N/A" : email;
        }

        public String getPhoneOrDefault() {
            return phone.isEmpty() ? "N/A" : phone;
        }
    }

    public static final class Offering extends CatalogEntry {
        private static final long serialVersionUID = 1L;
        private String description;
        private double unitPrice;

        public Offering(String name, String description, double unitPrice) {
            update(name, description, unitPrice);
        }

        public void update(String name, String description, double unitPrice) {
            String cleanedName = requiredName(name, "Enter an offering name.");
            this.unitPrice = checkedPrice(unitPrice);
            setName(cleanedName);
            this.description = clean(description);
        }

        public String getDescription() {
            return description;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        static double checkedPrice(double price) {
            require(Double.isFinite(price) && price >= 0, "Unit price must be a finite number of zero or more.");
            return price == 0 ? 0 : price;
        }
    }
}
