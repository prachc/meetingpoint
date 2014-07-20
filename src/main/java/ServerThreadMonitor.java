
public class ServerThreadMonitor {
	private ServerThreadMonitor() {
	}
	public static boolean isFree = true;
	private static ServerThreadMonitor instance = new ServerThreadMonitor();

	public static ServerThreadMonitor getInstance() {
		return instance;
	}
}

