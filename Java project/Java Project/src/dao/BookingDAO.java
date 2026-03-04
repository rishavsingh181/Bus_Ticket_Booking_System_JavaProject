package dao;

import model.Booking;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Booking Data Access Object
 * Handles all database operations for bookings
 */
public class BookingDAO {
    
    private Connection connection;
    
    public BookingDAO() {
        try {
            this.connection = DatabaseConnection.getInstance().getConnection();
        } catch (SQLException e) {
            System.err.println("Failed to get database connection: " + e.getMessage());
        }
    }
    
    /**
     * Create a new booking with transaction support
     * Returns generated booking ID or -1 on failure
     */
    public int createBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (seat_id, passenger_name, passenger_phone, passenger_email, travel_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, booking.getSeatId());
            stmt.setString(2, booking.getPassengerName());
            stmt.setString(3, booking.getPassengerPhone());
            stmt.setString(4, booking.getPassengerEmail());
            stmt.setDate(5, booking.getTravelDate());
            stmt.setString(6, booking.getStatus());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Cancel a booking
     */
    public boolean cancelBooking(int bookingId) {
        String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ? AND status = 'CONFIRMED'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bookingId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error cancelling booking: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get booking by ID with full details
     */
    public Booking getBookingById(int bookingId) {
        String sql = "SELECT b.booking_id, b.seat_id, b.passenger_name, b.passenger_phone, " +
                     "b.passenger_email, b.travel_date, b.booking_time, b.status, " +
                     "s.seat_number, s.seat_type, " +
                     "bus.bus_number, bus.bus_type, bus.departure_time, " +
                     "r.source, r.destination, r.fare " +
                     "FROM bookings b " +
                     "JOIN seats s ON b.seat_id = s.seat_id " +
                     "JOIN buses bus ON s.bus_id = bus.bus_id " +
                     "JOIN routes r ON bus.route_id = r.route_id " +
                     "WHERE b.booking_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bookingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBooking(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching booking: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search bookings by phone number
     */
    public List<Booking> getBookingsByPhone(String phone) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.seat_id, b.passenger_name, b.passenger_phone, " +
                     "b.passenger_email, b.travel_date, b.booking_time, b.status, " +
                     "s.seat_number, s.seat_type, " +
                     "bus.bus_number, bus.bus_type, bus.departure_time, " +
                     "r.source, r.destination, r.fare " +
                     "FROM bookings b " +
                     "JOIN seats s ON b.seat_id = s.seat_id " +
                     "JOIN buses bus ON s.bus_id = bus.bus_id " +
                     "JOIN routes r ON bus.route_id = r.route_id " +
                     "WHERE b.passenger_phone LIKE ? " +
                     "ORDER BY b.travel_date DESC, b.booking_time DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + phone + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking booking = mapResultSetToBooking(rs);
                    bookings.add(booking);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching bookings: " + e.getMessage());
        }
        
        return bookings;
    }
    
    /**
     * Get all confirmed bookings
     */
    public List<Booking> getAllConfirmedBookings() {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.seat_id, b.passenger_name, b.passenger_phone, " +
                     "b.passenger_email, b.travel_date, b.booking_time, b.status, " +
                     "s.seat_number, s.seat_type, " +
                     "bus.bus_number, bus.bus_type, bus.departure_time, " +
                     "r.source, r.destination, r.fare " +
                     "FROM bookings b " +
                     "JOIN seats s ON b.seat_id = s.seat_id " +
                     "JOIN buses bus ON s.bus_id = bus.bus_id " +
                     "JOIN routes r ON bus.route_id = r.route_id " +
                     "WHERE b.status = 'CONFIRMED' " +
                     "ORDER BY b.travel_date, bus.departure_time";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Booking booking = mapResultSetToBooking(rs);
                bookings.add(booking);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching confirmed bookings: " + e.getMessage());
        }
        
        return bookings;
    }
    
    /**
     * Check if booking exists and is confirmed
     */
    public boolean isBookingConfirmed(int bookingId) {
        String sql = "SELECT status FROM bookings WHERE booking_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bookingId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "CONFIRMED".equals(rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking booking status: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to Booking object
     */
    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        Booking booking = new Booking();
        booking.setBookingId(rs.getInt("booking_id"));
        booking.setSeatId(rs.getInt("seat_id"));
        booking.setPassengerName(rs.getString("passenger_name"));
        booking.setPassengerPhone(rs.getString("passenger_phone"));
        booking.setPassengerEmail(rs.getString("passenger_email"));
        booking.setTravelDate(rs.getDate("travel_date"));
        booking.setBookingTime(rs.getTimestamp("booking_time"));
        booking.setStatus(rs.getString("status"));
        booking.setSeatNumber(rs.getString("seat_number"));
        booking.setBusNumber(rs.getString("bus_number"));
        booking.setBusType(rs.getString("bus_type"));
        booking.setDepartureTime(rs.getTime("departure_time").toString().substring(0, 5));
        booking.setSource(rs.getString("source"));
        booking.setDestination(rs.getString("destination"));
        booking.setFare(rs.getDouble("fare"));
        return booking;
    }
}
