package com.nitorcreations.willow.deployer;

import java.util.concurrent.Callable;

import com.nitorcreations.willow.utils.MergeableProperties;

public interface LaunchMethod extends Callable<Integer> {
	
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
	public void setProperties(MergeableProperties properties);
	public void setProperties(MergeableProperties properties, String keyPrefix);
	public long getProcessId();
	void stopRelaunching();
	int destroyChild() throws InterruptedException;
	int restarts();
	String getName();
}
