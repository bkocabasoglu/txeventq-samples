package com.oracle.osd;

import oracle.jdbc.datasource.impl.OracleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ProduceClaimRecords23c {
    private static final String urlNode1 = "[your_node1_url]";
    private static final String urlNode2 = "[your_node2_url]";
    private static final String username = "[your_user]";
    private static final String password = "[your_password]";
    private static final int numberOfClaims = 20;
    private static final int entriesPerClaim = 10;

    public static void main(String[] args) {
        OracleDataSource dsNode1 = createDataSource(urlNode1);
        OracleDataSource dsNode2 = createDataSource(urlNode2);

        List<ClaimEntry> claimEntries = prepareData(dsNode1, numberOfClaims, entriesPerClaim);

        System.out.println("Prepared Data:");
        for (ClaimEntry entry : claimEntries) {
            System.out.println("Claim ID: " + entry.claimNumber() + ", Entry Number: " + entry.entryNumber());
        }
        insertClaimEntries(claimEntries, dsNode1, dsNode2);
    }

    private static OracleDataSource createDataSource(String url) {
        OracleDataSource ds = null;
        try {
            ds = new OracleDataSource();
            ds.setURL(url);
            ds.setUser(username);
            ds.setPassword(password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    private static List<ClaimEntry> prepareData(OracleDataSource ds, int numberOfClaims, int entriesPerClaim) {
        List<ClaimEntry> claimEntries = new ArrayList<>();
        List<Integer> claimNumbers = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = ds.getConnection();
            for (int i = 0; i < numberOfClaims; i++) {
                String seqQuery = "SELECT generali_claim_seq.nextval FROM dual";
                pstmt = con.prepareStatement(seqQuery);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    int claimNumber = rs.getInt(1);
                    claimNumbers.add(claimNumber);
                }
            }
            Collections.shuffle(claimNumbers); // Shuffle the claim IDs

            for (int claimNumber : claimNumbers) {
                for (int j = 1; j <= entriesPerClaim; j++) {
                    claimEntries.add(new ClaimEntry(claimNumber, j));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return claimEntries;
    }

    private static void insertClaimEntries(List<ClaimEntry> claimEntries, OracleDataSource dsNode1, OracleDataSource dsNode2) {
        Random random = new Random();
        for (ClaimEntry entry : claimEntries) {
            String message = "Claim entry: " + entry.entryNumber() + ", Timestamp: " + System.currentTimeMillis() + ", for claim: " + entry.claimNumber();
            System.out.println("Value to be inserted: " + message);

            OracleDataSource ds = random.nextBoolean() ? dsNode1 : dsNode2;
            insertRecord(ds, message, entry.claimNumber());
        }
    }

    private static void insertRecord(OracleDataSource ds, String message, int claimNumber) {
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = ds.getConnection();  // Auto-commit is enabled by default
            con.setAutoCommit(false);

            message += (ds.getURL().equals(urlNode1)) ? " - node1" : " - node2";

            String insertQuery = "INSERT INTO generali_poc_claim_jms2 (SomeValue, ClaimId) VALUES (?, ?)";
            pstmt = con.prepareStatement(insertQuery);
            pstmt.setString(1, message);
            pstmt.setInt(2, claimNumber);
            pstmt.executeUpdate();  // Auto-commit will commit immediately
            con.commit();

            System.out.println("Record inserted successfully to " + ds.getURL());

        } catch (SQLException e) {
            e.printStackTrace();
            if (con != null) {
                try {
                    con.rollback();  // 🔹 Rollback in case of failure (although auto-commit is enabled)
                    System.out.println("Transaction rolled back due to an error.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            // Clean up resources
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private record ClaimEntry(int claimNumber, int entryNumber) {
    }
}
