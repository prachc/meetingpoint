public class ThreadMonitor {
	private ThreadMonitor() {
	}

	private static ThreadMonitor instance = new ThreadMonitor();

	public static ThreadMonitor getInstance() {
		return instance;
	}
}
