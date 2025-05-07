-- If you are using the free container
-- ALTER SESSION SET CONTAINER = FREEPDB1;

--- User creation and permissions for TxEventQ
CREATE USER TXEVENTQ_ADMIN IDENTIFIED BY [Your User Password];

-- Basic login permission
GRANT CREATE SESSION TO TXEVENTQ_ADMIN;

-- Required roles for TxEventQ (Advanced Queuing)
GRANT AQ_ADMINISTRATOR_ROLE TO TXEVENTQ_ADMIN;
GRANT CONNECT, RESOURCE TO TXEVENTQ_ADMIN;

-- Enable usage of Oracle Streams AQ (used by TxEventQ)
GRANT EXECUTE ON DBMS_AQ TO TXEVENTQ_ADMIN;
GRANT EXECUTE ON DBMS_AQADM TO TXEVENTQ_ADMIN;
GRANT ENQUEUE ANY QUEUE TO TXEVENTQ_ADMIN;
GRANT DEQUEUE ANY QUEUE TO TXEVENTQ_ADMIN;

-- Access to performance views (optional, for diagnostics)
GRANT SELECT_CATALOG_ROLE TO TXEVENTQ_ADMIN;

-- Give quota on tablespace (adjust if needed)
ALTER USER TXEVENTQ_ADMIN QUOTA UNLIMITED ON USERS;
-- or if you're using DATA tablespace
-- ALTER USER TXEVENTQ_ADMIN QUOTA UNLIMITED ON DATA;

---------------------

-- Topic to store orders. This topic will be used for sink connector to consume messages from the Kafka topic
BEGIN
    DBMS_AQADM.CREATE_DATABASE_KAFKA_TOPIC(
        topicname                => 'OrdersKafka',
        partition_num            => 3,           -- 3 partitions
        retentiontime            => 7*24*3600,   -- Retain messages for 7 days (default)
        partition_assignment_mode => 2,
        replication_mode         => SYS.DBMS_AQADM.NONE -- No replication
    );
END;

-- BEGIN
--     DBMS_AQADM.drop_database_kafka_topic(topicname => 'OrdersKafka');
-- END;

-- Topic for orders to be reconciled. If any of the product in an order is not available, the order will be sent to this topic for reconciliation.
BEGIN
    DBMS_AQADM.CREATE_DATABASE_KAFKA_TOPIC(
        topicname                => 'OrdersToReconcile',
        partition_num            => 3,           -- 3 partitions
        retentiontime            => 7*24*3600,   -- Retain messages for 7 days (default)
        partition_assignment_mode => 2,
        replication_mode         => SYS.DBMS_AQADM.NONE
    );
END;

-- -- Needed for the source connector to consume messages from the topic
-- BEGIN
--   DBMS_AQADM.ADD_SUBSCRIBER(
--         queue_name => 'OrdersToReconcile',
--         subscriber => SYS.AQ$_AGENT('ORDERSTORECONCILE_SUBSCRIBER_LOCAL', NULL, NULL)
--   );
-- END;

-- Topic for the OrdersToShip queue. After Orders are processed and if they are ready to ship, they will be sent to this topic.
BEGIN
    DBMS_AQADM.CREATE_DATABASE_KAFKA_TOPIC(
        topicname                => 'OrdersToShip',
        partition_num            => 3,           -- 3 partitions
        retentiontime            => 7*24*3600,   -- Retain messages for 7 days (default)
        partition_assignment_mode => 2,
        replication_mode         => SYS.DBMS_AQADM.NONE
    );
END;


---- Table to store product Inventory
CREATE TABLE ProductInventory (
                                  productId INT PRIMARY KEY,
                                  productName VARCHAR2(255) NOT NULL,
                                  description VARCHAR2(4000),
                                  itemsInStock INT NOT NULL,
                                  unitPrice DECIMAL(10, 2)
);

-- Insert sample data (100 records)
-- only needed run once
BEGIN
FOR i IN 1..100 LOOP
        INSERT INTO ProductInventory (productId, productName, description, itemsInStock, unitPrice)
        VALUES (
            i,                                      -- productId
            CASE MOD(i, 5)
                WHEN 0 THEN 'Laptop ' || i
                WHEN 1 THEN 'Smartphone ' || i
                WHEN 2 THEN 'Tablet ' || i
                WHEN 3 THEN 'Headphones ' || i
                ELSE 'Smartwatch ' || i
            END,                                    -- productName
            CASE MOD(i, 5)
                WHEN 0 THEN 'High-performance laptop for professionals'
                WHEN 1 THEN 'Latest smartphone with advanced camera'
                WHEN 2 THEN 'Lightweight tablet for on-the-go use'
                WHEN 3 THEN 'Noise-cancelling headphones for immersive audio'
                ELSE 'Feature-rich smartwatch for fitness tracking'
            END,                                    -- description
            ROUND(DBMS_RANDOM.VALUE(10, 1000)),    -- itemsInStock (random number between 10 and 1000)
            ROUND(DBMS_RANDOM.VALUE(50, 2000), 2)     -- unitPrice (random number between 50 and 2000)
        );
END LOOP;
COMMIT;
END;
