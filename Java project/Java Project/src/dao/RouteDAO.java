package dao;

import model.Route;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Route Data Access Object
 * Handles all database operations for routes
 */
public class RouteDAO {
    
    private Connection connection;
    
    public RouteDAO() {
        try {
            this.connection = DatabaseConnection.getInstance().getConnection();
        } catch (SQLException e) {
            System.err.println("Failed to get database connection: " + e.getMessage());
        }
    }
    
    /**
     * Get all routes
     */
    public List<Route> getAllRoutes() {
        List<Route> routes = new ArrayList<>();
        String sql = "SELECT route_id, source, destination, fare, duration_minutes FROM routes ORDER BY source, destination";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Route route = new Route();
                route.setRouteId(rs.getInt("route_id"));
                route.setSource(rs.getString("source"));
                route.setDestination(rs.getString("destination"));
                route.setFare(rs.getDouble("fare"));
                route.setDurationMinutes(rs.getInt("duration_minutes"));
                routes.add(route);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching routes: " + e.getMessage());
        }
        
        return routes;
    }
    
    /**
     * Get route by ID
     */
    public Route getRouteById(int routeId) {
        String sql = "SELECT route_id, source, destination, fare, duration_minutes FROM routes WHERE route_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, routeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Route route = new Route();
                    route.setRouteId(rs.getInt("route_id"));
                    route.setSource(rs.getString("source"));
                    route.setDestination(rs.getString("destination"));
                    route.setFare(rs.getDouble("fare"));
                    route.setDurationMinutes(rs.getInt("duration_minutes"));
                    return route;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching route by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search routes by source and/or destination
     */
    public List<Route> searchRoutes(String source, String destination) {
        List<Route> routes = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT route_id, source, destination, fare, duration_minutes FROM routes WHERE 1=1"
        );
        
        if (source != null && !source.trim().isEmpty()) {
            sql.append(" AND LOWER(source) LIKE LOWER(?)");
        }
        if (destination != null && !destination.trim().isEmpty()) {
            sql.append(" AND LOWER(destination) LIKE LOWER(?)");
        }
        sql.append(" ORDER BY source, destination");
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            
            if (source != null && !source.trim().isEmpty()) {
                stmt.setString(paramIndex++, "%" + source.trim() + "%");
            }
            if (destination != null && !destination.trim().isEmpty()) {
                stmt.setString(paramIndex++, "%" + destination.trim() + "%");
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Route route = new Route();
                    route.setRouteId(rs.getInt("route_id"));
                    route.setSource(rs.getString("source"));
                    route.setDestination(rs.getString("destination"));
                    route.setFare(rs.getDouble("fare"));
                    route.setDurationMinutes(rs.getInt("duration_minutes"));
                    routes.add(route);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching routes: " + e.getMessage());
        }
        
        return routes;
    }
    
    /**
     * Add a new route
     */
    public boolean addRoute(Route route) {
        String sql = "INSERT INTO routes (source, destination, fare, duration_minutes) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, route.getSource());
            stmt.setString(2, route.getDestination());
            stmt.setDouble(3, route.getFare());
            stmt.setInt(4, route.getDurationMinutes());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        route.setRouteId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding route: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get unique sources
     */
    public List<String> getUniqueSources() {
        List<String> sources = new ArrayList<>();
        String sql = "SELECT DISTINCT source FROM routes ORDER BY source";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                sources.add(rs.getString("source"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching sources: " + e.getMessage());
        }
        
        return sources;
    }
    
    /**
     * Get unique destinations
     */
    public List<String> getUniqueDestinations() {
        List<String> destinations = new ArrayList<>();
        String sql = "SELECT DISTINCT destination FROM routes ORDER BY destination";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                destinations.add(rs.getString("destination"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching destinations: " + e.getMessage());
        }
        
        return destinations;
    }
}
