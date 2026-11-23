import java.util.ArrayList;
import model.Client;

public class DataStore {
    private final ArrayList<Client> clients = new ArrayList<>();

    public void addClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Client can't be null");
        }
        clients.add(client);
    }

    public boolean removeClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Client can't be null");
        }

        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i) == client) {
                // TODO: Define and enforce a deletion policy for clients referenced by quotations.
                clients.remove(i);
                return true;
            }
        }
        return false;
    }

    public int getClientCount() {
        return clients.size();
    }

    public Client getClient(int index) {
        return clients.get(index);
    }
}
