package service;

import dao.BookingDAO;
import dao.BusDAO;
import dao.SeatDAO;
import model.Booking;
import model.Bus;
import model.Seat;
import util.ConsoleUI;
import util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Booking Service
 * Handles business logic for booking operations
 */
public class BookingService {
    
    private BookingDAO bookingDAO;
    private SeatDAO seatDAO;
    private BusDAO busDAO;
    private Scanner scanner;
    
    public BookingService(Scanner scanner) {
        this.bookingDAO = new BookingDAO();
        this.seatDAO = new SeatDAO();
        this.busDAO = new BusDAO();
        this.scanner = scanner;
    }
    
    /**
     * Book a seat with double-booking prevention
     * Uses transaction and row-level locking
     */
    public boolean bookSeat(int busId, String seatNumber, String passengerName, 
                           String passengerPhone, String passengerEmail, Date travelDate) {
        
        Connection conn = null;
        
        try {
            // Get a new connection for transaction
            conn = DatabaseConnection.getInstance().getNewConnection();
            conn.setAutoCommit(false);
            
            // Get seat by bus and seat number
            Seat seat = seatDAO.getSeatByBusAndNumber(busId, seatNumber);
            
            if (seat == null) {
                ConsoleUI.printError("Seat " + seatNumber + " not found on this bus!");
                return false;
            }
            
            // Check if seat is available with row-level lock
            if (!seatDAO.isSeatAvailable(conn, seat.getSeatId(), travelDate)) {
                conn.rollback();
                ConsoleUI.printError("Sorry! Seat " + seatNumber + " is already booked for " + travelDate);
                ConsoleUI.printWarning("Please select a different seat.");
                return false;
            }
            
            // Create booking
            Booking booking = new Booking();
            booking.setSeatId(seat.getSeatId());
            booking.setPassengerName(passengerName);
            booking.setPassengerPhone(passengerPhone);
            booking.setPassengerEmail(passengerEmail);
            booking.setTravelDate(travelDate);
            booking.setStatus(Booking.STATUS_CONFIRMED);
            
            int bookingId = bookingDAO.createBooking(conn, booking);
            
            if (bookingId > 0) {
                // Commit transaction
                conn.commit();
                
                ConsoleUI.printSuccess("Booking successful!");
                System.out.println();
                
                // Fetch and display booking details
                Booking confirmedBooking = bookingDAO.getBookingById(bookingId);
                if (confirmedBooking != null) {
                    displayBookingTicket(confirmedBooking);
                }
                
                return true;
            } else {
                conn.rollback();
                ConsoleUI.printError("Failed to create booking. Please try again.");
                return false;
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            ConsoleUI.printError("Database error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Cancel a booking
     */
    public boolean cancelBooking(int bookingId) {
        // First verify booking exists and is confirmed
        Booking booking = bookingDAO.getBookingById(bookingId);
        
        if (booking == null) {
            ConsoleUI.printError("Booking #" + bookingId + " not found!");
            return false;
        }
        
        if (booking.isCancelled()) {
            ConsoleUI.printWarning("Booking #" + bookingId + " is already cancelled.");
            return false;
        }
        
        // Display booking details before cancellation
        ConsoleUI.printSubHeader("Booking Details");
        displayBookingTicket(booking);
        
        // Confirm cancellation
        ConsoleUI.printPrompt("Are you sure you want to cancel this booking? (yes/no)");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            ConsoleUI.printInfo("Cancellation aborted.");
            return false;
        }
        
        // Perform cancellation
        if (bookingDAO.cancelBooking(bookingId)) {
            ConsoleUI.printSuccess("Booking #" + bookingId + " has been cancelled successfully!");
            ConsoleUI.printInfo("Seat " + booking.getSeatNumber() + " is now available for booking.");
            return true;
        } else {
            ConsoleUI.printError("Failed to cancel booking. Please try again.");
            return false;
        }
    }
    
    /**
     * View booking details
     */
    public void viewBookingById(int bookingId) {
        Booking booking = bookingDAO.getBookingById(bookingId);
        
        if (booking == null) {
            ConsoleUI.printError("Booking #" + bookingId + " not found!");
            return;
        }
        
        displayBookingTicket(booking);
    }
    
    /**
     * Search bookings by phone number
     */
    public void searchBookingsByPhone(String phone) {
        List<Booking> bookings = bookingDAO.getBookingsByPhone(phone);
        
        if (bookings.isEmpty()) {
            ConsoleUI.printWarning("No bookings found for phone: " + phone);
            return;
        }
        
        ConsoleUI.printSubHeader("Found " + bookings.size() + " Booking(s)");
        
        int[] widths = {10, 15, 12, 20, 8, 10};
        ConsoleUI.printTableHeader(widths, "Booking#", "Passenger", "Phone", "Route", "Seat", "Status");
        
        for (Booking booking : bookings) {
            String route = booking.getSource() + " -> " + booking.getDestination();
            if (route.length() > 20) route = route.substring(0, 17) + "...";
            
            ConsoleUI.printTableDataRow(widths,
                String.valueOf(booking.getBookingId()),
                truncate(booking.getPassengerName(), 15),
                truncate(booking.getPassengerPhone(), 12),
                route,
                booking.getSeatNumber(),
                booking.getStatus()
            );
        }
        
        ConsoleUI.printTableFooter(widths);
    }
    
    /**
     * View all confirmed bookings
     */
    public void viewAllBookings() {
        List<Booking> bookings = bookingDAO.getAllConfirmedBookings();
        
        if (bookings.isEmpty()) {
            ConsoleUI.printWarning("No confirmed bookings found.");
            return;
        }
        
        ConsoleUI.printSubHeader("All Confirmed Bookings (" + bookings.size() + ")");
        
        int[] widths = {8, 15, 12, 10, 8, 10, 8};
        ConsoleUI.printTableHeader(widths, "ID", "Passenger", "Phone", "Date", "Bus", "Seat", "Fare");
        
        for (Booking booking : bookings) {
            ConsoleUI.printTableDataRow(widths,
                String.valueOf(booking.getBookingId()),
                truncate(booking.getPassengerName(), 15),
                truncate(booking.getPassengerPhone(), 12),
                booking.getTravelDate().toString(),
                truncate(booking.getBusNumber(), 10),
                booking.getSeatNumber(),
                "Rs." + String.format("%.0f", booking.getFare())
            );
        }
        
        ConsoleUI.printTableFooter(widths);
    }
    
    /**
     * Display a booking as a ticket
     */
    private void displayBookingTicket(Booking booking) {
        ConsoleUI.printTicket(
            String.valueOf(booking.getBookingId()),
            booking.getPassengerName(),
            booking.getPassengerPhone(),
            booking.getBusNumber() + " (" + booking.getBusType() + ")",
            booking.getSource() + " -> " + booking.getDestination(),
            booking.getSeatNumber(),
            booking.getTravelDate().toString(),
            booking.getDepartureTime(),
            String.format("%.2f", booking.getFare()),
            booking.getStatus()
        );
    }
    
    /**
     * Helper: Truncate string
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
