package com.nitorcreations.willow.deployer;

import java.util.Properties;

public interface LaunchMethod extends Runnable {
	public static final String ENV_KEY_DEPLOYER_IDENTIFIER = "DEPLOYER_IDENTIFIER";
	
	public enum TYPE {
		DEPENDENCY(DependencyLauncher.class), NATIVE(NativeLauncher.class);
		Class<? extends LaunchMethod> launcher;
		private TYPE(Class<? extends LaunchMethod> launcher) {
			this.launcher = launcher;
		}
		public LaunchMethod getLauncher() {
			try {
				return launcher.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
		}
	}
	public void setProperties(Properties properties);
	public long getProcessId();
	void stop();
}
