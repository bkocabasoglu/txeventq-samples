import * as oracledb from 'oracledb';
import 'dotenv/config';

interface OracleConfig {
  user: string;
  password: string;
  connectString: string;
  walletPath: string;
  walletPassword?: string;
}

interface QueueConfig {
  name: string;
  consumerName: string;
}

interface SubscriberConfig {
  batchSize: number;
  waitSeconds: number;
  retryAttempts: number;
  retryDelayMs: number;
}

interface ConnectionResilienceConfig {
  maxReconnectAttempts: number;
  initialReconnectDelayMs: number;
  maxReconnectDelayMs: number;
}

export interface AppConfig {
  oracle: OracleConfig;
  subscriber: SubscriberConfig;
  connectionResilience: ConnectionResilienceConfig;
}

export const QUEUE_CONFIG: QueueConfig = {
  name: 'EVENT_TOPIC',
  consumerName: 'event_subscriber'
};

let cachedConfig: AppConfig | null = null;

function getRequiredEnv(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function getOptionalEnv(name: string): string | undefined {
  const value = process.env[name]?.trim();
  return value || undefined;
}

function getNumberEnv(name: string, defaultValue: number, minimum: number): number {
  const raw = getOptionalEnv(name);
  if (!raw) {
    return defaultValue;
  }

  const parsed = Number.parseInt(raw, 10);
  if (Number.isNaN(parsed) || parsed < minimum) {
    throw new Error(`Invalid ${name} value "${raw}". Expected an integer >= ${minimum}.`);
  }

  return parsed;
}

export function getConfig(): AppConfig {
  if (cachedConfig) {
    return cachedConfig;
  }

  cachedConfig = {
    oracle: {
      user: getRequiredEnv('ORACLE_USER'),
      password: getRequiredEnv('ORACLE_PASSWORD'),
      connectString: getRequiredEnv('ORACLE_CONNECT_STRING'),
      walletPath: getRequiredEnv('ORACLE_WALLET_PATH'),
      walletPassword: getOptionalEnv('ORACLE_WALLET_PASSWORD')
    },
    subscriber: {
      batchSize: getNumberEnv('SUBSCRIBER_BATCH_SIZE', 25, 1),
      waitSeconds: getNumberEnv('SUBSCRIBER_WAIT_SECONDS', 5, 0),
      retryAttempts: getNumberEnv('SUBSCRIBER_RETRY_ATTEMPTS', 3, 1),
      retryDelayMs: getNumberEnv('SUBSCRIBER_RETRY_DELAY_MS', 1000, 1)
    },
    connectionResilience: {
      // 0 means unlimited reconnect attempts for long-running services.
      maxReconnectAttempts: getNumberEnv('DB_RECONNECT_MAX_ATTEMPTS', 0, 0),
      initialReconnectDelayMs: getNumberEnv('DB_RECONNECT_INITIAL_DELAY_MS', 1000, 1),
      maxReconnectDelayMs: getNumberEnv('DB_RECONNECT_MAX_DELAY_MS', 30000, 1)
    }
  };

  return cachedConfig;
}

export function assertThinMode(): void {
  if (!oracledb.thin) {
    throw new Error('Oracle Thin mode is required for ADB wallet connections. Remove initOracleClient() calls.');
  }
}

export function getOracleConnectionAttributes(
  config: AppConfig = getConfig()
): oracledb.ConnectionAttributes {
  const attrs: oracledb.ConnectionAttributes = {
    user: config.oracle.user,
    password: config.oracle.password,
    connectString: config.oracle.connectString,
    configDir: config.oracle.walletPath,
    walletLocation: config.oracle.walletPath
  };

  if (config.oracle.walletPassword) {
    attrs.walletPassword = config.oracle.walletPassword;
  }

  return attrs;
}
