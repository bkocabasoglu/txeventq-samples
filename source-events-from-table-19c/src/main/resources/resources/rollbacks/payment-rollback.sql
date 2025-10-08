-- ========================================================================
-- ROLLBACK SCRIPT FOR PAYMENT SQL RESOURCE
-- This script removes all database objects created by payment-sql-resource.sql
-- Execute steps in order to properly handle dependencies
-- ========================================================================

--STEP: 1 ------------------------------------------------------
-- Remove subscriber from queue
--------------------------------------------------------
BEGIN
    DBMS_AQADM.REMOVE_SUBSCRIBER(
            queue_name => 'PaymentUpdatesTopic',
            subscriber => SYS.AQ$_AGENT('PaymentUpdatesSubscriber1', NULL, NULL)
    );
    DBMS_OUTPUT.PUT_LINE('Subscriber PaymentUpdatesSubscriber1 removed successfully');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -24034 THEN
            DBMS_OUTPUT.PUT_LINE('Subscriber does not exist, skipping removal');
        ELSE
            DBMS_OUTPUT.PUT_LINE('Error removing subscriber: ' || SQLERRM);
            RAISE;
        END IF;
END;
/

--STEP: 2 ------------------------------------------------------
-- Drop trigger (must be dropped before table)
--------------------------------------------------------
BEGIN
    EXECUTE IMMEDIATE 'DROP TRIGGER PaymentUpdatesTableTrigger';
    DBMS_OUTPUT.PUT_LINE('Trigger PaymentUpdatesTableTrigger dropped successfully');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -942 THEN
            DBMS_OUTPUT.PUT_LINE('Trigger does not exist, skipping drop');
        ELSE
            DBMS_OUTPUT.PUT_LINE('Error dropping trigger: ' || SQLERRM);
            RAISE;
        END IF;
END;
/

--STEP: 3 ------------------------------------------------------
-- Drop procedure
--------------------------------------------------------
BEGIN
    EXECUTE IMMEDIATE 'DROP PROCEDURE PaymentUpdatesTriggerHandler';
    DBMS_OUTPUT.PUT_LINE('Procedure PaymentUpdatesTriggerHandler dropped successfully');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -942 THEN
            DBMS_OUTPUT.PUT_LINE('Procedure does not exist, skipping drop');
        ELSE
            DBMS_OUTPUT.PUT_LINE('Error dropping procedure: ' || SQLERRM);
            RAISE;
        END IF;
END;
/

--STEP: 4 ------------------------------------------------------
-- Stop and drop TxEventQ queue
--------------------------------------------------------
BEGIN
    -- Stop the queue first
    DBMS_AQADM.STOP_QUEUE(queue_name => 'PaymentUpdatesTopic');
    DBMS_OUTPUT.PUT_LINE('Queue PaymentUpdatesTopic stopped successfully');

    -- Drop the sharded queue
    DBMS_AQADM.DROP_SHARDED_QUEUE(queue_name => 'PaymentUpdatesTopic');
    DBMS_OUTPUT.PUT_LINE('Sharded queue PaymentUpdatesTopic dropped successfully');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -24010 THEN
            DBMS_OUTPUT.PUT_LINE('Queue does not exist, skipping drop');
        ELSIF SQLCODE = -24001 THEN
            DBMS_OUTPUT.PUT_LINE('Queue already stopped, attempting to drop...');
            BEGIN
                DBMS_AQADM.DROP_SHARDED_QUEUE(queue_name => 'PaymentUpdatesTopic');
                DBMS_OUTPUT.PUT_LINE('Sharded queue PaymentUpdatesTopic dropped successfully');
            EXCEPTION
                WHEN OTHERS THEN
                    DBMS_OUTPUT.PUT_LINE('Error dropping queue: ' || SQLERRM);
                    RAISE;
            END;
        ELSE
            DBMS_OUTPUT.PUT_LINE('Error with queue operations: ' || SQLERRM);
            RAISE;
        END IF;
END;
/

--STEP: 5 ------------------------------------------------------
-- Drop table (must be last due to dependencies)
--------------------------------------------------------
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE PaymentUpdatesTable';
    DBMS_OUTPUT.PUT_LINE('Table PaymentUpdatesTable dropped successfully');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -942 THEN
            DBMS_OUTPUT.PUT_LINE('Table does not exist, skipping drop');
        ELSE
            DBMS_OUTPUT.PUT_LINE('Error dropping table: ' || SQLERRM);
            RAISE;
        END IF;
END;
/

-- ========================================================================
-- ROLLBACK COMPLETE
-- All database objects from payment-sql-resource.sql have been removed
-- ========================================================================
COMMIT;

DBMS_OUTPUT.PUT_LINE('========================================');
DBMS_OUTPUT.PUT_LINE('ROLLBACK SCRIPT EXECUTION COMPLETED');
DBMS_OUTPUT.PUT_LINE('========================================');
