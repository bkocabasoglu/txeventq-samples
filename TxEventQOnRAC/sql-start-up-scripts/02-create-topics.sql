-- =====================================================
-- Oracle TxEventQ - Create Topic and Default Subsciber(Consumer Grouper)
-- =====================================================

-- Connect to the PDB
ALTER SESSION SET CONTAINER = FREEPDB1;

--------------------------------------------------------
-- Create TxEventQ in 23ai
--------------------------------------------------------
BEGIN
  DBMS_AQADM.CREATE_TRANSACTIONAL_EVENT_QUEUE(
    queue_name => 'TXEVENTQ_USER.ClaimUpdatesTopic',
    multiple_consumers => TRUE,
    comment => 'Transactional Event Queue for Claim Updates Topic',
    queue_payload_type => 'JSON'
  );
  
  -- Creates 4 shards/event-streams/partitions
  DBMS_AQADM.set_queue_parameter('TXEVENTQ_USER.ClaimUpdatesTopic', 'SHARD_NUM', 8);
  -- Making the queue key-based to maintain order of events per key
  DBMS_AQADM.set_queue_parameter('TXEVENTQ_USER.ClaimUpdatesTopic', 'KEY_BASED_ENQUEUE', 1);
  -- Making consumers be sticky so the order is maintained during dequeue
  DBMS_AQADM.set_queue_parameter('TXEVENTQ_USER.ClaimUpdatesTopic', 'STICKY_DEQUEUE', 1);
  -- Start the queue
  DBMS_AQADM.start_queue(queue_name => 'TXEVENTQ_USER.ClaimUpdatesTopic');
END;
/

--------------------------------------------------------
-- Create default subscriber
--------------------------------------------------------
BEGIN
  DBMS_AQADM.ADD_SUBSCRIBER(
    queue_name => 'TXEVENTQ_USER.ClaimUpdatesTopic',
    subscriber => SYS.AQ$_AGENT('ClaimUpdatesSubscriber', NULL, NULL)
  );
END;
/

-- Display success message
SELECT 'TxEventQ topic EVENT_TOPIC created successfully!' as status FROM dual;
/