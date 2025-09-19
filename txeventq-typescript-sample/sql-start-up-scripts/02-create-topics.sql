-- =====================================================
-- Oracle TxEventQ POC - Topic Creation Script
-- =====================================================

-- Connect to the PDB
ALTER SESSION SET CONTAINER = FREEPDB1;

-- Create the TxEventQ topic using CREATE TRANSACTIONAL EVENT QUEUE
-- Using schema prefix to create in TXEVENTQ_USER's schema
BEGIN
  DBMS_AQADM.CREATE_TRANSACTIONAL_EVENT_QUEUE(
    queue_name => 'TXEVENTQ_USER.EVENT_TOPIC',
    multiple_consumers => TRUE,
    comment => 'Transactional Event Queue for Event Topic',
    queue_payload_type => 'JSON'
  );
  
  -- Creates 4 shards/event-streams/partitions
  DBMS_AQADM.set_queue_parameter('TXEVENTQ_USER.EVENT_TOPIC', 'SHARD_NUM', 4);
  -- Making the queue key-based to maintain order of events per key
  DBMS_AQADM.set_queue_parameter('TXEVENTQ_USER.EVENT_TOPIC', 'KEY_BASED_ENQUEUE', 1);
  -- Making consumers be sticky so the order is maintained during dequeue
  DBMS_AQADM.set_queue_parameter('TXEVENTQ_USER.EVENT_TOPIC', 'STICKY_DEQUEUE', 1);
  -- Start the queue
  DBMS_AQADM.start_queue(queue_name => 'TXEVENTQ_USER.EVENT_TOPIC');
END;
/

-- Create subscriber
BEGIN
  DBMS_AQADM.ADD_SUBSCRIBER(
    queue_name => 'TXEVENTQ_USER.EVENT_TOPIC',
    subscriber => SYS.AQ$_AGENT('event_subscriber', NULL, NULL)
  );
END;
/

-- Display success message
SELECT 'TxEventQ topic EVENT_TOPIC created successfully!' as status FROM dual;