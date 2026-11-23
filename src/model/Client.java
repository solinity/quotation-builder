package model;

import java.io.Serializable;

public class Client implements Serializable {
   private String name;
   private String email; 
   private String phone;

   public Client(String name, String email, String phone) {
      setName(name);
      setEmail(email);
      setPhone(phone);
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

   public void setName(String name) {
      if (name == null || name.trim().isEmpty()) {
         throw new IllegalArgumentException("Name can't be empty");
      } 
      this.name = name.trim();
   }

   public void setEmail(String email) {
      String cleaned = clean(email);
      if (!cleaned.isEmpty() && !cleaned.contains("@")) {
         throw new IllegalArgumentException("Email must contain '@'");
      }
      this.email = cleaned;
   }

    public void setPhone(String phone) {
        this.phone = clean(phone);
    }

    public String getPhoneOrDefault() {
        return phone.isEmpty() ? "N/A" : phone;
    }

    public String getEmailOrDefault() {
        return email.isEmpty() ? "N/A" : email;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return name;
    }
}

































