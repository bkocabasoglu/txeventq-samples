package com.oracle.osd.utilities;

import com.oracle.osd.models.ClaimEvent;
import com.oracle.osd.models.ClaimEvent.ClaimType;
import com.oracle.osd.models.ClaimEvent.ClaimStatus;
import com.oracle.osd.models.ClaimEvent.EntryStatus;
import java.util.*;

/**
 * Utility class for generating claim events.
 * Provides centralized data generation functionality for claim events.
 */
public class ClaimEventGenerator {
    
    // Claim status options
    private static final ClaimStatus[] CLAIM_STATUSES = ClaimStatus.values();
    
    // Claim type options
    private static final ClaimType[] CLAIM_TYPES = ClaimType.values();
    
    // Entry status options
    private static final EntryStatus[] ENTRY_STATUSES = EntryStatus.values();
    
    // Amount range
    private static final double MIN_AMOUNT = 100.0;
    private static final double MAX_AMOUNT = 10100.0;
    
    // Claim ID range
    private static final int MIN_CLAIM_ID = 1000;
    private static final int MAX_CLAIM_ID = 9999;
    
    private final Random random;
    
    /**
     * Creates a ClaimEventGenerator with a new Random instance.
     */
    public ClaimEventGenerator() {
        this.random = new Random();
    }
    
    /**
     * Creates a ClaimEventGenerator with a seeded Random instance.
     * 
     * @param seed the seed for the random number generator
     */
    public ClaimEventGenerator(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * Generates a list of claim events based on the provided configuration.
     * 
     * @param numberOfClaims the number of claims to generate
     * @param entriesPerClaim the number of entries per claim
     * @return list of generated claim events
     */
    public List<ClaimEvent> generateClaimEvents(int numberOfClaims, int entriesPerClaim) {
        List<ClaimEvent> claimEvents = new ArrayList<>();
        List<Integer> claimNumbers = generateClaimNumbers(numberOfClaims);
        
        for (int claimNumber : claimNumbers) {
            ClaimType claimType = getRandomClaimType();
            ClaimStatus status = getRandomClaimStatus();
            double amount = generateRandomAmount();
            
            for (int entryNumber = 1; entryNumber <= entriesPerClaim; entryNumber++) {
                String eventId = generateEventId();
                long timestamp = System.currentTimeMillis();
                EntryStatus entryStatus = getRandomEntryStatus();
                
                claimEvents.add(new ClaimEvent(
                    eventId,
                    claimNumber,
                    entryNumber,
                    claimType,
                    status,
                    entryStatus,
                    amount,
                    timestamp
                ));
            }
        }
        
        return claimEvents;
    }
    
    /**
     * Generates a single claim event with random data.
     * 
     * @return a randomly generated claim event
     */
    public ClaimEvent generateSingleClaimEvent() {
        String eventId = generateEventId();
        int claimId = generateClaimId();
        int entryNumber = 1;
        ClaimType claimType = getRandomClaimType();
        ClaimStatus status = getRandomClaimStatus();
        EntryStatus entryStatus = getRandomEntryStatus();
        double amount = generateRandomAmount();
        long timestamp = System.currentTimeMillis();
        
        return new ClaimEvent(
            eventId,
            claimId,
            entryNumber,
            claimType,
            status,
            entryStatus,
            amount,
            timestamp
        );
    }
    
    /**
     * Generates a list of unique claim numbers.
     * 
     * @param count the number of claim numbers to generate
     * @return list of unique claim numbers
     */
    private List<Integer> generateClaimNumbers(int count) {
        List<Integer> claimNumbers = new ArrayList<>();
        int baseClaimNumber = MIN_CLAIM_ID + random.nextInt(MAX_CLAIM_ID - MIN_CLAIM_ID - count + 1);
        
        for (int i = 0; i < count; i++) {
            claimNumbers.add(baseClaimNumber + i);
        }
        
        Collections.shuffle(claimNumbers);
        return claimNumbers;
    }
    
    /**
     * Generates a unique event ID.
     * 
     * @return a unique event ID string
     */
    private String generateEventId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generates a random claim ID.
     * 
     * @return a random claim ID
     */
    private int generateClaimId() {
        return MIN_CLAIM_ID + random.nextInt(MAX_CLAIM_ID - MIN_CLAIM_ID + 1);
    }
    
    /**
     * Gets a random claim type.
     * 
     * @return a random claim type
     */
    private ClaimType getRandomClaimType() {
        return CLAIM_TYPES[random.nextInt(CLAIM_TYPES.length)];
    }
    
    /**
     * Gets a random claim status.
     * 
     * @return a random claim status
     */
    private ClaimStatus getRandomClaimStatus() {
        return CLAIM_STATUSES[random.nextInt(CLAIM_STATUSES.length)];
    }
    
    /**
     * Gets a random entry status.
     * 
     * @return a random entry status
     */
    private EntryStatus getRandomEntryStatus() {
        return ENTRY_STATUSES[random.nextInt(ENTRY_STATUSES.length)];
    }
    
    /**
     * Generates a random amount within the defined range.
     * 
     * @return a random amount
     */
    private double generateRandomAmount() {
        return MIN_AMOUNT + random.nextDouble() * (MAX_AMOUNT - MIN_AMOUNT);
    }
    
    /**
     * Gets all available claim types.
     * 
     * @return array of claim types
     */
    public static ClaimType[] getClaimTypes() {
        return CLAIM_TYPES.clone();
    }
    
    /**
     * Gets all available claim statuses.
     * 
     * @return array of claim statuses
     */
    public static ClaimStatus[] getClaimStatuses() {
        return CLAIM_STATUSES.clone();
    }
    
    /**
     * Gets all available entry statuses.
     * 
     * @return array of entry statuses
     */
    public static EntryStatus[] getEntryStatuses() {
        return ENTRY_STATUSES.clone();
    }
}
