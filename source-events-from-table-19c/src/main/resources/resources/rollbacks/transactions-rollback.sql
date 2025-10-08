-- ========================================================================
-- ROLLBACK SCRIPT FOR TRANSACTIONS SQL RESOURCE
-- This script removes all database objects created by transactions-sql-resource.sql
-- Execute steps in order to properly handle dependencies
-- NOTE: The shared queue TransactionStatementTopic is managed by the statement SQL resource
-- ========================================================================

--STEP: 1 ------------------------------------------------------
-- Drop trigger (must be dropped before table)
--------------------------------------------------------
BEGIN
    EXECUTE IMMEDIATE 'DROP TRIGGER TransactionUpdatesTableTrigger';
    DBMS_OUTPUT.PUT_LINE('Trigger TransactionUpdatesTableTrigger dropped successfully');
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

--STEP: 2 ------------------------------------------------------
-- Drop procedure
--------------------------------------------------------
BEGIN
    EXECUTE IMMEDIATE 'DROP PROCEDURE TransactionUpdatesTriggerHandler';
    DBMS_OUTPUT.PUT_LINE('Procedure TransactionUpdatesTriggerHandler dropped successfully');
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

--STEP: 3 ------------------------------------------------------
-- Drop table (must be last due to dependencies)
--------------------------------------------------------
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE TransactionUpdatesTable';
    DBMS_OUTPUT.PUT_LINE('Table TransactionUpdatesTable dropped successfully');
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
-- All database objects from transactions-sql-resource.sql have been removed
-- NOTE: The shared queue TransactionStatementTopic is NOT removed here
--       Use the statement-rollback.sql script to remove the shared queue
-- ========================================================================
COMMIT;

DBMS_OUTPUT.PUT_LINE('========================================');
DBMS_OUTPUT.PUT_LINE('ROLLBACK SCRIPT EXECUTION COMPLETED');
DBMS_OUTPUT.PUT_LINE('Note: Shared queue TransactionStatementTopic preserved');
DBMS_OUTPUT.PUT_LINE('Use statement-rollback.sql to remove the shared queue');
DBMS_OUTPUT.PUT_LINE('========================================');
