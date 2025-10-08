-- ========================================================================
-- ROLLBACK SCRIPT FOR STATEMENT SQL RESOURCE
-- This script removes all database objects created by statement-sql-resource.sql
-- Execute steps in order to properly handle dependencies
-- ========================================================================

--STEP: 1 ------------------------------------------------------
-- Remove subscriber from queue
--------------------------------------------------------
BEGIN
    DBMS_AQADM.REMOVE_SUBSCRIBER(
            queue_name => 'TransactionStatementTopic',
            subscriber => SYS.AQ$_AGENT('StatementsUpdatesSubscriber1', NULL, NULL)
    );
    DBMS_OUTPUT.PUT_LINE('Subscriber StatementsUpdatesSubscriber1 removed successfully');
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
    EXECUTE IMMEDIATE 'DROP TRIGGER StatementsUpdatesTableTrigger';
    DBMS_OUTPUT.PUT_LINE('Trigger StatementsUpdatesTableTrigger dropped successfully');
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
    EXECUTE IMMEDIATE 'DROP PROCEDURE StatementsUpdatesTriggerHandler';
    DBMS_OUTPUT.PUT_LINE('Procedure StatementsUpdatesTriggerHandler dropped successfully');
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
-- Stop and drop TxEventQ queue (shared queue used by both transactions and statements)
--------------------------------------------------------
BEGIN
    -- Stop the queue first
    DBMS_AQADM.STOP_QUEUE(queue_name => 'TransactionStatementTopic');
    DBMS_OUTPUT.PUT_LINE('Queue TransactionStatementTopic stopped successfully');

    -- Drop the sharded queue
    DBMS_AQADM.DROP_SHARDED_QUEUE(queue_name => 'TransactionStatementTopic');
    DBMS_OUTPUT.PUT_LINE('Sharded queue TransactionStatementTopic dropped successfully');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -24010 THEN
            DBMS_OUTPUT.PUT_LINE('Queue does not exist, skipping drop');
        ELSIF SQLCODE = -24001 THEN
            DBMS_OUTPUT.PUT_LINE('Queue already stopped, attempting to drop...');
            BEGIN
                DBMS_AQADM.DROP_SHARDED_QUEUE(queue_name => 'TransactionStatementTopic');
                DBMS_OUTPUT.PUT_LINE('Sharded queue TransactionStatementTopic dropped successfully');
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
    EXECUTE IMMEDIATE 'DROP TABLE StatementsUpdatesTable';
    DBMS_OUTPUT.PUT_LINE('Table StatementsUpdatesTable dropped successfully');
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
-- All database objects from statement-sql-resource.sql have been removed
-- NOTE: This also removes the shared queue TransactionStatementTopic used by transactions
-- ========================================================================
COMMIT;

DBMS_OUTPUT.PUT_LINE('========================================');
DBMS_OUTPUT.PUT_LINE('ROLLBACK SCRIPT EXECUTION COMPLETED');
DBMS_OUTPUT.PUT_LINE('========================================');
