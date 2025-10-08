--STEP: 1 ------------------------------------------------------
-- Create table for statements events to be inserted or updated
--------------------------------------------------------
CREATE TABLE StatementsUpdatesTable
(
    Id            RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    StatementsId VARCHAR(100),
    Notes         VARCHAR(500),
    Timestamp     NUMBER(13, 0)
);


--STEP: 2 ------------------------------------------------------
-- Create TxEventQ in 19c
--------------------------------------------------------
BEGIN
    DBMS_AQADM.CREATE_SHARDED_QUEUE(
            queue_name => 'TransactionStatementTopic',
            multiple_consumers => TRUE
    );

    DBMS_AQADM.set_queue_parameter('TransactionStatementTopic', 'SHARD_NUM', 20);

    -- Start the queue
    DBMS_AQADM.start_queue(queue_name=>'TransactionStatementTopic');
END;

--STEP: 3 ------------------------------------------------------
-- Create/Update trigger handler
--------------------------------------------------------
CREATE OR REPLACE PROCEDURE StatementsUpdatesTriggerHandler(
    p_Notes IN VARCHAR2,
    p_StatementsId IN VARCHAR2,
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
        message.set_text('{ "StatementsId": "' || p_StatementsId || '", "Notes": "' || p_Notes ||
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
        message.set_text('{ "StatementsId": "' || p_StatementsId || '", "Notes": "' || p_Notes ||
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
CREATE OR REPLACE TRIGGER StatementsUpdatesTableTrigger
    BEFORE INSERT OR UPDATE
    ON StatementsUpdatesTable
    FOR EACH ROW
BEGIN
    IF INSERTING THEN
        StatementsUpdatesTriggerHandler(:NEW.Notes, :NEW.StatementsId, 'INSERT', NULL);
    ELSIF UPDATING THEN
        StatementsUpdatesTriggerHandler(:NEW.Notes, :NEW.StatementsId, 'UPDATE', :OLD.Notes);
    END IF;
END;


--STEP: 5 ------------------------------------------------------
-- Create subscriber
--------------------------------------------------------
BEGIN
    DBMS_AQADM.ADD_SUBSCRIBER(
            queue_name => 'TransactionStatementTopic',
            subscriber => SYS.AQ$_AGENT('StatementsUpdatesSubscriber1', NULL, NULL)
    );
END;

--STEP: 6 ------------------------------------------------------
-- Insert messages PL/SQL for testing
--------------------------------------------------------
-- declare
--     i               NUMBER        := 0;
--     j               NUMBER        := 0;
--     statements_number NUMBER        := 0;
--     message         VARCHAR2(100) := '';
-- begin
--
--
--     for i in 1..10000
--         loop
--             j := j + 1;
--
--             statements_number := 127;
--             select ('Create statements entry: ' || j || ' for statements: ' || statements_number) into message from dual;
-- --dbms_output.put_line('Value to be inserted: ' || message);
--
--             INSERT INTO StatementsUpdatesTable(Notes, StatementsId)
--             VALUES ('Create statements entry: ' || j || ' for statements: ' || statements_number,
--                     statements_number);
--
--         end loop;
--     commit;
-- end;
--
-- select *
-- from TransactionStatementTopic;
--
-- select shard, count(*)
-- from TransactionStatementTopic
-- group by shard;
--
-- select *
-- from StatementsUpdatesTable;