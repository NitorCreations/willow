package com.nitorcreations.willow.deployer;

import java.util.Properties;
import java.util.concurrent.Callable;

public interface LaunchMethod extends Callable<Integer> {
	public static final String ENV_KEY_DEPLOYER_IDENTIFIER = "DEPLOYER_IDENTIFIER";
	
	public enum TYPE {
		DEPENDENCY(DependencyLauncher.class), NATIVE(NativeLauncher.class), JAVA(JavaLauncher.class);
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
	public void setProperties(Properties properties, String keyPrefix);
	public long getProcessId();
	void stopRelaunching();
	int destroyChild() throws InterruptedException;
	int restarts();
}
