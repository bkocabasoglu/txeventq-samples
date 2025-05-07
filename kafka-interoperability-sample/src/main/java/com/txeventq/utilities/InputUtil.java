package com.txeventq.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.txeventq.KafkaOrderCreatorService;

public class InputUtil {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static final Logger logger = LoggerFactory.getLogger(KafkaOrderCreatorService.class);

    public static UserCommand readUserCommand() throws IOException {
        logger.warn("Press ENTER to send 1 message, type a number to send that many messages, or type 'bulk-<number>' for bulk send (e.g., bulk-100), type 'exit' to exit:");
        String input = reader.readLine();
        if ("exit".equalsIgnoreCase(input.trim())) {
            logger.info("Exiting application...");
            System.exit(0);
        }
        int count = 1;
        boolean bulkMode = false;
        if (!input.trim().isEmpty()) {
            if (input.trim().toLowerCase().startsWith("bulk-")) {
                bulkMode = true;
                try {
                    count = Integer.parseInt(input.trim().substring("bulk-".length()));
                } catch (NumberFormatException e) {
                    logger.warn( "Invalid bulk number. Defaulting to 1 message.", e);
                }
            } else {
                try {
                    count = Integer.parseInt(input.trim());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid number input. Defaulting to 1 message.", e);
                }
            }
        }
        return new UserCommand(false, count, bulkMode);
    }

    public static class UserCommand {
        public boolean exit;
        public int count;
        public boolean bulkMode;

        public UserCommand(boolean exit, int count, boolean bulkMode) {
            this.exit = exit;
            this.count = count;
            this.bulkMode = bulkMode;
        }
    }
}