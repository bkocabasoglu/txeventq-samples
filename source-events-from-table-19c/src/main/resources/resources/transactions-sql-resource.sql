--STEP: 1 ------------------------------------------------------
-- Create table for transaction events to be inserted or updated
--------------------------------------------------------
CREATE TABLE TransactionUpdatesTable
(
    Id            RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    TransactionId VARCHAR(100),
    Notes         VARCHAR(500),
    Timestamp     NUMBER(13, 0)
);

--STEP: 2 ------------------------------------------------------
-- Create/Update trigger handler
--------------------------------------------------------
CREATE OR REPLACE PROCEDURE TransactionUpdatesTriggerHandler(
    p_Notes IN VARCHAR2,
    p_TransactionId IN VARCHAR2,
    p_OperationType IN VARCHAR2,
    p_OldNotes IN VARCHAR2
)
AS
    message            SYS.AQ$_JMS_TEXT_MESSAGE;
    enqueue_options    DBMS_AQ.ENQUEUE_OPTIONS_T;
    message_properties DBMS_AQ.MESSAGE_PROPERTIES_T;
    msg_id             RAW(16);
    epoch_millis       NUMBER(13, 0);
BEGIN
    enqueue_options := DBMS_AQ.ENQUEUE_OPTIONS_T();
    message_properties := DBMS_AQ.MESSAGE_PROPERTIES_T();

    -- Calculate timestamp once for both operations
    SELECT (SYSDATE - TO_DATE('1970-01-01', 'YYYY-MM-DD')) * 86400000 INTO epoch_millis FROM dual;

    IF p_OperationType = 'INSERT' THEN
        DBMS_OUTPUT.PUT_LINE('INSERT operation detected');

        message := SYS.AQ$_JMS_TEXT_MESSAGE.CONSTRUCT();
        message.set_text('{ "TransactionId": "' || p_TransactionId || '", "Notes": "' || p_Notes ||
                         '","Timestamp": "' || epoch_millis || '", "Action": "' || p_OperationType || '" }');

        DBMS_AQ.enqueue(
                queue_name => 'TransactionStatementTopic',
                enqueue_options => enqueue_options,
                message_properties => message_properties,
                payload => message,
                msgid => msg_id
        );


    ELSIF p_OperationType = 'UPDATE' THEN
        DBMS_OUTPUT.PUT_LINE('UPDATE operation detected');

        DBMS_OUTPUT.PUT_LINE(p_OldNotes);
        message := SYS.AQ$_JMS_TEXT_MESSAGE.CONSTRUCT();
        message.set_text('{ "TransactionId": "' || p_TransactionId || '", "Notes": "' || p_Notes ||
                         '","Timestamp": "' || epoch_millis || '", "Action": "' || p_OperationType ||
                         '", "Notes_Old": "' || p_OldNotes || '" }');


        DBMS_AQ.enqueue(
                queue_name => 'TransactionStatementTopic',
                enqueue_options => enqueue_options,
                message_properties => message_properties,
                payload => message,
                msgid => msg_id
        );

    ELSE
        RAISE_APPLICATION_ERROR(-20001, 'Invalid operation type');
    END IF;
END;

--STEP: 4 ------------------------------------------------------
-- Create/Update trigger to call enqueue handler procedure
--------------------------------------------------------
CREATE OR REPLACE TRIGGER TransactionUpdatesTableTrigger
    BEFORE INSERT OR UPDATE
    ON TransactionUpdatesTable
    FOR EACH ROW
BEGIN
    IF INSERTING THEN
        TransactionUpdatesTriggerHandler(:NEW.Notes, :NEW.TransactionId, 'INSERT', NULL);
    ELSIF UPDATING THEN
        TransactionUpdatesTriggerHandler(:NEW.Notes, :NEW.TransactionId, 'UPDATE', :OLD.Notes);
    END IF;
END;

--STEP: 5 ------------------------------------------------------
-- Insert messages PL/SQL for testing
--------------------------------------------------------
-- declare
--     i               NUMBER        := 0;
--     j               NUMBER        := 0;
--     transaction_number NUMBER        := 0;
--     message         VARCHAR2(100) := '';
-- begin
--
--
--     for i in 1..10000
--         loop
--             j := j + 1;
--
--             transaction_number := 127;
--             select ('Create transaction entry: ' || j || ' for transaction: ' || transaction_number) into message from dual;
-- --dbms_output.put_line('Value to be inserted: ' || message);
--
--             INSERT INTO TransactionUpdatesTable(Notes, TransactionId)
--             VALUES ('Create transaction entry: ' || j || ' for transaction: ' || transaction_number,
--                     transaction_number);
--
--         end loop;
--     commit;
-- end;
--
-- select *
-- from TransactionStatementTopic order by ENQUEUE_TIME desc;
--
-- select shard, count(*)
-- from TransactionStatementTopic
-- group by shard;
--
-- select *
-- from TransactionUpdatesTable;