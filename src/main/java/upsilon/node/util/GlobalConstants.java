package upsilon.node.util;

import java.time.Duration;

public abstract class GlobalConstants {
	public static final Duration MIN_SERVICE_EXECUTION_DELAY = Duration.ofSeconds(10);
	public static final Duration DEF_TIMER_EXECUTOR_DELAY = Duration.ofSeconds(2);
	public static final Duration DEF_TIMER_QUEUE_MAINTAINER_DELAY = Duration.ofSeconds(60);

	public static final Duration MAX_UPDATE_FREQUENCY = Duration.ofSeconds(60);
	public static final Duration MIN_UPDATE_FREQUENCY = Duration.ofSeconds(10);

	public static final Duration MAX_SERVICE_SLEEP = Duration.ofMinutes(15);
	public static final Duration MIN_SERVICE_SLEEP = Duration.ofSeconds(10);

	public static final Duration MIN_EXECUTOR_SLEEP = Duration.ofSeconds(2);
	public static final Duration MAX_EXECUTOR_SLEEP = Duration.ofSeconds(30);
	public static final Duration INC_EXECUTOR_SLEEP = Duration.ofSeconds(5);

	public static final Duration DEF_INC_SERVICE_UPDATE = GlobalConstants.MIN_SERVICE_SLEEP;
	public static final Duration DEF_TIMEOUT = Duration.ofSeconds(3);

	public static final int DEF_REST_PORT = 4000; 
	public static final boolean DEF_CRYPTO_ENABLED = false;
	public static final boolean DEF_DAEMON_REST_ENABLED = true;
	public static final boolean DEF_DAEMON_AMQP_ENABLED = true;
	public static final Duration CONFIG_WATCHER_DELAY = Duration.ofSeconds(2);
	public static final String DEF_AMQP_HOST = "upsilon";
}
