package me.nakilex.levelplugin.auctionhouse.data;

import me.nakilex.levelplugin.auctionhouse.AuctionListing;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simple SQLite based storage for auction listings.
 */
public class AuctionStorageProvider {
    private final File dbFile;
    private Connection connection;

    public AuctionStorageProvider(File dataFolder) {
        this.dbFile = new File(dataFolder, "auctionhouse.db");
    }

    /**
     * Opens the connection and creates tables.
     */
    public void init() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS listings (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "seller TEXT," +
                        "price REAL," +
                        "expire INTEGER," +
                        "item BLOB," +
                        "active INTEGER"
                        + ")");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[Auction] Failed to initialise database: " + e.getMessage());
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public List<AuctionListing> loadActiveListings() {
        List<AuctionListing> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM listings WHERE active=1")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    UUID seller = UUID.fromString(rs.getString("seller"));
                    double price = rs.getDouble("price");
                    long expire = rs.getLong("expire");
                    ItemStack item = deserializeItem(rs.getBytes("item"));
                    list.add(new AuctionListing(id, seller, item, price, expire, true));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int insertListing(UUID seller, ItemStack item, double price, long expire) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO listings(seller,price,expire,item,active) VALUES(?,?,?,?,1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.toString());
            ps.setDouble(2, price);
            ps.setLong(3, expire);
            ps.setBytes(4, serializeItem(item));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void markInactive(int id) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE listings SET active=0 WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private byte[] serializeItem(ItemStack item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out)) {
            dataOut.writeObject(item);
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private ItemStack deserializeItem(byte[] data) {
        if (data == null) return null;
        try (ByteArrayInputStream in = new ByteArrayInputStream(data);
             BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in)) {
            return (ItemStack) dataIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
